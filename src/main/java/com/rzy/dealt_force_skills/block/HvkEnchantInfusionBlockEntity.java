package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.compat.ApotheosisCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkEnchantInfusion;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HvkEnchantInfusionBlockEntity extends BlockEntity {
    private ItemStack stored = ItemStack.EMPTY;

    public HvkEnchantInfusionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ENCHANT_INFUSION.get(), pos, state);
    }

    public void open(ServerPlayer player) {
        if (!accessible(player)) return;
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(stored);
        List<S2C_OpenHvkEnchantInfusion.Entry> entries = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (Map.Entry<net.minecraft.resources.ResourceKey<Enchantment>, Enchantment> registryEntry
                    : BuiltInRegistries.ENCHANTMENT.entrySet()) {
                Enchantment enchantment = registryEntry.getValue();
                int existing = current.getOrDefault(enchantment, 0);
                if (existing > 0 || stored.is(Items.ENCHANTED_BOOK) || enchantment.canEnchant(stored)) {
                    entries.add(new S2C_OpenHvkEnchantInfusion.Entry(registryEntry.getKey().location().toString(),
                            existing, ApotheosisCompat.enchantmentMaxLevel(enchantment,
                            enchantment.getMaxLevel()) * 5));
                }
            }
        }
        entries.sort(Comparator.comparing(S2C_OpenHvkEnchantInfusion.Entry::id));
        List<S2C_OpenHvkEnchantInfusion.AffixEntry> affixEntries = new ArrayList<>();
        for (ApotheosisCompat.AffixEntry entry : ApotheosisCompat.listAffixes(stored)) {
            if (affixEntries.size() >= S2C_OpenHvkEnchantInfusion.MAX_ENTRIES) break;
            affixEntries.add(new S2C_OpenHvkEnchantInfusion.AffixEntry(entry.id(), entry.name(),
                    entry.currentValue(), entry.maxValue()));
        }
        NetworkHandler.sendToPlayer(new S2C_OpenHvkEnchantInfusion(worldPosition, stored.copy(), entries,
                ApotheosisCompat.isAffixEditingAvailable(), affixEntries), player);
    }

    public void insertFromSlot(ServerPlayer player, int selectedSlot) {
        if (!accessible(player) || !stored.isEmpty()
                || selectedSlot < 0 || selectedSlot >= Inventory.INVENTORY_SIZE) return;
        ItemStack playerStack = player.getInventory().getItem(selectedSlot);
        if (playerStack.isEmpty()) return;
        stored = playerStack;
        player.getInventory().setItem(selectedSlot, ItemStack.EMPTY);
        setChanged();
        player.inventoryMenu.broadcastChanges();
        open(player);
    }

    public void retrieve(ServerPlayer player) {
        if (!accessible(player) || stored.isEmpty()) return;
        if (player.getInventory().add(stored)) stored = ItemStack.EMPTY;
        setChanged();
        player.inventoryMenu.broadcastChanges();
        open(player);
    }

    public void apply(ServerPlayer player, String enchantmentId, int requestedLevel) {
        if (!accessible(player) || stored.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        Enchantment enchantment = id == null ? null : BuiltInRegistries.ENCHANTMENT.getOptional(id).orElse(null);
        if (enchantment == null) return;
        Map<Enchantment, Integer> current = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(stored));
        int oldLevel = current.getOrDefault(enchantment, 0);
        int max = ApotheosisCompat.enchantmentMaxLevel(enchantment, enchantment.getMaxLevel()) * 5;
        int level = Math.max(0, Math.min(max, requestedLevel));
        if (oldLevel == 0 && !stored.is(Items.ENCHANTED_BOOK) && !enchantment.canEnchant(stored)) return;
        int delta = level - oldLevel;
        if (delta > 0 && !player.getAbilities().instabuild && player.experienceLevel < delta) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.hvk_enchant_infusion.no_experience", delta), true);
            return;
        }
        if (level == 0) current.remove(enchantment); else current.put(enchantment, level);
        EnchantmentHelper.setEnchantments(current, stored);
        if (!player.getAbilities().instabuild && delta != 0) player.giveExperienceLevels(-delta);
        if (delta > 0) {
            player.level().playSound(null, player.blockPosition(), ModSounds.HVK_ENCHANT_INFUSION_UPGRADE.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (delta < 0) {
            player.level().playSound(null, player.blockPosition(), ModSounds.HVK_ENCHANT_INFUSION_DOWNGRADE_OR_CLEAR.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        setChanged();
        open(player);
    }

    public void applyAffix(ServerPlayer player, String affixId, int requestedValue) {
        if (!accessible(player) || stored.isEmpty() || !ApotheosisCompat.isAffixEditingAvailable()) return;
        int max = ApotheosisCompat.affixMaxValue();
        if (max <= 0) return;
        int value = Math.max(0, Math.min(max, requestedValue));
        if (value > 0 && !ApotheosisCompat.canApplyAffix(stored, affixId)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.hvk_enchant_infusion.affix_not_applicable"), true);
            return;
        }
        if (value > 0 && !player.getAbilities().instabuild && player.totalExperience < value) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.hvk_enchant_infusion.no_experience_points", value), true);
            return;
        }
        if (!ApotheosisCompat.applyAffix(stored, affixId, value)) return;
        if (!player.getAbilities().instabuild && value > 0) player.giveExperiencePoints(-value);
        player.level().playSound(null, player.blockPosition(), value > 0
                        ? ModSounds.HVK_ENCHANT_INFUSION_UPGRADE.get()
                        : ModSounds.HVK_ENCHANT_INFUSION_DOWNGRADE_OR_CLEAR.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        setChanged();
        open(player);
    }

    public void dropStoredItem() {
        if (level != null && !level.isClientSide && !stored.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stored);
            stored = ItemStack.EMPTY;
        }
    }

    private boolean accessible(ServerPlayer player) {
        return player.level() == level && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stored.isEmpty()) tag.put("Stored", stored.save(new CompoundTag()));
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.contains("Stored") ? ItemStack.of(tag.getCompound("Stored")) : ItemStack.EMPTY;
    }
}
