package mccfk.DeltaForce.java.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * 标点渲染类型 - 继承RenderType以访问protected的RenderStateShard常量
 */
public final class MarkerRenderTypes extends RenderType {

    /** 发光线条：无深度测试、无纹理、无剔除、5像素粗线，可穿透方块显示 */
    public static final RenderType GLOWING_LINES = RenderType.create(
            "glowing_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            TRANSIENT_BUFFER_SIZE,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setTextureState(NO_TEXTURE)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLineState(new LineStateShard(OptionalDouble.of(5.0)))
                    .createCompositeState(false)
    );

    private MarkerRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload,
                              Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}