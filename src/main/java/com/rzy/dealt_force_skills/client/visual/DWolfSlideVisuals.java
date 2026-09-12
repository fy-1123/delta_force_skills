package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.client.DWolfInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;

public final class DWolfSlideVisuals {
    private static LocalPlayer posedPlayer;

    private DWolfSlideVisuals() { }

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !DWolfInputHandler.isSlideActiveForVisual()) {
            reset();
            return;
        }
        if (posedPlayer != player) reset();
        posedPlayer = player;
        player.setForcedPose(Pose.SWIMMING);
    }

    public static void reset() {
        if (posedPlayer != null && posedPlayer.getForcedPose() == Pose.SWIMMING) {
            posedPlayer.setForcedPose(null);
        }
        posedPlayer = null;
    }
}
