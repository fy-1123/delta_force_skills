package com.rzy.dealt_force_skills.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class TrainingTargetCompat {
    private static final ResourceLocation DUMMMMMMY_TARGET =
            new ResourceLocation("dummmmmmy", "target_dummy");

    private TrainingTargetCompat() {
    }

    public static boolean recordsDamageWithoutDying(LivingEntity entity) {
        return entity != null && DUMMMMMMY_TARGET.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }
}