package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 地面物品信息HUD预览Screen — 真实地面物品信息面板渲染
 * 渲染原点固定在(0,0)，完全由外层LayoutEntry的relX/relY/scaleX/scaleY控制位置
 */
public class GroundItemHudScreen extends Screen {

    public GroundItemHudScreen() {
        super(Component.literal("GroundItemHUD"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x00000000);
        if (this.width <= 0 || this.height <= 0) return;

        var font = Minecraft.getInstance().font;
        if (font == null) return;

        int boxW = Math.min(this.width - 8, 120);
        int boxH = Math.min(this.height - 8, 60);
        int boxX = Math.max(0, (this.width - boxW) / 2);
        int boxY = Math.max(0, (this.height - boxH) / 2);

        // 面板背景
        g.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC1E1E1E);

        // 边框（上亮下暗模拟立体效果）
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF5A5A5A);
        g.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0xFF3A3A3A);
        g.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFF5A5A5A);
        g.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, 0xFF3A3A3A);

        // 物品图标区域
        int iconSize = Math.min(30, boxH - 12);
        int iconX = boxX + 4;
        int iconY = boxY + (boxH - iconSize) / 2;
        g.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xAA2A2A2A);
        g.renderOutline(iconX, iconY, iconSize, iconSize, 0xFF555555);

        // 渲染一个假物品图标
        ItemStack demoStack = new ItemStack(Items.DIAMOND);
        g.pose().pushPose();
        g.pose().translate(iconX + iconSize / 2f, iconY + iconSize / 2f, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.renderFakeItem(demoStack, -8, -8);
        g.pose().popPose();

        // 物品名称
        int textX = iconX + iconSize + 6;
        int textY = boxY + 6;
        String itemName = "示例物品";
        g.drawString(font, itemName, textX, textY, 0xFFFF55FF, false);

        // 品质标签
        String quality = "[史诗]";
        int qualityW = font.width(quality);
        int remainingW = boxX + boxW - 6 - textX;
        g.drawString(font, quality, textX + remainingW - qualityW, textY, 0xFFAA00AA, false);

        // 描述行
        int descY = textY + font.lineHeight + 2;
        String desc = "描述文本...";
        if (descY + font.lineHeight <= boxY + boxH - 2) {
            g.drawString(font, desc, textX, descY, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}