package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.compat.ParcoolStaminaBridge;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class SpecialOperationsBaseEffects {
    private static final UUID MOVEMENT_ID = UUID.fromString("bf07bea6-93d3-49b2-9563-2c621b6e2741");
    private static final UUID SWIM_ID = UUID.fromString("0cd21a56-ab87-4779-93db-440ade722768");
    private static final String LAST_AIR = DealtForceSkillsMod.MODID + ".base.last_air";
    private static final String LAST_STAMINA = DealtForceSkillsMod.MODID + ".base.last_stamina";
    private static final String STAMINA_FRACTION = DealtForceSkillsMod.MODID + ".base.stamina_fraction";
    private static final ThreadLocal<Boolean> RESCALING_EFFECT = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Map<MobEffect, Entity>> PENDING_EFFECTS = new HashMap<>();
    private static final Set<UUID> ACTIVE_PARCOOL_ACTIONS = new HashSet<>();

    private SpecialOperationsBaseEffects() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0) {
            event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - level * 0.03F));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        int level = SpecialOperationsBaseManager.effectiveLevel(event.getEntity(),
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0 && event.getNewSpeed() > 0.0F) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + level * 0.05F));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0 && event.getDuration() > 1 && player.level().getGameTime() % 20L < level) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(event.getEntity(),
                SpecialOperationsBaseManager.Facility.COMMAND_CENTER);
        if (level > 0) {
            event.setAmount(Math.max(1, Math.round(event.getAmount() * (1.0F + level * 0.2F))));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!player.level().isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            tickAttributesAndAir(serverPlayer);
            applyPendingEffects(serverPlayer);
            tickStamina(serverPlayer);
            tickParcoolSpeed(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (RESCALING_EFFECT.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance original = event.getEffectInstance();
        MobEffect effect = original.getEffect();
        if (effect.isInstantenous() || effect.getCategory() == MobEffectCategory.NEUTRAL) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DECONTAMINATION_ROOM);
        if (level > 0) {
            PENDING_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(effect, event.getEffectSource());
        }
    }

    private static void applyPendingEffects(ServerPlayer player) {
        Map<MobEffect, Entity> pending = PENDING_EFFECTS.remove(player.getUUID());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DECONTAMINATION_ROOM);
        if (level <= 0) {
            return;
        }
        for (Map.Entry<MobEffect, Entity> entry : pending.entrySet()) {
            MobEffect effect = entry.getKey();
            MobEffectInstance original = player.getEffect(effect);
            if (original == null) {
                continue;
            }
            rescaleEffect(player, original, entry.getValue(), level);
        }
    }

    private static void rescaleEffect(ServerPlayer player, MobEffectInstance original, Entity source, int level) {
        MobEffect effect = original.getEffect();
        int duration;
        int amplifier;
        if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            duration = scaleDuration(original.getDuration(), Math.max(0.25F, 1.0F - level * 0.25F));
            amplifier = original.getAmplifier() - level;
        } else {
            duration = scaleDuration(original.getDuration(), 1.0F + level * 0.3F);
            amplifier = original.getAmplifier() + (level >= 3 ? 1 : 0);
        }
        RESCALING_EFFECT.set(true);
        try {
            player.removeEffect(effect);
            if (amplifier >= 0) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier, original.isAmbient(),
                        original.isVisible(), original.showIcon()), source);
            }
        } finally {
            RESCALING_EFFECT.set(false);
        }
    }

    private static int scaleDuration(int duration, float multiplier) {
        return duration < 0 ? duration : Math.max(1, Math.round(duration * multiplier));
    }

    private static void tickAttributesAndAir(ServerPlayer player) {
        int training = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.TRAINING_CENTER);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_ID,
                "Special Operations Base training", training * 0.03D);
        int diving = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DIVING_CENTER);
        applyModifier(player.getAttribute(ForgeMod.SWIM_SPEED.get()), SWIM_ID,
                "Special Operations Base swimming", diving * 0.25D);

        int currentAir = player.getAirSupply();
        int previousAir = player.getPersistentData().contains(LAST_AIR)
                ? player.getPersistentData().getInt(LAST_AIR) : currentAir;
        if (diving > 0 && currentAir < previousAir
                && player.level().getGameTime() % 10L < diving * 3L) {
            player.setAirSupply(Math.min(player.getMaxAirSupply(), currentAir + (previousAir - currentAir)));
            currentAir = player.getAirSupply();
        }
        player.getPersistentData().putInt(LAST_AIR, currentAir);
    }

    private static void tickStamina(ServerPlayer player) {
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.TRAINING_CENTER);
        int current = ParcoolStaminaBridge.currentValue(player);
        if (current < 0) {
            player.getPersistentData().remove(LAST_STAMINA);
            return;
        }
        if (level > 0 && player.getPersistentData().contains(LAST_STAMINA)) {
            int previous = player.getPersistentData().getInt(LAST_STAMINA);
            if (current < previous) {
                double refund = (previous - current) * level * 0.07D
                        + player.getPersistentData().getDouble(STAMINA_FRACTION);
                int whole = (int) Math.floor(refund);
                player.getPersistentData().putDouble(STAMINA_FRACTION, refund - whole);
                if (whole > 0) {
                    ParcoolStaminaBridge.recoverLocal(player, whole);
                    current = ParcoolStaminaBridge.currentValue(player);
                }
            }
        }
        player.getPersistentData().putInt(LAST_STAMINA, Math.max(0, current));
    }

    private static void tickParcoolSpeed(ServerPlayer player) {
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DIVING_CENTER);
        UUID playerId = player.getUUID();
        if (level <= 0 || !ParcoolStaminaBridge.isParcoolActionActive(player)) {
            ACTIVE_PARCOOL_ACTIONS.remove(playerId);
            return;
        }
        if (ACTIVE_PARCOOL_ACTIONS.add(playerId)) {
            Vec3 motion = player.getDeltaMovement();
            double multiplier = 1.0D + level * 0.15D;
            player.setDeltaMovement(motion.x * multiplier, motion.y * multiplier, motion.z * multiplier);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayerRuntime(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING_EFFECTS.clear();
        ACTIVE_PARCOOL_ACTIONS.clear();
        RESCALING_EFFECT.remove();
    }

    private static void clearPlayerRuntime(UUID playerId) {
        PENDING_EFFECTS.remove(playerId);
        ACTIVE_PARCOOL_ACTIONS.remove(playerId);
    }

    private static void applyModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && Math.abs(old.getAmount() - amount) < 0.000001D) {
            return;
        }
        if (old != null) {
            attribute.removeModifier(old);
        }
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
