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

    public DFInventoryScreen() {
        super(Component.translatable("screen.deltaforce.df_inventory"));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 居中显示标题
        Component title = this.getTitle();
        int textWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - textWidth) / 2, this.height / 2 - 20, 0xFFFFFFFF, false);

        // 占位提示
        Component placeholder = Component.translatable("screen.deltaforce.df_inventory.placeholder");
        int pw = this.font.width(placeholder);
        guiGraphics.drawString(this.font, placeholder, (this.width - pw) / 2, this.height / 2 + 4, 0xFFAAAAAA, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}