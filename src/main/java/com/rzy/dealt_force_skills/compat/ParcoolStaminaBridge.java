package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParcoolStaminaBridge {
    private static final String STAMINA_API_CLASS = "com.alrex.parcool.api.Stamina";
    private static final String SYNC_MESSAGE_CLASS = "com.alrex.parcool.common.network.SyncStaminaMessage";
    private static final String SYNC_TO_CLIENT_CLASS = "com.alrex.parcool.common.network.SyncStaminaToClientMessage";
    private static final String PARKOURABILITY_CLASS = "com.alrex.parcool.common.capability.Parkourability";
    private static final Map<UUID, Long> ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL = new HashMap<>();
    private static Method staminaGet;
    private static Method staminaGetMax;
    private static Method staminaGetValue;
    private static Method staminaIsExhausted;
    private static Method staminaConsume;
    private static Method staminaRecover;
    private static Method syncToServer;
    private static Method syncToClient;
    private static Method parkourabilityGet;
    private static Method parkourabilityIsDoingNothing;
    private static boolean reflectionResolved;

    private ParcoolStaminaBridge() {
    }

    public static ConsumeResult consumeLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, staminaGetMax, -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        return consumeLocal(stamina, player, amount);
    }

    public static ConsumeResult consumeLocalPercentAboveFloor(Player player, int percent, int floorPercent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, staminaGetMax, -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int value = invokeInt(stamina, staminaGetValue, -1);
        boolean exhausted = invokeBoolean(stamina, staminaIsExhausted, false);
        if (value <= 0 || exhausted) {
            return ConsumeResult.NOT_ENOUGH;
        }

        int floor = Math.max(0, Math.min(maxValue, (maxValue * floorPercent + 99) / 100));
        int available = Math.max(0, value - floor);
        if (available <= 0) {
            return ConsumeResult.NOT_ENOUGH;
        }

        int requested = Math.max(1, (maxValue * percent + 99) / 100);
        int amount = Math.min(requested, available);
        if (!invokeVoid(stamina, staminaConsume, amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static ConsumeResult canConsumeLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, staminaGetMax, -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        int value = invokeInt(stamina, staminaGetValue, -1);
        boolean exhausted = invokeBoolean(stamina, staminaIsExhausted, false);
        return value >= amount && !exhausted ? ConsumeResult.SUCCESS : ConsumeResult.NOT_ENOUGH;
    }

    public static ConsumeResult recoverLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }
        if (isActiveRecoverySuppressed(player)) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, staminaGetMax, -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        if (!invokeVoid(stamina, staminaRecover, amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static ConsumeResult recoverLocal(Player player, int amount) {
        if (player == null || amount <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }
        Object stamina = getStamina(player);
        if (stamina == null || !invokeVoid(stamina, staminaRecover, amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static ConsumeResult consumeLocal(Player player, int amount) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        return consumeLocal(stamina, player, amount);
    }

    public static int currentValue(Player player) {
        Object stamina = player == null ? null : getStamina(player);
        return stamina == null ? -1 : invokeInt(stamina, staminaGetValue, -1);
    }

    private static ConsumeResult consumeLocal(Object stamina, Player player, int amount) {
        int value = invokeInt(stamina, staminaGetValue, -1);
        boolean exhausted = invokeBoolean(stamina, staminaIsExhausted, false);
        if (value < amount || exhausted) {
            return ConsumeResult.NOT_ENOUGH;
        }

        if (!invokeVoid(stamina, staminaConsume, amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static void markActiveStaminaUse(Player player, int ticks) {
        if (player == null || ticks <= 0) {
            return;
        }
        long until = SkillCooldownHelper.now(player) + ticks;
        ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.merge(player.getUUID(), until,
                (oldUntil, newUntil) -> Math.max(oldUntil, newUntil));
    }

    public static void clear(Player player) {
        if (player != null) {
            ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.remove(player.getUUID());
        }
    }

    private static Object getStamina(Player player) {
        resolveReflection();
        if (staminaGet == null) {
            return null;
        }
        try {
            return staminaGet.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static boolean isParcoolActionActive(Player player) {
        if (player == null) {
            return false;
        }
        resolveReflection();
        if (parkourabilityGet == null || parkourabilityIsDoingNothing == null) {
            return false;
        }
        try {
            Object parkourability = parkourabilityGet.invoke(null, player);
            Object doingNothing = parkourability == null ? null : parkourabilityIsDoingNothing.invoke(parkourability);
            return doingNothing instanceof Boolean bool && !bool;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isActiveRecoverySuppressed(Player player) {
        Long until = ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (SkillCooldownHelper.now(player) >= until) {
            ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static int invokeInt(Object target, Method method, int fallback) {
        if (method == null) {
            return fallback;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Integer integer ? integer : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static boolean invokeBoolean(Object target, Method method, boolean fallback) {
        if (method == null) {
            return fallback;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static boolean invokeVoid(Object target, Method method, int value) {
        if (method == null) {
            return false;
        }
        try {
            method.invoke(target, value);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void syncLocal(Player player) {
        resolveReflection();
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                if (syncToClient != null) {
                    syncToClient.invoke(null, serverPlayer);
                }
            } else if (syncToServer != null) {
                syncToServer.invoke(null, player);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Parcool performs periodic stamina sync; this immediate sync is only a latency reduction.
        }
    }

    private static synchronized void resolveReflection() {
        if (reflectionResolved) {
            return;
        }
        reflectionResolved = true;
        try {
            Class<?> stamina = Class.forName(STAMINA_API_CLASS);
            staminaGet = stamina.getMethod("get", Player.class);
            staminaGetMax = stamina.getMethod("getMaxValue");
            staminaGetValue = stamina.getMethod("getValue");
            staminaIsExhausted = stamina.getMethod("isExhausted");
            staminaConsume = stamina.getMethod("consume", int.class);
            staminaRecover = stamina.getMethod("recover", int.class);
            syncToServer = Class.forName(SYNC_MESSAGE_CLASS).getMethod("sync", Player.class);
            syncToClient = Class.forName(SYNC_TO_CLIENT_CLASS).getMethod("requestSync", ServerPlayer.class);
            Class<?> parkourability = Class.forName(PARKOURABILITY_CLASS);
            parkourabilityGet = parkourability.getMethod("get", Player.class);
            parkourabilityIsDoingNothing = parkourability.getMethod("isDoingNothing");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            staminaGet = null;
        }
    }

    public enum ConsumeResult {
        SUCCESS,
        NOT_ENOUGH,
        UNAVAILABLE
    }
}
