package mccfk.DeltaForce.java.client.dfgame;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.screen.LayoutDataManager;
import mccfk.DeltaForce.java.client.screen.LayoutEntry;
import mccfk.DeltaForce.java.dfgame.DFGameManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * DFGAME 客户端处理器
 * 血条位置/缩放读取布局配置，编辑器中改的实时生效
 */
public class ClientDFGameHandler {

    /**
     * 渲染 DFGAME 血量条（左下角，替换原版血量显示）
     * 位置/缩放从布局配置读取
     */
    public static void renderHealthBar(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        float healthPercent;
        String healthText;
        if (player == null) {
            healthPercent = 1.0f;
            healthText = "100.00/100";
        } else {
            String playerName = player.getName().getString();
            double health = DFGameManager.getHealth(playerName);
            healthPercent = (float) (health / DFGameManager.MAX_HEALTH);
            healthText = String.format("%.2f/100", health);
        }

        // 从布局配置读取血条位置/缩放/尺寸
        LayoutEntry le = LayoutDataManager.loadElement(
                "dfgame", "hud", "health_health_bar",
                0.02f, 0.92f, 0.22f, 0.04f);

        int barW = Math.max(10, (int) (le.relWidth * screenWidth * le.scaleX));
        int barH = Math.max(4, (int) (le.relHeight * screenHeight * le.scaleY));
        int barX = (int) (le.relX() * screenWidth);
        int barY = (int) (le.relY() * screenHeight);

        var font = Minecraft.getInstance().font;

        // 血量文本
        int textY = barY - Math.round(barH * 1.2f);
        if (textY < 0) textY = barY + barH + 2;
        guiGraphics.drawString(font, healthText, barX, textY, 0xFFFFFFFF, false);

        // 血条背景
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0x80333333);

        // 血量填充
        int fillW = (int) (barW * healthPercent);
        guiGraphics.fill(barX, barY, barX + fillW, barY + barH, 0xFFFFFFFF);

        // 边框
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFFCCCCCC);
    }
}