package com.rzy.dealt_force_skills.character.rover;

import com.rzy.dealt_force_skills.config.DealtForceConfig;

public final class RoverConfig {
    public static volatile int SMOKE_COOLDOWN = DealtForceConfig.bind("SMOKE_COOLDOWN", () -> DealtForceConfig.intValue("characters.rover.smoke_cooldown", 600));
    public static volatile int SMOKE_CHARGES = DealtForceConfig.bind("SMOKE_CHARGES", () -> DealtForceConfig.intValue("characters.rover.smoke_charges", 2));
    public static volatile int SMOKE_DURATION = DealtForceConfig.bind("SMOKE_DURATION", () -> DealtForceConfig.intValue("characters.rover.smoke_duration", 300));
    public static volatile double SMOKE_RADIUS = DealtForceConfig.bind("SMOKE_RADIUS", () -> DealtForceConfig.doubleValue("characters.rover.smoke_radius", 5.0));
    public static volatile double SMOKE_MOB_FRACTION = DealtForceConfig.bind("SMOKE_MOB_FRACTION", () -> DealtForceConfig.doubleValue("characters.rover.smoke_mob_fraction", 0.04));
    public static volatile double SMOKE_VULNERABILITY = DealtForceConfig.bind("SMOKE_VULNERABILITY", () -> DealtForceConfig.doubleValue("characters.rover.smoke_vulnerability", 0.50));
    public static volatile int SMOKE_VULNERABILITY_DURATION = DealtForceConfig.bind("SMOKE_VULNERABILITY_DURATION", () -> DealtForceConfig.intValue("characters.rover.smoke_vulnerability_duration", 300));
    public static volatile int SMOKE_DEBUFF_DURATION = DealtForceConfig.bind("SMOKE_DEBUFF_DURATION", () -> DealtForceConfig.intValue("characters.rover.smoke_debuff_duration", 40));
    public static volatile int SMOKE_NAUSEA_AMPLIFIER = DealtForceConfig.bind("SMOKE_NAUSEA_AMPLIFIER", () -> DealtForceConfig.intValue("characters.rover.smoke_nausea_amplifier", 2));
    public static volatile int WITHER_AMPLIFIER = DealtForceConfig.bind("WITHER_AMPLIFIER", () -> DealtForceConfig.intValue("characters.rover.wither_amplifier", 1));
    public static volatile int INJECTOR_COOLDOWN = DealtForceConfig.bind("INJECTOR_COOLDOWN", () -> DealtForceConfig.intValue("characters.rover.injector_cooldown", 700));
    public static volatile int INJECTOR_CHARGES = DealtForceConfig.bind("INJECTOR_CHARGES", () -> DealtForceConfig.intValue("characters.rover.injector_charges", 2));
    public static volatile double INJECTOR_DAMAGE = DealtForceConfig.bind("INJECTOR_DAMAGE", () -> DealtForceConfig.doubleValue("characters.rover.injector_damage", 8.0));
    public static volatile double INJECTOR_DIRECT_REDUCTION = DealtForceConfig.bind("INJECTOR_DIRECT_REDUCTION", () -> DealtForceConfig.doubleValue("characters.rover.injector_direct_reduction", 0.40));
    public static volatile double INJECTOR_TICK_REDUCTION = DealtForceConfig.bind("INJECTOR_TICK_REDUCTION", () -> DealtForceConfig.doubleValue("characters.rover.injector_tick_reduction", 0.08));
    public static volatile int INJECTOR_DURATION = DealtForceConfig.bind("INJECTOR_DURATION", () -> DealtForceConfig.intValue("characters.rover.injector_duration", 200));
    public static volatile double INJECTOR_RADIUS = DealtForceConfig.bind("INJECTOR_RADIUS", () -> DealtForceConfig.doubleValue("characters.rover.injector_radius", 4.0));
    public static volatile int INJECTOR_RESTORE_DELAY = DealtForceConfig.bind("INJECTOR_RESTORE_DELAY", () -> DealtForceConfig.intValue("characters.rover.injector_restore_delay", 20));
    public static volatile int INJECTOR_SLOW_AMPLIFIER = DealtForceConfig.bind("INJECTOR_SLOW_AMPLIFIER", () -> DealtForceConfig.intValue("characters.rover.injector_slow_amplifier", 2));
    public static volatile double INJECTOR_SPEED = DealtForceConfig.bind("INJECTOR_SPEED", () -> DealtForceConfig.doubleValue("characters.rover.injector_speed", 2.2));
    public static volatile int PROTECTION_DURATION = DealtForceConfig.bind("PROTECTION_DURATION", () -> DealtForceConfig.intValue("characters.rover.protection_duration", 1200));
    public static volatile double TOTEM_HEALTH = DealtForceConfig.bind("TOTEM_HEALTH", () -> DealtForceConfig.doubleValue("characters.rover.totem_health", 1.0));
    public static volatile int TOTEM_REGEN_DURATION = DealtForceConfig.bind("TOTEM_REGEN_DURATION", () -> DealtForceConfig.intValue("characters.rover.totem_regen_duration", 900));
    public static volatile int TOTEM_ABSORPTION_DURATION = DealtForceConfig.bind("TOTEM_ABSORPTION_DURATION", () -> DealtForceConfig.intValue("characters.rover.totem_absorption_duration", 100));
    public static volatile int TOTEM_FIRE_RESISTANCE_DURATION = DealtForceConfig.bind("TOTEM_FIRE_RESISTANCE_DURATION", () -> DealtForceConfig.intValue("characters.rover.totem_fire_resistance_duration", 800));
    public static volatile int DOWNED_DURATION = DealtForceConfig.bind("DOWNED_DURATION", () -> DealtForceConfig.intValue("characters.rover.downed_duration", 1500));
    public static volatile double DOWNED_MOVE_REDUCTION = DealtForceConfig.bind("DOWNED_MOVE_REDUCTION", () -> DealtForceConfig.doubleValue("characters.rover.downed_move_reduction", 0.70));
    public static volatile double DOWNED_DAMAGE_REDUCTION = DealtForceConfig.bind("DOWNED_DAMAGE_REDUCTION", () -> DealtForceConfig.doubleValue("characters.rover.downed_damage_reduction", 0.50));
    public static volatile int RESCUE_TICKS = DealtForceConfig.bind("RESCUE_TICKS", () -> DealtForceConfig.intValue("characters.rover.rescue_ticks", 121));
    public static volatile int SELF_RESCUE_TICKS = DealtForceConfig.bind("SELF_RESCUE_TICKS", () -> DealtForceConfig.intValue("characters.rover.self_rescue_ticks", 60));
    public static volatile double RESCUE_RADIUS = DealtForceConfig.bind("RESCUE_RADIUS", () -> DealtForceConfig.doubleValue("characters.rover.rescue_radius", 2.0));
    public static volatile double TEAM_DOWNED_BONUS = DealtForceConfig.bind("TEAM_DOWNED_BONUS", () -> DealtForceConfig.doubleValue("characters.rover.team_downed_bonus", 0.50));
    public static volatile int CORE_COOLDOWN = DealtForceConfig.bind("CORE_COOLDOWN", () -> DealtForceConfig.intValue("characters.rover.core_cooldown", 1500));
    public static volatile int CORE_DURATION = DealtForceConfig.bind("CORE_DURATION", () -> DealtForceConfig.intValue("characters.rover.core_duration", 900));
    public static volatile double DOG_HEALTH_MULTIPLIER = DealtForceConfig.bind("DOG_HEALTH_MULTIPLIER", () -> DealtForceConfig.doubleValue("characters.rover.dog_health_multiplier", 1.5));
    public static volatile double DOG_DAMAGE = DealtForceConfig.bind("DOG_DAMAGE", () -> DealtForceConfig.doubleValue("characters.rover.dog_damage", 8.0));
    public static volatile double DOG_TARGET_RANGE = DealtForceConfig.bind("DOG_TARGET_RANGE", () -> DealtForceConfig.doubleValue("characters.rover.dog_target_range", 50.0));
    public static volatile int DOG_REVEAL_DURATION = DealtForceConfig.bind("DOG_REVEAL_DURATION", () -> DealtForceConfig.intValue("characters.rover.dog_reveal_duration", 40));
    public static volatile double DOG_INJURY_CHANCE = DealtForceConfig.bind("DOG_INJURY_CHANCE", () -> DealtForceConfig.doubleValue("characters.rover.dog_injury_chance", 0.50));
    public static volatile int DOG_INJURY_DURATION = DealtForceConfig.bind("DOG_INJURY_DURATION", () -> DealtForceConfig.intValue("characters.rover.dog_injury_duration", 1200));
    public static volatile double COMMAND_RANGE = DealtForceConfig.bind("COMMAND_RANGE", () -> DealtForceConfig.doubleValue("characters.rover.command_range", 128.0));
    public static volatile double SMOKE_LOW_SPEED = DealtForceConfig.bind("SMOKE_LOW_SPEED", () -> DealtForceConfig.doubleValue("characters.rover.smoke_low_speed", 1.1));
    public static volatile double SMOKE_HIGH_SPEED = DealtForceConfig.bind("SMOKE_HIGH_SPEED", () -> DealtForceConfig.doubleValue("characters.rover.smoke_high_speed", 1.8));
    public static volatile double SMOKE_LOW_LIFT = DealtForceConfig.bind("SMOKE_LOW_LIFT", () -> DealtForceConfig.doubleValue("characters.rover.smoke_low_lift", 0.08));
    public static volatile double SMOKE_HIGH_LIFT = DealtForceConfig.bind("SMOKE_HIGH_LIFT", () -> DealtForceConfig.doubleValue("characters.rover.smoke_high_lift", 0.34));
    public static volatile int HOLD_TICKS = DealtForceConfig.bind("HOLD_TICKS", () -> DealtForceConfig.intValue("characters.rover.hold_ticks", 8));

    private RoverConfig() {}
}
