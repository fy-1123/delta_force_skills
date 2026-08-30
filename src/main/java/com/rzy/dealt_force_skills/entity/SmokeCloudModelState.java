package com.rzy.dealt_force_skills.entity;

public interface SmokeCloudModelState {
    int smokeLifetimeTicks();

    default boolean shouldRenderSmokeModel() {
        return true;
    }
}
