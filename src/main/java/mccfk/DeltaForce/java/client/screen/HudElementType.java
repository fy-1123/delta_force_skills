package mccfk.DeltaForce.java.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 预定义的 HUD / 界面元素类型
 * 编辑时从该列表选择要添加到预览窗口的控件
 */
public enum HudElementType {

    // === 游戏内 HUD ===
    HOTBAR("hotbar", "gui.deltaforce.element.hotbar", 0xFFAA6600, 0.7f, 0.06f),
    HEALTH("health", "gui.deltaforce.element.health", 0xFFFF4444, 0.3f, 0.04f),
    ARMOR("armor", "gui.deltaforce.element.armor", 0xFF888888, 0.3f, 0.04f),
    HUNGER("hunger", "gui.deltaforce.element.hunger", 0xFFCC8844, 0.3f, 0.04f),
    CROSSHAIR("crosshair", "gui.deltaforce.element.crosshair", 0xFF00FF00, 0.04f, 0.08f),
    EXPERIENCE("experience", "gui.deltaforce.element.experience", 0xFF44FF44, 0.7f, 0.02f),
    CHAT("chat", "gui.deltaforce.element.chat", 0xFF333333, 0.5f, 0.2f),
    SCOREBOARD("scoreboard", "gui.deltaforce.element.scoreboard", 0xFF552200, 0.25f, 0.35f),
    BOSSBAR("bossbar", "gui.deltaforce.element.bossbar", 0xFF880088, 0.7f, 0.04f),
    ACTIONBAR("actionbar", "gui.deltaforce.element.actionbar", 0xFF222222, 0.5f, 0.03f),
    PLAYER_LIST("player_list", "gui.deltaforce.element.player_list", 0xFF444444, 0.35f, 0.6f),

    // === 容器/界面 ===
    CONTAINER_SLOTS("container_slots", "gui.deltaforce.element.container_slots", 0xFF665544, 0.5f, 0.4f),
    PLAYER_INVENTORY("player_inventory", "gui.deltaforce.element.player_inventory", 0xFF776655, 0.5f, 0.35f),
    TITLE_LABEL("title_label", "gui.deltaforce.element.title_label", 0xFF777777, 0.4f, 0.06f),

    // === 自定义 ===
    CUSTOM_BOX("custom_box", "gui.deltaforce.element.custom_box", 0xFF4488CC, 0.2f, 0.15f);

    public final String id;
    public final String translationKey;
    public final int color;
    public final float defaultRelW;
    public final float defaultRelH;

    HudElementType(String id, String translationKey, int color,
                   float defaultRelW, float defaultRelH) {
        this.id = id;
        this.translationKey = translationKey;
        this.color = color;
        this.defaultRelW = defaultRelW;
        this.defaultRelH = defaultRelH;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public static HudElementType fromId(String id) {
        for (HudElementType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return CUSTOM_BOX;
    }

    /**
     * 创建一个使用此类型默认配置的 LayoutEntry
     */
    public LayoutEntry createEntry() {
        LayoutEntry e = new LayoutEntry();
        e.id = this.id;
        e.displayName = Component.translatable(translationKey).getString();
        e.anchorX = 0.5f;
        e.anchorY = 0.5f;
        e.relWidth = this.defaultRelW;
        e.relHeight = this.defaultRelH;
        return e;
    }

    /**
     * 创建带指定ID和位置尺寸的默认 LayoutEntry
     * @param fullId 完整元素ID（含后缀）
     * @param anchorX 锚点X
     * @param anchorY 锚点Y
     * @param relW 相对宽度
     * @param relH 相对高度
     */
    public LayoutEntry createDefault(String fullId, float anchorX,
            float anchorY, float relW, float relH) {
        LayoutEntry e = new LayoutEntry();
        e.id = fullId;
        e.displayName = Component.translatable(translationKey).getString();
        e.anchorX = anchorX;
        e.anchorY = anchorY;
        e.relWidth = relW;
        e.relHeight = relH;
        e.enabled = true;
        e.scaleX = 1.0f;
        e.scaleY = 1.0f;
        return e;
    }
}