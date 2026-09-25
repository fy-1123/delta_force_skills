package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * DFGame HUD预览Screen — 与ClientDFGameHandler.renderHealthBar一致的渲染
 * 渲染原点固定在(0,0)，完全由外层LayoutEntry的relX/relY/scaleX/scaleY控制位置
 */
public class DFGameHudScreen extends Screen {

    public DFGameHudScreen() {
        super(Component.literal("DFGameHUD"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x00000000);
        if (this.width <= 0 || this.height <= 0) return;

        // 与ClientDFGameHandler.renderHealthBar一致的渲染逻辑
        float healthPercent = 1.0f;
        String healthText = "100.00/100";
        var font = Minecraft.getInstance().font;

        int barW = Math.max(10, this.width * 9 / 10);
        int barH = Math.max(4, this.height * 15 / 100);
        int barX = this.width / 20;
        int barY = this.height * 7 / 10;

        // 血量文本
        int textY = barY - Math.round(barH * 1.2f);
        if (textY < 0) textY = barY + barH + 2;
        g.drawString(font, healthText, barX, textY, 0xFFFFFFFF, false);

        // 血条背景
        g.fill(barX, barY, barX + barW, barY + barH, 0x80333333);

        // 血量填充
        int fillW = (int) (barW * healthPercent);
        g.fill(barX, barY, barX + fillW, barY + barH, 0xFFFFFFFF);

        // 边框
        g.renderOutline(barX, barY, barW, barH, 0xFFCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}