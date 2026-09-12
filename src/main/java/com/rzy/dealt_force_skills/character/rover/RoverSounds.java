package com.rzy.dealt_force_skills.character.rover;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class RoverSounds {
    public static final SoundEvent SMOKE_EQUIP = SoundEvents.ARMOR_EQUIP_IRON;
    public static final SoundEvent SMOKE_STOW = SoundEvents.ARMOR_EQUIP_LEATHER;
    public static final SoundEvent SMOKE_THROW = SoundEvents.SNOWBALL_THROW;
    public static final SoundEvent SMOKE_BOUNCE = SoundEvents.STONE_HIT;
    public static final SoundEvent SMOKE_BURST = SoundEvents.FIRE_EXTINGUISH;
    public static final SoundEvent INJECTOR_EQUIP = SoundEvents.CROSSBOW_LOADING_START;
    public static final SoundEvent INJECTOR_STOW = SoundEvents.CROSSBOW_LOADING_END;
    public static final SoundEvent INJECTOR_MODE = SoundEvents.LEVER_CLICK;
    public static final SoundEvent INJECTOR_FIRE = SoundEvents.CROSSBOW_SHOOT;
    public static final SoundEvent INJECTOR_BURST = SoundEvents.SPLASH_POTION_BREAK;
    public static final SoundEvent DOG_SUMMON = SoundEvents.WOLF_AMBIENT;
    public static final SoundEvent DOG_COMMAND = SoundEvents.NOTE_BLOCK_BELL.get();
    public static final SoundEvent DOG_RECALL = SoundEvents.WOLF_WHINE;
    public static final SoundEvent DOG_ATTACK = SoundEvents.WOLF_GROWL;
    public static final SoundEvent DOG_RETREAT = SoundEvents.WOLF_WHINE;

    private RoverSounds() {}
}
