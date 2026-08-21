package com.rzy.dealt_force_skills.arena;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum HvkAsaraArenaModifier {
    ARMOR_I(10, "armor", 1), ARMOR_II(20, "armor", 2), ARMOR_III(25, "armor", 3),
    FORGED_ARMOR(25, "forged_armor", 0, ARMOR_III),
    BLADE_I(10, "blade", 1), BLADE_II(20, "blade", 2), BLADE_III(35, "blade", 3),
    STRONG_BOW_I(10, "strong_bow", 1), STRONG_BOW_II(20, "strong_bow", 2), STRONG_BOW_III(35, "strong_bow", 3),
    WEAPON_FORGING(25, "weapon_forging", 0, BLADE_III, STRONG_BOW_III),
    MAGIC_I(10, "magic", 1), MAGIC_II(20, "magic", 2), MAGIC_III(25, "magic", 3),
    FULLY_ARMED(15, "fully_armed", 0, MAGIC_III, WEAPON_FORGING, FORGED_ARMOR),
    MARCH_I(10, "march", 1), MARCH_II(20, "march", 2), MARCH_III(35, "march", 3),
    GUARD_I(10, "guard", 1), GUARD_II(20, "guard", 2), GUARD_III(35, "guard", 3),
    SUPPORT_I(15, "support", 1), SUPPORT_II(20, "support", 2),
    TRAINED(35, "trained", 0, MARCH_III, GUARD_III, SUPPORT_II),
    INVISIBLE(30), UNBREAKABLE(25), LAST_STAND(20), BLINK(20), QUICK_STRIKE(25), REFLECTION_FIELD(30),
    HEART_FAILURE_I(10, "heart_failure", 1), HEART_FAILURE_II(20, "heart_failure", 2), HEART_FAILURE_III(25, "heart_failure", 3),
    BROKEN_ARMOR_I(10, "broken_armor", 1), BROKEN_ARMOR_II(20, "broken_armor", 2), BROKEN_ARMOR_III(25, "broken_armor", 3),
    BLUNT_WEAPONS_I(10, "blunt_weapons", 1), BLUNT_WEAPONS_II(20, "blunt_weapons", 2), BLUNT_WEAPONS_III(25, "blunt_weapons", 3),
    SYSTEM_FAILURE_I(10, "system_failure", 1), SYSTEM_FAILURE_II(20, "system_failure", 2), SYSTEM_FAILURE_III(25, "system_failure", 3),
    FINAL_LINE(35, "final_line", 0, HEART_FAILURE_III, BROKEN_ARMOR_III, BLUNT_WEAPONS_III, SYSTEM_FAILURE_III),
    MAGNETIC_DISORDER(15), FROZEN_WORLD(25), EXHAUSTED(20), STAMINA_FAILURE(20),
    RACE_TIME_I(10, "race_time", 1), RACE_TIME_II(20, "race_time", 2), RACE_TIME_III(25, "race_time", 3),
    DARKNESS(30), DISEASED(20),
    HIDDEN_EXTREME(80, "hidden_extreme", 0,
            FULLY_ARMED, TRAINED, INVISIBLE, UNBREAKABLE, LAST_STAND, BLINK, QUICK_STRIKE, REFLECTION_FIELD,
            FINAL_LINE, MAGNETIC_DISORDER, FROZEN_WORLD, EXHAUSTED, STAMINA_FAILURE, RACE_TIME_III, DARKNESS, DISEASED);

    private static final Map<String, HvkAsaraArenaModifier> BY_ID;

    static {
        Map<String, HvkAsaraArenaModifier> values = new LinkedHashMap<>();
        for (HvkAsaraArenaModifier modifier : values()) {
            values.put(modifier.id(), modifier);
        }
        BY_ID = Collections.unmodifiableMap(values);
    }

    private final int score;
    private final String group;
    private final int tier;
    private final Set<HvkAsaraArenaModifier> prerequisites;

    HvkAsaraArenaModifier(int score, HvkAsaraArenaModifier... prerequisites) {
        this(score, null, 0, prerequisites);
    }

    HvkAsaraArenaModifier(int score, String group, int tier, HvkAsaraArenaModifier... prerequisites) {
        this.score = score;
        this.group = group;
        this.tier = tier;
        this.prerequisites = prerequisites.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(
                        new java.util.LinkedHashSet<>(java.util.Arrays.asList(prerequisites)));
    }

    public String id() {
        return name().toLowerCase();
    }

    public int score() {
        return score;
    }

    public String group() {
        return group == null ? id() : group;
    }

    public int tier() {
        return tier;
    }

    public Set<HvkAsaraArenaModifier> prerequisites() {
        return prerequisites;
    }

    public String translationKey() {
        return "arena.dealt_force_skills.modifier." + id();
    }

    public static HvkAsaraArenaModifier byId(String id) {
        return BY_ID.get(id);
    }

    public static long sanitize(long requestedMask) {
        long mask = requestedMask & allMask();
        boolean changed;
        do {
            changed = false;
            EnumSet<HvkAsaraArenaModifier> selected = fromMask(mask);
            for (HvkAsaraArenaModifier modifier : selected) {
                if (!selected.containsAll(modifier.prerequisites)) {
                    mask &= ~(1L << modifier.ordinal());
                    changed = true;
                }
            }
        } while (changed);
        return mask;
    }

    public static long toggle(long mask, HvkAsaraArenaModifier modifier) {
        long bit = 1L << modifier.ordinal();
        if ((mask & bit) != 0L) {
            return sanitize(mask & ~bit);
        }
        if (modifier.tier > 0) {
            for (HvkAsaraArenaModifier other : values()) {
                if (other != modifier && other.tier > 0 && other.group().equals(modifier.group())) {
                    mask &= ~(1L << other.ordinal());
                }
            }
        }
        return sanitize(mask | bit);
    }

    public static EnumSet<HvkAsaraArenaModifier> fromMask(long mask) {
        EnumSet<HvkAsaraArenaModifier> result = EnumSet.noneOf(HvkAsaraArenaModifier.class);
        for (HvkAsaraArenaModifier modifier : values()) {
            if ((mask & (1L << modifier.ordinal())) != 0L) {
                result.add(modifier);
            }
        }
        return result;
    }

    public static int score(long mask) {
        return fromMask(sanitize(mask)).stream().mapToInt(HvkAsaraArenaModifier::score).sum();
    }

    public static int tier(Set<HvkAsaraArenaModifier> selected, String group) {
        return selected.stream().filter(value -> value.group().equals(group)).mapToInt(HvkAsaraArenaModifier::tier).max().orElse(0);
    }

    private static long allMask() {
        return values().length == 64 ? -1L : (1L << values().length) - 1L;
    }
}
