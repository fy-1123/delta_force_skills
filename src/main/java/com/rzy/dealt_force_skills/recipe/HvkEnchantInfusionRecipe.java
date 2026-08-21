package com.rzy.dealt_force_skills.recipe;

import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HvkEnchantInfusionRecipe extends CustomRecipe {
    public HvkEnchantInfusionRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    @Override public boolean matches(CraftingContainer container, Level level) {
        List<CompoundTag> distinct = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(Items.ENCHANTED_BOOK)) return false;
            CompoundTag tag = stack.getTag();
            if (distinct.stream().anyMatch(existing -> Objects.equals(existing, tag))) return false;
            distinct.add(tag == null ? null : tag.copy());
        }
        return distinct.size() == 9;
    }

    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        return new ItemStack(ModItems.HVK_ENCHANT_INFUSION.get());
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 9; }
    @Override public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(9, Ingredient.of(Items.ENCHANTED_BOOK));
    }
    @Override public boolean isSpecial() { return false; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.HVK_ENCHANT_INFUSION.get(); }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(ModItems.HVK_ENCHANT_INFUSION.get())) {
            return;
        }
        float luck = Math.max(0.0F, event.getEntity().getLuck());
        float failureChance = Math.max(0.04F, 0.99F - luck * 0.05F);
        if (event.getEntity().getRandom().nextFloat() < failureChance) {
            event.getCrafting().setCount(0);
            event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.hvk_enchant_infusion.craft_failed"), true);
        }
    }
}
