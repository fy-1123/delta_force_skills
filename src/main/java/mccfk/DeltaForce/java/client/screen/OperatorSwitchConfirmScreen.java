package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.network.C2S_SelectCharacter;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 干员切换确认弹窗（生存模式消耗浮木）
 * 全屏遮罩变暗并禁用窗口外点击；淡灰色窗口 + 细边框。
 * 取消按钮贴图 mod-gbutton-update-hover.png，确认按钮贴图 mod-gbutton-normal.png。
 * 按钮固定小尺寸；确认在左、取消在右；按钮上绘制"确认"/"取消"文字。
 * 无浮木时显示"需要有至少一个[浮木]才可以切换！"提示并绘制浮木物品图标（悬浮可见物品信息），仅一个确认按钮。
 */
public class OperatorSwitchConfirmScreen extends Screen {

    private static final ResourceLocation BTN_CONFIRM = new ResourceLocation(
            "deltaforce", "textures/temp/mod-gbutton-normal.png");
    private static final ResourceLocation BTN_CANCEL = new ResourceLocation(
            "deltaforce", "textures/temp/mod-gbutton-update-hover.png");

    // 按钮显示尺寸（基准尺寸的 0.4 倍），窗口随按钮/文字长度自动缩放
    private static final int BTN_W = 144;
    private static final int BTN_H = 32;
    private static final int BTN_GAP = 12;
    private static final int WIN_PADDING = 16;
    private static final int TITLE_H = 32;
    private static final int ICON_SIZE = 16;   // 浮木物品图标尺寸（无浮木弹窗）
    private static final int ICON_AREA = 24;   // 图标区域高度（图标 + 上下留白）

    private final String characterId;
    private final boolean missingDriftwood;
    private final ItemStack driftwoodStack;    // 无浮木弹窗中展示的浮木物品图标

    /**
     * 确认切换弹窗（背包有浮木）
     */
    public OperatorSwitchConfirmScreen(String characterId) {
        super(Component.translatable("message.dealt_force_skills.driftwood_confirm"));
        this.characterId = characterId;
        this.missingDriftwood = false;
        this.driftwoodStack = ItemStack.EMPTY;
    }

    /**
     * 无浮木提示弹窗（仅一个确认按钮）
     */
    public OperatorSwitchConfirmScreen(boolean missingDriftwood) {
        super(Component.translatable("message.dealt_force_skills.driftwood_missing_title"));
        this.characterId = null;
        this.missingDriftwood = true;
        this.driftwoodStack = new ItemStack(ModItems.DRIFTWOOD.get());
    }

    /**
     * 打开确认切换弹窗（由干员栏确认时调用）
     */
    public static void open(String characterId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DFGUI_MAIN main) {
            main.showOperatorConfirm(characterId);
        } else {
            minecraft.setScreen(new OperatorSwitchConfirmScreen(characterId));
        }
    }

    /**
     * 打开无浮木提示弹窗
     */
    public static void openMissing() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DFGUI_MAIN main) {
            main.showOperatorMissingConfirm();
        } else {
            minecraft.setScreen(new OperatorSwitchConfirmScreen(true));
        }
    }

    /**
     * 内容基准宽度：取按钮区总宽、标题文字宽、按钮文字最大宽中的最大值，
     * 使窗口在不同按钮数量和文字长度下自动缩放，文字不会被截断。
     */
    private int contentWidth() {
        int buttonsW = missingDriftwood ? BTN_W : BTN_W * 2 + BTN_GAP;
        int titleW = font.width(title);
        int confirmW = font.width(Component.translatable("screen.dealt_force_skills.operator_confirm.confirm"));
        int labelW = missingDriftwood ? confirmW : Math.max(confirmW,
                font.width(Component.translatable("screen.dealt_force_skills.operator_confirm.cancel")));
        return Math.max(Math.max(buttonsW, titleW), labelW);
    }

    private int windowWidth() {
        return contentWidth() + WIN_PADDING * 2;
    }

    private int windowHeight() {
        return TITLE_H + (missingDriftwood ? ICON_AREA : 0) + BTN_H + WIN_PADDING;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 全屏遮罩：变暗
        graphics.fill(0, 0, width, height, 0x88000000);

        int winW = windowWidth();
        int winH = windowHeight();
        int winX = (width - winW) / 2;
        int winY = (height - winH) / 2;

        // 淡灰色窗口
        graphics.fill(winX, winY, winX + winW, winY + winH, 0xE0C8C8C8);

        // 细边框（四边）
        graphics.fill(winX, winY, winX + winW, winY + 1, 0xFF666666);
        graphics.fill(winX, winY + winH - 1, winX + winW, winY + winH, 0xFF666666);
        graphics.fill(winX, winY, winX + 1, winY + winH, 0xFF666666);
        graphics.fill(winX + winW - 1, winY, winX + winW, winY + winH, 0xFF666666);

        // 标题
        graphics.drawCenteredString(font, title, width / 2, winY + 12, 0xFF202020);

        // 无浮木提示：绘制浮木物品图标，鼠标悬浮时显示物品 tooltip
        if (missingDriftwood) {
            int iconX = winX + (winW - ICON_SIZE) / 2;
            int iconY = winY + TITLE_H + 4;
            graphics.renderItem(driftwoodStack, iconX, iconY);
            graphics.renderItemDecorations(font, driftwoodStack, iconX, iconY);
            if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                    && mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
                graphics.renderTooltip(font, driftwoodStack, mouseX, mouseY);
            }
        }

        int btnY = winY + TITLE_H + (missingDriftwood ? ICON_AREA : 0);
        int buttonsW = missingDriftwood ? BTN_W : BTN_W * 2 + BTN_GAP;
        int firstX = winX + (winW - buttonsW) / 2;
        if (missingDriftwood) {
            // 无浮木提示：单个确认按钮居中
            graphics.blit(BTN_CONFIRM, firstX, btnY, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
            drawButtonLabel(graphics, firstX, btnY,
                    Component.translatable("screen.dealt_force_skills.operator_confirm.confirm"));
        } else {
            // 确认切换：确认在左，取消在右
            int confirmX = firstX;
            int cancelX = confirmX + BTN_W + BTN_GAP;
            graphics.blit(BTN_CONFIRM, confirmX, btnY, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
            graphics.blit(BTN_CANCEL, cancelX, btnY, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
            drawButtonLabel(graphics, confirmX, btnY,
                    Component.translatable("screen.dealt_force_skills.operator_confirm.confirm"));
            drawButtonLabel(graphics, cancelX, btnY,
                    Component.translatable("screen.dealt_force_skills.operator_confirm.cancel"));
        }
    }

    /**
     * 在按钮中央绘制文字（白色带阴影）
     */
    private void drawButtonLabel(GuiGraphics graphics, int x, int y, Component label) {
        int textX = x + (BTN_W - font.width(label)) / 2;
        int textY = y + (BTN_H - font.lineHeight) / 2;
        graphics.drawString(font, label, textX, textY, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int winW = windowWidth();
            int winH = windowHeight();
            int winX = (width - winW) / 2;
            int winY = (height - winH) / 2;
            int btnY = winY + TITLE_H + (missingDriftwood ? ICON_AREA : 0);

            int buttonsW = missingDriftwood ? BTN_W : BTN_W * 2 + BTN_GAP;
            int firstX = winX + (winW - buttonsW) / 2;

            if (missingDriftwood) {
                // 无浮木提示：单个确认按钮居中
                if (mouseX >= firstX && mouseX < firstX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H) {
                    close();
                    return true;
                }
            } else {
                // 确认切换：确认在左，取消在右
                int confirmX = firstX;
                int cancelX = confirmX + BTN_W + BTN_GAP;
                // 确认按钮（左）
                if (mouseX >= confirmX && mouseX < confirmX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H) {
                    confirm();
                    return true;
                }
                // 取消按钮（右）
                if (mouseX >= cancelX && mouseX < cancelX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H) {
                    close();
                    return true;
                }
            }
        }
        // 窗口其他位置：禁用点击（吞掉事件）
        return true;
    }

    private void confirm() {
        NetworkHandler.sendToServer(new C2S_SelectCharacter(characterId));
        Operator.clearPending();
        closeModal();
    }

    private void close() {
        Operator.clearPending();
        closeModal();
    }

    private void closeModal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DFGUI_MAIN main) {
            main.closeOperatorConfirm();
        } else {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}

