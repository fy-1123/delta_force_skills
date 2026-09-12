package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import com.rzy.dealt_force_skills.network.C2S_UseCommanderInheritedSkill;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientCommanderState {
    private static long tacticalPoints;
    private static final Map<CommanderSkillChoice, String> SOURCES = new EnumMap<>(CommanderSkillChoice.class);
    private static final Map<CommanderUpgrade, Integer> UPGRADES = new EnumMap<>(CommanderUpgrade.class);
    private static final Map<CommanderSupport, Integer> COOLDOWNS = new EnumMap<>(CommanderSupport.class);
    private static final Map<CommanderBranchUpgrade, Boolean> BRANCH_UPGRADES =
            new EnumMap<>(CommanderBranchUpgrade.class);
    private static final Set<String> ENABLED_PASSIVES = new HashSet<>();
    private static final EnumSet<CommanderSupport> READY_PLAYED = EnumSet.noneOf(CommanderSupport.class);
    private static long revision;

    private ClientCommanderState() {
    }

    public static void reset() {
        tacticalPoints = 0L;
        SOURCES.clear();
        UPGRADES.clear();
        COOLDOWNS.clear();
        BRANCH_UPGRADES.clear();
        ENABLED_PASSIVES.clear();
        READY_PLAYED.clear();
        revision++;
    }

    public static void sync(long points, String[] sources, String[] enabledPassives, int[] upgrades,
                            int[] cooldowns, boolean[] branchUpgrades) {
        tacticalPoints = Math.max(0L, points);
        CommanderSkillChoice[] choices = CommanderSkillChoice.values();
        for (int i = 0; i < choices.length; i++) {
            SOURCES.put(choices[i], i < sources.length ? sources[i] : "");
        }
        ENABLED_PASSIVES.clear();
        for (String passive : enabledPassives) {
            if (passive != null && !passive.isBlank()) {
                ENABLED_PASSIVES.add(passive);
            }
        }
        CommanderUpgrade[] upgradeValues = CommanderUpgrade.values();
        for (int i = 0; i < upgradeValues.length; i++) {
            UPGRADES.put(upgradeValues[i], i < upgrades.length ? upgrades[i] : 0);
        }
        CommanderSupport[] supportValues = CommanderSupport.values();
        for (int i = 0; i < supportValues.length; i++) {
            setCooldown(supportValues[i], i < cooldowns.length ? cooldowns[i] : 0);
        }
        CommanderBranchUpgrade[] branchValues = CommanderBranchUpgrade.values();
        for (int i = 0; i < branchValues.length; i++) {
            BRANCH_UPGRADES.put(branchValues[i], i < branchUpgrades.length && branchUpgrades[i]);
        }
        revision++;
    }

    public static boolean shouldRender() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID);
    }

    public static long tacticalPoints() {
        return tacticalPoints;
    }

    public static String source(CommanderSkillChoice choice) {
        return SOURCES.getOrDefault(choice, "");
    }

    public static void setSource(CommanderSkillChoice choice, String source) {
        SOURCES.put(choice, source == null ? "" : source);
        revision++;
    }

    public static long revision() {
        return revision;
    }

    public static int skillCost(int baseCost) {
        if (!shouldRender() || baseCost <= 0) {
            return baseCost;
        }
        return Math.max(1, (int) Math.ceil(baseCost * 0.5D));
    }

    public static boolean inherits(String characterId) {
        if (!shouldRender() || characterId == null) {
            return false;
        }
        return characterId.equals(source(CommanderSkillChoice.ACTIVE_1))
                || characterId.equals(source(CommanderSkillChoice.ACTIVE_2))
                || characterId.equals(source(CommanderSkillChoice.CORE_1))
                || characterId.equals(source(CommanderSkillChoice.CORE_2))
                || isPassiveEnabled(characterId);
    }

    public static boolean isPassiveEnabled(String characterId) {
        if (characterId == null) {
            return false;
        }
        if (passiveLimitUnlocked()) {
            return ENABLED_PASSIVES.contains(characterId);
        }
        return characterId.equals(source(CommanderSkillChoice.PASSIVE_1))
                || characterId.equals(source(CommanderSkillChoice.PASSIVE_2))
                || characterId.equals(source(CommanderSkillChoice.PASSIVE_3))
                || characterId.equals(source(CommanderSkillChoice.PASSIVE_4));
    }

    public static int enabledPassiveCount() {
        if (passiveLimitUnlocked()) {
            return ENABLED_PASSIVES.size();
        }
        Set<String> selected = new HashSet<>();
        selected.add(source(CommanderSkillChoice.PASSIVE_1));
        selected.add(source(CommanderSkillChoice.PASSIVE_2));
        selected.add(source(CommanderSkillChoice.PASSIVE_3));
        selected.add(source(CommanderSkillChoice.PASSIVE_4));
        selected.remove("");
        return selected.size();
    }

    public static boolean passiveLimitUnlocked() {
        return upgradeLevel(CommanderUpgrade.GENE) >= CommanderUpgrade.GENE.maxLevel();
    }

    public static boolean setPassiveEnabled(String characterId, boolean enabled) {
        if (characterId == null || characterId.isBlank()) {
            return false;
        }
        if (passiveLimitUnlocked()) {
            boolean changed = enabled ? ENABLED_PASSIVES.add(characterId) : ENABLED_PASSIVES.remove(characterId);
            if (changed) {
                revision++;
            }
            return true;
        }
        CommanderSkillChoice[] choices = {
                CommanderSkillChoice.PASSIVE_1, CommanderSkillChoice.PASSIVE_2,
                CommanderSkillChoice.PASSIVE_3, CommanderSkillChoice.PASSIVE_4
        };
        for (CommanderSkillChoice choice : choices) {
            if (characterId.equals(source(choice))) {
                if (!enabled) {
                    SOURCES.put(choice, "");
                    revision++;
                }
                return true;
            }
        }
        if (!enabled || enabledPassiveCount() >= 4) {
            return !enabled;
        }
        for (CommanderSkillChoice choice : choices) {
            if (source(choice).isBlank()) {
                SOURCES.put(choice, characterId);
                revision++;
                return true;
            }
        }
        return false;
    }

    public static boolean handlesSkill(String characterId, SkillSlot slot) {
        if (!shouldRender() || characterId == null || slot == null) {
            return false;
        }
        return switch (slot) {
            case ACTIVE_1 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_1));
            case ACTIVE_2 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_2));
            case CORE -> characterId.equals(source(CommanderSkillChoice.CORE_1))
                    || characterId.equals(source(CommanderSkillChoice.CORE_2));
            default -> false;
        };
    }

    public static boolean handlesSkill(String characterId, SkillSlot slot, boolean alternate) {
        if (!shouldRender() || characterId == null || slot == null) {
            return false;
        }
        return switch (slot) {
            case ACTIVE_1 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_1));
            case ACTIVE_2 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_2));
            case CORE -> characterId.equals(source(alternate
                    ? CommanderSkillChoice.CORE_2 : CommanderSkillChoice.CORE_1));
            default -> false;
        };
    }

    public static boolean useInheritedSkill(String characterId, SkillSlot slot, boolean inheritedAlternate) {
        CommanderSkillChoice choice = selectedChoice(characterId, slot);
        if (choice == null) {
            return false;
        }
        NetworkHandler.sendToServer(new C2S_UseCommanderInheritedSkill(choice, inheritedAlternate));
        return true;
    }

    public static void useNativeOrInherited(String characterId, SkillSlot slot, boolean inheritedAlternate) {
        if (!useInheritedSkill(characterId, slot, inheritedAlternate)) {
            ClientCharacterSelectionState.useSkill(slot, inheritedAlternate);
        }
    }

    private static CommanderSkillChoice selectedChoice(String characterId, SkillSlot slot) {
        if (!shouldRender() || characterId == null || slot == null) {
            return null;
        }
        return switch (slot) {
            case ACTIVE_1 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_1))
                    ? CommanderSkillChoice.ACTIVE_1 : null;
            case ACTIVE_2 -> characterId.equals(source(CommanderSkillChoice.ACTIVE_2))
                    ? CommanderSkillChoice.ACTIVE_2 : null;
            case CORE -> characterId.equals(source(CommanderSkillChoice.CORE_1))
                    ? CommanderSkillChoice.CORE_1
                    : characterId.equals(source(CommanderSkillChoice.CORE_2))
                    ? CommanderSkillChoice.CORE_2 : null;
            default -> null;
        };
    }

    public static int skillCooldownTicks(CommanderSkillChoice choice) {
        String source = source(choice);
        SkillSlot slot = choice.skillSlot();
        if (ModCharacters.ROVER_ID.equals(source)) return com.rzy.dealt_force_skills.client.RoverClient.cooldown(slot);
        if (ModCharacters.SINEVA_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientSinevaHudState.bladeWireCharges(),
                        ClientSinevaHudState.bladeWireRechargeTicks());
                case ACTIVE_2 -> ClientSinevaHudState.grappleCooldownTicks();
                case CORE -> ClientSinevaHudState.bombSuitCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.ULURU_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientUluruHudState.incendiaryCharges(),
                        ClientUluruHudState.incendiaryRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientUluruHudState.coverCharges(),
                        ClientUluruHudState.coverRechargeTicks());
                case CORE -> ClientUluruHudState.missileCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.D_WOLF_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientDWolfHudState.handCannonCharges(),
                        ClientDWolfHudState.handCannonRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientDWolfHudState.smokeCharges(),
                        ClientDWolfHudState.smokeRechargeTicks());
                case CORE -> ClientDWolfHudState.overloadCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.GIZMO_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientGizmoHudState.smokeCharges(),
                        ClientGizmoHudState.smokeRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientGizmoHudState.spiderCharges(),
                        ClientGizmoHudState.spiderRechargeTicks());
                case CORE -> ClientGizmoHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.SHEPHERD_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientShepherdHudState.trapCharges(),
                        ClientShepherdHudState.trapRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientShepherdHudState.fragCharges(),
                        ClientShepherdHudState.fragRechargeTicks());
                case CORE -> ClientShepherdHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.N_TWO_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientNTwoHudState.trackingCooldownTicks();
                case ACTIVE_2 -> chargeCooldown(ClientNTwoHudState.dewarCharges(),
                        ClientNTwoHudState.dewarRechargeTicks());
                case CORE -> ClientNTwoHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.LUNA_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientLunaHudState.shockCharges(),
                        ClientLunaHudState.shockRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientLunaHudState.grenadeCharges(),
                        ClientLunaHudState.grenadeRechargeTicks());
                case CORE -> ClientLunaHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.HACKCLAW_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientHackclawHudState.knifeCharges(),
                        ClientHackclawHudState.knifeRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientHackclawHudState.flashDroneCharges(),
                        ClientHackclawHudState.flashDroneRechargeTicks());
                case CORE -> ClientHackclawHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.VYRON_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientVyronHudState.dashCooldownTicks();
                case ACTIVE_2 -> chargeCooldown(ClientVyronHudState.bombCharges(),
                        ClientVyronHudState.bombRechargeTicks());
                case CORE -> ClientVyronHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.STINGER_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientStingerHudState.smokeCooldownTicks();
                case ACTIVE_2 -> ClientStingerHudState.droneCooldownTicks();
                case CORE -> chargeCooldown(ClientStingerHudState.stimCharges(),
                        ClientStingerHudState.stimRechargeTicks());
                default -> 0;
            };
        }
        if (ModCharacters.NOX_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientNoxHudState.rotorCooldownTicks();
                case ACTIVE_2 -> chargeCooldown(ClientNoxHudState.flashCharges(),
                        ClientNoxHudState.flashRechargeTicks());
                case CORE -> ClientNoxHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.MORSE_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientMorseHudState.shockCharges(),
                        ClientMorseHudState.shockRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientMorseHudState.flashCharges(),
                        ClientMorseHudState.flashRechargeTicks());
                case CORE -> ClientMorseHudState.sonarCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.TOXIK_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientToxikHudState.adrenalineCooldownTicks();
                case ACTIVE_2 -> chargeCooldown(ClientToxikHudState.tearGasCharges(),
                        ClientToxikHudState.tearGasRechargeTicks());
                case CORE -> ClientToxikHudState.fireflyCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.RAPTOR_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientRaptorHudState.falconCooldownTicks();
                case ACTIVE_2 -> chargeCooldown(ClientRaptorHudState.pulseCharges(),
                        ClientRaptorHudState.pulseRechargeTicks());
                case CORE -> ClientRaptorHudState.hummingbirdCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.VLINDER_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientVlinderHudState.medicalCharges(),
                        ClientVlinderHudState.medicalRechargeTicks());
                case ACTIVE_2 -> chargeCooldown(ClientVlinderHudState.smokeCharges(),
                        ClientVlinderHudState.smokeRechargeTicks());
                case CORE -> ClientVlinderHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        if (ModCharacters.TEMPEST_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> chargeCooldown(ClientTempestHudState.wallCharges(),
                        ClientTempestHudState.wallRechargeTicks());
                case ACTIVE_2 -> ClientTempestHudState.rollCooldownTicks();
                case CORE -> ClientTempestHudState.coreCooldownTicks();
                default -> 0;
            };
        }
        return 0;
    }

    private static int chargeCooldown(int charges, int rechargeTicks) {
        return charges > 0 ? 0 : Math.max(0, rechargeTicks);
    }

    public static int upgradeLevel(CommanderUpgrade upgrade) {
        return UPGRADES.getOrDefault(upgrade, 0);
    }

    public static int cooldownTicks(CommanderSupport support) {
        return COOLDOWNS.getOrDefault(support, 0);
    }

    public static boolean ownsBranchUpgrade(CommanderBranchUpgrade upgrade) {
        return BRANCH_UPGRADES.getOrDefault(upgrade, false);
    }

    public static void tick() {
        for (CommanderSupport support : CommanderSupport.values()) {
            int ticks = COOLDOWNS.getOrDefault(support, 0);
            if (ticks > 0) {
                setCooldown(support, ticks - 1);
            }
        }
    }

    private static void setCooldown(CommanderSupport support, int requestedTicks) {
        int next = Math.max(0, requestedTicks);
        Integer previous = COOLDOWNS.put(support, next);
        if (next > 0 && (previous == null || previous <= 0)) {
            READY_PLAYED.remove(support);
        } else if (previous != null && previous > 0 && next == 0 && READY_PLAYED.add(support)) {
            playReadySound(support);
        }
    }

    private static void playReadySound(CommanderSupport support) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        net.minecraft.sounds.SoundEvent sound = switch (support) {
            case AREA_SMOKE -> ModSounds.COMMANDER_SMOKE_READY.get();
            case ARTILLERY_POSITION -> ModSounds.COMMANDER_ARTILLERY_READY.get();
            case VEHICLE_AIRDROP -> ModSounds.COMMANDER_VEHICLE_READY.get();
            case TACTICAL_RESPAWN_BEACON -> ModSounds.COMMANDER_BEACON_READY.get();
            case GUIDED_MISSILE -> ModSounds.COMMANDER_MISSILE_READY.get();
            default -> null;
        };
        if (sound != null) {
            player.playSound(sound, 1.0F, 1.0F);
        }
    }
}
