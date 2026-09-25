package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * DFS技能HUD预览Screen — 使用与GenericSkillHudOverlay一致的SLOT=46/GAP=4参数
 * 渲染原点固定在(0,0)，完全由外层LayoutEntry的relX/relY/scaleX/scaleY控制位置
 */
public class DfsSkillHudScreen extends Screen {

    private static final int SLOT = 46;
    private static final int GAP = 4;
    private static final int[] ACCENT_COLORS = {
            0xFF67D6FF, 0xFFFFC857, 0xFF8EE36A
    };
    private static final String[] LABELS = {"核心", "技能2", "技能1"};

    public DfsSkillHudScreen() {
        super(Component.literal("DfsSkillHUD"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x00000000);
        if (this.width <= 0 || this.height <= 0) return;

        // 绘制3个技能槽位，使用与GenericSkillHudOverlay相同的参数
        int totalW = SLOT * 3 + GAP * 2;
        int startX = Math.max(0, (this.width - totalW) / 2);
        int startY = Math.max(0, (this.height - SLOT) / 2);

        for (int i = 0; i < 3; i++) {
            int sx = startX + i * (SLOT + GAP);
            int sy = startY;
            drawDfsSlot(g, sx, sy, SLOT, ACCENT_COLORS[i], LABELS[i]);
        }
    }

    /** 以与GenericSkillHudOverlay.drawSlot一致的DFS样式绘制单个技能槽位 */
    private void drawDfsSlot(GuiGraphics g, int x, int y, int size, int accent, String label) {
        // 深色背景
        g.fill(x, y, x + size, y + size, 0xAA101216);
        // 彩色强调边框
        g.fill(x, y, x + size, y + 1, accent);
        g.fill(x, y + size - 1, x + size, y + size, accent);
        g.fill(x, y, x + 1, y + size, accent);
        g.fill(x + size - 1, y, x + size, y + size, accent);
        // 内嵌暗色面板
        g.fill(x + 6, y + 5, x + 40, y + 31, 0xDD171A20);
        // 槽位编号
        if (this.font != null) {
            int lw = this.font.width(label);
            int lh = this.font.lineHeight;
            g.drawString(this.font, label,
                    x + (size - lw) / 2, y + 12,
                    0xFFFFFFFF, false);
            g.drawString(this.font, "就绪",
                    x + (size - this.font.width("就绪")) / 2, y + 22,
                    0xFFE0E4EA, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}