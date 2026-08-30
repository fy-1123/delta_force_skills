package mccfk.DeltaForce.java.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import mccfk.DeltaForce.java.compat.SubLevelProjectionHelper;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.network.Marker;

/**
 * 标点渲染器 - 在 RenderLevelStageEvent 中调用
 * 使用穿透方块的发光线条渲染三种标点类型：实体（红色）、容器（黄色）、位置（绿色）
 */
public class MarkerRenderer {

    /** 实体标点颜色：红色 ARGB */
    private static final int ENTITY_LINE_COLOR = 0xFFFF0000;
    /** 容器标点颜色：黄色 ARGB */
    private static final int CONTAINER_LINE_COLOR = 0xFFFFFF00;
    /** 位置标点颜色：绿色 ARGB */
    private static final int POSITION_LINE_COLOR = 0xFF00FF00;
    /** 底部圆圈半径 */
    private static final float CIRCLE_RADIUS = 0.25F;
    /** 标点竖线高度（格） */
    private static final double MARKER_HEIGHT = 3.5;

    /**
     * 渲染所有标点（在 RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS 阶段调用）
     */
    public static void renderAllMarkers(PoseStack poseStack, Camera camera) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(MarkerRenderTypes.GLOWING_LINES);
        for (Marker marker : ClientMarkerStorage.getAllMarkers()) {
            renderMarker(poseStack, camera, marker, lines);
        }
        bufferSource.endBatch(MarkerRenderTypes.GLOWING_LINES);
    }

    /**
     * 根据标点类型分发到对应的渲染方法
     */
    private static void renderMarker(PoseStack poseStack, Camera camera, Marker marker, VertexConsumer lines) {
        BlockPos pos = marker.position();
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        switch (marker.type()) {
            case ENTITY -> renderEntityMarker(poseStack, pos, lines);
            case CONTAINER -> renderContainerMarker(poseStack, marker, lines);
            case POSITION -> renderPositionMarker(poseStack, pos, marker.precisePosition(), lines);
        }

        poseStack.popPose();
    }

    /**
     * 渲染实体标点：红色竖线 + 底部红色圆圈
     */
    private static void renderEntityMarker(PoseStack poseStack, BlockPos pos, VertexConsumer lines) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        drawVerticalLine(poseStack, lines, x, y, z, MARKER_HEIGHT, ENTITY_LINE_COLOR);
        drawCircle(poseStack, lines, x, y + 0.01, z, CIRCLE_RADIUS, ENTITY_LINE_COLOR);
    }

    /**
     * 渲染容器标点：黄色AABB线框
     * 子世界容器（containerPos 与 position 不同）时，用 plotgrid 原始坐标精确计算世界空间AABB
     */
    private static void renderContainerMarker(PoseStack poseStack, Marker marker, VertexConsumer lines) {
        BlockPos pos = marker.position();

        // 检查是否为子世界容器：containerPos存在且不等于position，说明是plotgrid原始坐标
        if (marker.containerPos().isPresent() && !marker.containerPos().get().equals(pos)) {
            // 子世界容器：用plotgrid原始坐标精确计算世界空间AABB（处理旋转/分数偏移）
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockPos plotgridPos = marker.containerPos().get();
                AABB aabb = SubLevelProjectionHelper.projectBlockAABBToGlobal(mc.level, plotgridPos);
                drawAABBOutline(poseStack, lines, aabb, CONTAINER_LINE_COLOR);
                return;
            }
        }

        // 普通容器：直接使用BlockPos绘制AABB
        AABB aabb = new AABB(pos);
        drawAABBOutline(poseStack, lines, aabb, CONTAINER_LINE_COLOR);
    }

    /**
     * 渲染位置标点：绿色竖线 + 底部绿色圆圈
     * 优先使用精确命中点坐标，否则使用方块网格中心
     */
    private static void renderPositionMarker(PoseStack poseStack, BlockPos pos, java.util.Optional<Vec3> precisePos,
                                             VertexConsumer lines) {
        double x, y, z;
        if (precisePos.isPresent()) {
            Vec3 p = precisePos.get();
            x = p.x;
            y = p.y;
            z = p.z;
        } else {
            x = pos.getX() + 0.5;
            y = pos.getY();
            z = pos.getZ() + 0.5;
        }

        drawVerticalLine(poseStack, lines, x, y, z, MARKER_HEIGHT, POSITION_LINE_COLOR);
        drawCircle(poseStack, lines, x, y + 0.01, z, CIRCLE_RADIUS, POSITION_LINE_COLOR);
    }

    /**
     * 绘制竖线（从底部到指定高度）
     */
    private static void drawVerticalLine(PoseStack poseStack, VertexConsumer consumer, double x, double y, double z,
                                         double height, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = 1.0F;

        consumer.vertex(matrix, (float) x, (float) y, (float) z).color(r, g, b, a).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, (float) x, (float) (y + height), (float) z).color(r, g, b, a).normal(0, 1, 0).endVertex();
    }

    /**
     * 绘制水平圆圈（32段近似）
     */
    private static void drawCircle(PoseStack poseStack, VertexConsumer consumer, double centerX, double centerY,
                                   double centerZ, double radius, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            addLine(consumer, matrix,
                    centerX + radius * Math.cos(angle1), centerY, centerZ + radius * Math.sin(angle1),
                    centerX + radius * Math.cos(angle2), centerY, centerZ + radius * Math.sin(angle2), r, g, b);
        }
    }

    /**
     * 绘制AABB线框（12条边）
     */
    private static void drawAABBOutline(PoseStack poseStack, VertexConsumer consumer, AABB aabb, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.minX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.minX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.minZ, aabb.minX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);
    }

    private static void addLine(VertexConsumer consumer, Matrix4f matrix, double x1, double y1, double z1,
                                double x2, double y2, double z2, float r, float g, float b) {
        consumer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
    }
}