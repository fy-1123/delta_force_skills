package mccfk.DeltaForce.java.client.screen;

import com.google.gson.annotations.SerializedName;

/**
 * 单个 UI 元素的布局数据 - 存在 .MCCFK_ALL_MODDATA/deltaforce/LCS/ 下
 * 坐标/尺寸均为相对值（0.0~1.0），独立于分辨率
 */
public class LayoutEntry {

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    // 锚点（0.0~1.0，表示元素参考点位于父容器中的位置比例）
    @SerializedName("anchor_x")
    public float anchorX = 0.5f;
    @SerializedName("anchor_y")
    public float anchorY = 0.5f;

    // 相对于锚点的偏移（相对值，0.0~1.0）
    @SerializedName("offset_x")
    public float offsetX;
    @SerializedName("offset_y")
    public float offsetY;

    // 缩放（1.0 = 原始大小）
    @SerializedName("scale_x")
    public float scaleX = 1.0f;
    @SerializedName("scale_y")
    public float scaleY = 1.0f;

    // 是否启用（false 则不渲染）
    @SerializedName("enabled")
    public boolean enabled = true;

    // 元素的期望尺寸（相对值，0.0~1.0；0 表示自适应）
    @SerializedName("rel_width")
    public float relWidth;
    @SerializedName("rel_height")
    public float relHeight;

    public LayoutEntry() {}

    public LayoutEntry(String id, String displayName, float anchorX, float anchorY,
                       float offsetX, float offsetY, float relW, float relH) {
        this.id = id;
        this.displayName = displayName;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.relWidth = relW;
        this.relHeight = relH;
    }

    /** 计算在给定容器内的绝对 X 坐标 */
    public int toAbsX(int containerW, int containerH, int elemW) {
        return (int) ((offsetX + anchorX) * containerW - elemW * anchorX);
    }

    /** 计算在给定容器内的绝对 Y 坐标 */
    public int toAbsY(int containerW, int containerH, int elemH) {
        return (int) ((offsetY + anchorY) * containerH - elemH * anchorY);
    }

    /** 获取相对 X 位置（用于预览区内拖拽） */
    public float relX() {
        return anchorX + offsetX;
    }

    /** 获取相对 Y 位置（用于预览区内拖拽） */
    public float relY() {
        return anchorY + offsetY;
    }
}