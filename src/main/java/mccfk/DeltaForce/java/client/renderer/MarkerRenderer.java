package mccfk.DeltaForce.java.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.network.Marker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MarkerRenderer {
    private static final double HEIGHT = 3.5D;
    private static final double RADIUS = 0.25D;

    private MarkerRenderer() {
    }

    public static void renderAllMarkers(PoseStack poseStack, Camera camera) {
        if (ClientMarkerStorage.getAllMarkers().isEmpty()) return;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Vec3 cameraPos = camera.getPosition();
        RenderSystem.disableDepthTest();
        try {
            for (Marker marker : ClientMarkerStorage.getAllMarkers()) renderMarker(poseStack, lines, cameraPos, marker);
            buffers.endBatch(RenderType.lines());
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderMarker(PoseStack poseStack, VertexConsumer lines, Vec3 cameraPos, Marker marker) {
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        BlockPos position = marker.position();
        switch (marker.type()) {
            case ENTITY -> renderPin(poseStack, lines, Vec3.atBottomCenterOf(position), 255, 64, 64);
            case POSITION -> renderPin(poseStack, lines,
                    marker.precisePosition().orElse(Vec3.atBottomCenterOf(position)), 64, 255, 96);
            case CONTAINER -> LevelRenderer.renderLineBox(poseStack, lines, new AABB(position),
                    1.0F, 0.9F, 0.15F, 1.0F);
        }
        poseStack.popPose();
    }

    private static void renderPin(PoseStack poseStack, VertexConsumer lines, Vec3 base, int red, int green, int blue) {
        addLine(lines, poseStack.last(), base, base.add(0.0D, HEIGHT, 0.0D), red, green, blue);
        for (int segment = 0; segment < 32; segment++) {
            double first = Math.PI * 2.0D * segment / 32.0D;
            double second = Math.PI * 2.0D * (segment + 1) / 32.0D;
            Vec3 from = base.add(Math.cos(first) * RADIUS, 0.01D, Math.sin(first) * RADIUS);
            Vec3 to = base.add(Math.cos(second) * RADIUS, 0.01D, Math.sin(second) * RADIUS);
            addLine(lines, poseStack.last(), from, to, red, green, blue);
        }
    }

    private static void addLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to,
                                int red, int green, int blue) {
        Vec3 normal = to.subtract(from);
        normal = normal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        consumer.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
                .color(red, green, blue, 255)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        consumer.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(red, green, blue, 255)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
