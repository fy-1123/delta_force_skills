package com.rzy.dealt_force_skills.network;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Common-side facades for client packet callbacks.
 *
 * The network package must be loadable by a dedicated server. These facades
 * intentionally contain only class-name strings; the client implementation is
 * resolved after the packet has reached a client JVM.
 */
final class ClientPacketBridge {
    private ClientPacketBridge() {
    }

    static void invoke(String className, String methodName, Object... args) {
        try {
            Class<?> targetClass = Class.forName(className, false, ClientPacketBridge.class.getClassLoader());
            Method target = findMethod(targetClass, methodName, args);
            if (target == null) {
                throw new NoSuchMethodException(className + '#' + methodName);
            }
            target.setAccessible(true);
            target.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to dispatch client packet callback "
                    + className + '#' + methodName, exception);
        }
    }

    private static Method findMethod(Class<?> targetClass, String methodName, Object[] args) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || !method.getName().equals(methodName)
                    || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!isCompatible(parameterTypes[i], args[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> parameterType, Object argument) {
        if (argument == null) {
            return !parameterType.isPrimitive();
        }
        if (!parameterType.isPrimitive()) {
            return parameterType.isInstance(argument);
        }
        return (parameterType == boolean.class && argument instanceof Boolean)
                || (parameterType == byte.class && argument instanceof Byte)
                || (parameterType == short.class && argument instanceof Short)
                || (parameterType == int.class && argument instanceof Integer)
                || (parameterType == long.class && argument instanceof Long)
                || (parameterType == float.class && argument instanceof Float)
                || (parameterType == double.class && argument instanceof Double)
                || (parameterType == char.class && argument instanceof Character);
    }
}

final class AdvancedWorkBlockScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.AdvancedWorkBlockScreen", "open", args); }
    static void update(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.AdvancedWorkBlockScreen", "update", args); }
}
final class ClientCatDadHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCatDadHudState", "sync", args); }
}
final class ClientChamberHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientChamberHudState", "sync", args); }
}
final class ClientCharacterSelectionState {
    static void openReselectionScreen(boolean free) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "openReselectionScreen", free); }
    static void openReselectionScreen() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "openReselectionScreen"); }
    static void openSelectionScreen() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "openSelectionScreen"); }
    static void syncCharacterAvailability(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "syncCharacterAvailability", args); }
    static void syncSelectedCharacter(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "syncSelectedCharacter", args); }
    static void syncSelectionCatalog(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState", "syncSelectionCatalog", args); }
}
final class ClientCharacterSkinState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState", "sync", args); }
}
final class ClientCommanderState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCommanderState", "sync", args); }
}
final class ClientCommanderVehicleState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCommanderVehicleState", "sync", args); }
}
final class ClientCorpsHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCorpsHudState", "sync", args); }
}
final class ClientDepartmentHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState", "sync", args); }
}
final class ClientDWolfHudState {
    static void restoreStaminaPercent(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDWolfHudState", "restoreStaminaPercent", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDWolfHudState", "sync", args); }
}
final class ClientEvents {
    static void suppressLocalHurtAnimation(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientEvents", "suppressLocalHurtAnimation", args); }
}
final class ClientFearState {
    static void setStacks(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientFearState", "setStacks", args); }
}
final class ClientGamblerHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGamblerHudState", "sync", args); }
}
final class ClientGhrothState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGhrothState", "sync", args); }
}
final class ClientGizmoHudState {
    static void revealEntities(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGizmoHudState", "revealEntities", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGizmoHudState", "sync", args); }
}
final class ClientHackclawCoreVisualState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHackclawCoreVisualState", "sync", args); }
}
final class ClientHackclawHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHackclawHudState", "sync", args); }
}
final class ClientHeldToolVisualState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState", "sync", args); }
}
final class ClientHvkAsaraArenaState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientHvkAsaraArenaState", "sync", args); }
}
final class ClientLexNinjiaHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState", "sync", args); }
    static void syncLotusBoxes(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState", "syncLotusBoxes", args); }
}
final class ClientLunaBowVisualState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState", "sync", args); }
}
final class ClientLunaHudState {
    static void revealEntities(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaHudState", "revealEntities", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaHudState", "sync", args); }
}
final class ClientManbaHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState", "sync", args); }
    static void syncDuelView(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState", "syncDuelView", args); }
    static void syncOpportunityMarkers(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState", "syncOpportunityMarkers", args); }
    static void updateFlashlightProgress(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState", "updateFlashlightProgress", args); }
}
final class ClientMorseHudState {
    static void addMarkers(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientMorseHudState", "addMarkers", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientMorseHudState", "sync", args); }
}
final class ClientNikaidouHiroHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState", "sync", args); }
}
final class ClientNoxHudState {
    static void revealEntity(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNoxHudState", "revealEntity", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNoxHudState", "sync", args); }
}
final class ClientNTwoHudState {
    static void revealEntities(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNTwoHudState", "revealEntities", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNTwoHudState", "sync", args); }
}
final class ClientPacketHandlers {
    static void acceptStartPageSnapshot(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers", "acceptStartPageSnapshot", args); }
    static void characterHitFeedback(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers", "characterHitFeedback", args); }
    static void openCommanderScreen(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers", "openCommanderScreen", args); }
    static void openGtiTradingHall(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers", "openGtiTradingHall", args); }
    static void openSpecialOperationsBase(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers", "openSpecialOperationsBase", args); }
}
final class ClientRaptorHudState {
    static void revealEntities(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState", "revealEntities", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState", "sync", args); }
    static void syncFootprints(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState", "syncFootprints", args); }
}
final class ClientSaeedHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSaeedHudState", "sync", args); }
}
final class ClientShepherdHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientShepherdHudState", "sync", args); }
}
final class ClientSinevaHudState {
    static void consumeShieldBlockStamina(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaHudState", "consumeShieldBlockStamina", args); }
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaHudState", "sync", args); }
}
final class ClientSinevaKnockdownState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.ClientSinevaKnockdownState", "sync", args); }
}
final class ClientSinevaRenderState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState", "sync", args); }
}
final class ClientSkillModelVisualState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState", "sync", args); }
}
final class ClientStingerHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerHudState", "sync", args); }
    static void syncStimLockTargets(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerHudState", "syncStimLockTargets", args); }
}
final class ClientStingerStimLockState {
    static void warn(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerStimLockState", "warn", args); }
}
final class ClientTeammateRevealState {
    static void apply(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTeammateRevealState", "apply", args); }
}
final class ClientTeamSpectatorState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTeamSpectatorState", "sync", args); }
}
final class ClientTempestHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientTempestHudState", "sync", args); }
}
final class ClientTempestRollVisuals {
    static void startRoll() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTempestRollVisuals", "startRoll"); }
}
final class ClientToxikHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientToxikHudState", "sync", args); }
}
final class ClientUluruHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUluruHudState", "sync", args); }
}
final class ClientUndeadHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUndeadHudState", "sync", args); }
    static void syncSouls(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUndeadHudState", "syncSouls", args); }
}
final class ClientVlinderHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientVlinderHudState", "sync", args); }
}
final class ClientVyronHudState {
    static void sync(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientVyronHudState", "sync", args); }
}
final class DealtForceConfigScreen {
    static void acceptServerChunk(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.DealtForceConfigScreen", "acceptServerChunk", args); }
    static void onApplyResult(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.DealtForceConfigScreen", "onApplyResult", args); }
}
final class DWolfInputHandler {
    static void acceptSlide() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.DWolfInputHandler", "acceptSlide"); }
}
final class EternalLoveBlessingScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.EternalLoveBlessingScreen", "open", args); }
}
final class GamblerDuelInviteScreen {
    static void open() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GamblerDuelInviteScreen", "open"); }
}
final class GhrothArmoryScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GhrothArmoryScreen", "open", args); }
    static void updateCoins(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GhrothArmoryScreen", "updateCoins", args); }
}
final class GtiSpecialOperationsScreen {
    static void open() { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GtiSpecialOperationsScreen", "open"); }
}
final class HackclawPathLineRenderer {
    static void setLines(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.HackclawPathLineRenderer", "setLines", args); }
}
final class HaffShopScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HaffShopScreen", "open", args); }
    static void updateCoins(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HaffShopScreen", "updateCoins", args); }
}
final class HelmetVisionClient {
    static void syncLocalHelmetMode(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.HelmetVisionClient", "syncLocalHelmetMode", args); }
}
final class HvkAdvancedSpawnerScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkAdvancedSpawnerScreen", "open", args); }
}
final class HvkAsaraArenaScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkAsaraArenaScreen", "open", args); }
}
final class HvkConstructorScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen", "open", args); }
    static void updateDetail(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen", "updateDetail", args); }
    static void updateRecipes(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen", "updateRecipes", args); }
}
final class HvkEnchantInfusionScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkEnchantInfusionScreen", "open", args); }
}
final class HvkGlueClientCache {
    static void replaceAll(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.HvkGlueClientCache", "replaceAll", args); }
}
final class LexNinjiaPresetScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.LexNinjiaPresetScreen", "open", args); }
}
final class LexNinjiaShopScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.LexNinjiaShopScreen", "open", args); }
}
final class ManbaFlashlightBeamRenderer {
    static void syncBeam(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.ManbaFlashlightBeamRenderer", "syncBeam", args); }
}
final class NTwoFrozenVisuals {
    static void syncFrozenVisual(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.NTwoFrozenVisuals", "syncFrozenVisual", args); }
}
final class RaptorFalconController {
    static void start(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.RaptorFalconController", "start", args); }
    static void stop(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.RaptorFalconController", "stop", args); }
}
final class SaeedMonitorScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.SaeedMonitorScreen", "open", args); }
}
final class SaeedRecruitScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.SaeedRecruitScreen", "open", args); }
}
final class UluruGhostEntityManager {
    static void update(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruGhostEntityManager", "update", args); }
}
final class UluruMissileController {
    static void start(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruMissileController", "start", args); }
    static void stop(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruMissileController", "stop", args); }
}
final class UndeadShopScreen {
    static void open(Object... args) { ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.UndeadShopScreen", "open", args); }
}
