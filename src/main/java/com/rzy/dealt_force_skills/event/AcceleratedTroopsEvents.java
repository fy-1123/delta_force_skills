package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.boss.AcceleratedTroopsSquadManager;
import com.rzy.dealt_force_skills.entity.RespawnBeaconMarkerEntity;
import com.rzy.dealt_force_skills.item.PortableAdsDeviceItem;
import com.rzy.dealt_force_skills.item.RespawnBeaconItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class AcceleratedTroopsEvents {
    private static final TagKey<DamageType> TACZ_BULLETS = TagKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("tacz", "bullets"));

    private AcceleratedTroopsEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PortableAdsDeviceItem.isActive(player)) {
            return;
        }
        if (shouldBlock(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PortableAdsDeviceItem.isActive(player)
                && shouldBlock(player, event.getSource())) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RespawnBeaconMarkerEntity marker = RespawnBeaconItem.marker(player);
        if (marker == null || !(marker.level() instanceof ServerLevel markerLevel)) {
            return;
        }
        event.setCanceled(true);
        player.teleportTo(markerLevel, marker.getX(), marker.getY(), marker.getZ(),
                player.getYRot(), player.getXRot());
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.removeAllEffects();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isDamageableItem()) {
                stack.setDamageValue(0);
            }
        }
        RespawnBeaconItem.consumeMarker(player, marker);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide
                && event.player instanceof ServerPlayer player) {
            RespawnBeaconItem.displayMarkerStatus(player);
            PortableAdsDeviceItem.displayActiveStatus(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AcceleratedTroopsSquadManager.tickServer(event.getServer());
        }
    }

    private static boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        String messageId = source.getMsgId();
        return attacker instanceof LivingEntity && source.getDirectEntity() == attacker
                && ("mob".equals(messageId) || "player".equals(messageId));
    }

    private static boolean shouldBlock(ServerPlayer player, DamageSource source) {
        return !isMelee(source) && !(source.is(TACZ_BULLETS)
                && PortableAdsDeviceItem.consumeTaczHitAllowance(player, source));
    }
}