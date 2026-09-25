package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class WorldRenderMatrixCompat {
    private static boolean pushed;

    private WorldRenderMatrixCompat() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pushWorldMatrix(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || pushed) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Matrix4f modelView = poseStack.last().pose();
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().mulPoseMatrix(modelView);
        RenderSystem.applyModelViewMatrix();
        pushed = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void popWorldMatrix(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !pushed) {
            return;
        }
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        pushed = false;
    }
}