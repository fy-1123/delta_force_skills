package com.rzy.dealt_force_skills.character.rover;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.RoverFieldEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class RoverEvents {
    private RoverEvents() {}

    @SubscribeEvent public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) RoverStateManager.tick(player);
    }

    @SubscribeEvent public static void tickLiving(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) RoverStateManager.tickHealth(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damage(LivingHurtEvent event) {
        if (RoverStateManager.executingDeath(event.getEntity())) return;
        if (event.getEntity().hasEffect(ModEffects.ROVER_VULNERABLE.get()))
            event.setAmount(event.getAmount() * (float) (1 + Math.max(0, RoverConfig.SMOKE_VULNERABILITY)));
        if (RoverStateManager.isDowned(event.getEntity()))
            event.setAmount(event.getAmount() * (float) Math.max(0, 1 - RoverConfig.DOWNED_DAMAGE_REDUCTION));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void fatalDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0 && event.getAmount() >= event.getEntity().getHealth()
                && RoverStateManager.preventDeath(event.getEntity(), event.getSource())) event.setAmount(0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent event) {
        if (RoverStateManager.preventDeath(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void deathCleanup(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RoverStateManager.cleanup(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void effect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModEffects.ROVER_DOWNED.get()) event.setResult(Event.Result.ALLOW);
        else if (event.getEntity().hasEffect(ModEffects.ROVER_PURIFIED.get())
                && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void removeEffect(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.ROVER_DOWNED.get()
                && RoverStateManager.data(event.getEntity()).getLong("DownedUntil") > 0
                && !RoverStateManager.executingDeath(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (event.getNewTarget() == null) return;
        for (RoverFieldEntity field : event.getEntity().level().getEntitiesOfClass(RoverFieldEntity.class,
                event.getEntity().getBoundingBox().minmax(event.getNewTarget().getBoundingBox()).inflate(RoverConfig.SMOKE_RADIUS))) {
            if (field.conceals(event.getNewTarget()) && field.blocksSight(event.getEntity().getEyePosition(), event.getNewTarget().getEyePosition())) {
                event.setCanceled(true); return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void attack(AttackEntityEvent event) {
        if (RoverStateManager.isDowned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interact(PlayerInteractEvent event) {
        if (event.isCancelable() && RoverStateManager.isDowned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoverStateManager.finishDog(player);
            RoverStateManager.data(player).putInt("Tool", 0);
            RoverStateManager.sync(player);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var wolf = RoverStateManager.dog(player);
            if (wolf != null) wolf.discard();
            RoverStateManager.finishDog(player);
            RoverStateManager.data(player).putInt("Tool", 0);
            RoverStateManager.data(player).putInt("RescueTicks", 0);
            RoverStateManager.data(player).remove("RescueTarget");
            com.rzy.dealt_force_skills.compat.PlayerReviveCompat.clearRoverBleeding(player);
        }
    }
}
