package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * DFGAME 自定义背包界面（暂时为空）
 * DFGAME 开启且非创造模式时，按 E 打开背包将替换为原版背包界面
 */
public class DFInventoryScreen extends Screen {

    private static final int BG_COLOR = 0xFF0E1819;
    private static final float REFERENCE_WIDTH = 960f;
    private static final float REFERENCE_HEIGHT = 540f;
    private float uiScale = 1.0f;
    private int offsetX = 0;
    private int offsetY = 0;

    public DFInventoryScreen() {
        super(Component.translatable("screen.deltaforce.df_inventory"));
    }

    @Override
    protected void init() {
        super.init();
        this.uiScale = Math.min(this.width / REFERENCE_WIDTH, this.height / REFERENCE_HEIGHT);
        this.offsetX = (int)((this.width - REFERENCE_WIDTH * this.uiScale) / 2);
        this.offsetY = (int)((this.height - REFERENCE_HEIGHT * this.uiScale) / 2);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int w = (int)REFERENCE_WIDTH;
        int h = (int)REFERENCE_HEIGHT;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.offsetX, this.offsetY, 0);
        guiGraphics.pose().scale(this.uiScale, this.uiScale, 1.0f);

        guiGraphics.fill(0, 0, w, h, BG_COLOR);

        // 居中显示标题
        Component title = this.getTitle();
        int textWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (w - textWidth) / 2, h / 2 - 20, 0xFFFFFFFF, false);

        // 占位提示
        Component placeholder = Component.translatable("screen.deltaforce.df_inventory.placeholder");
        int pw = this.font.width(placeholder);
        guiGraphics.drawString(this.font, placeholder, (w - pw) / 2, h / 2 + 4, 0xFFAAAAAA, false);

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}