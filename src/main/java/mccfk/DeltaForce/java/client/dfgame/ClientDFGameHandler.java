package mccfk.DeltaForce.java.client.dfgame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.dfgame.DFGameManager;

/**
 * DFGAME 客户端处理器
 * 负责：血量条渲染（死亡流程走原版，由服务端伤害事件触发）
 */
public class ClientDFGameHandler {

    /**
     * 渲染 DFGAME 血量条（左下角，替换原版血量显示）
     * @param guiGraphics GUI 上下文
     * @param screenWidth 屏幕宽
     * @param screenHeight 屏幕高
     */
    public static void renderHealthBar(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String playerName = player.getName().getString();
        double health = DFGameManager.getHealth(playerName);
        float healthPercent = (float) (health / DFGameManager.MAX_HEALTH);

        // 左下角位置（按界面尺寸补偿：以界面尺寸=3为基准，锚定屏幕左下角）
        float hudScale = com.rzy.dealt_force_skills.client.ClientHudLayout.guiScaleFactor();
        int barX = Math.round(10 * hudScale);
        int barY = screenHeight - Math.round(30 * hudScale);
        int barW = Math.round(200 * hudScale);
        int barH = Math.round(10 * hudScale);

        var font = Minecraft.getInstance().font;

        // 绘制血量文本：血量/100
        String healthText = String.format("%.2f/100", health);
        guiGraphics.drawString(font, healthText, barX, barY - Math.round(12 * hudScale), 0xFFFFFFFF, false);

        // 绘制血条背景（深灰色）
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0x80333333);

        // 绘制血条（白色，按血量比例）
        int fillW = (int) (barW * healthPercent);
        guiGraphics.fill(barX, barY, barX + fillW, barY + barH, 0xFFFFFFFF);

        // 绘制血条边框
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFFCCCCCC);
    }
}