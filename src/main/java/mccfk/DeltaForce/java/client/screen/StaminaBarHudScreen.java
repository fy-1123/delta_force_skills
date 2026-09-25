package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.client.StaminaBarRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 体力条HUD预览Screen — 使用真实StaminaBarRenderer渲染
 * 渲染原点固定在(0,0)，完全由外层LayoutEntry的relX/relY/scaleX/scaleY控制位置
 */
public class StaminaBarHudScreen extends Screen {

    public StaminaBarHudScreen() {
        super(Component.literal("StaminaBarHUD"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x00000000);
        if (this.width <= 0 || this.height <= 0) return;

        // 使用真实StaminaBarRenderer渲染体力条，预览值70/100
        g.pose().pushPose();
        // 将体力条绘制在Screen中央
        int renderW = StaminaBarRenderer.WIDTH;
        int renderH = StaminaBarRenderer.HEIGHT;
        int x = Math.max(0, (this.width - renderW) / 2);
        int y = Math.max(0, (this.height - renderH) / 2);
        g.pose().translate(x, y, 0);
        StaminaBarRenderer.render(g, 70.0, false);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}