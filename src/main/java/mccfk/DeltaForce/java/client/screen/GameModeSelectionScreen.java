package mccfk.DeltaForce.java.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.DFClientConfig;

/**
 * GameModeSelectionScreen - 玩法选择界面
 * 顶部有7个标签栏（开始游戏标签选中态），底栏含 ESC文字 + 返回按钮
 * 点击非开始游戏标签跳转到 DFGUI_MAIN 主界面
 */
public class GameModeSelectionScreen extends Screen {

    private static final int BG_COLOR = 0xFF0E1819;
    private static final int BAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_COLOR = 0x30333333;

    private static final ResourceLocation BTN_BUTTON = new ResourceLocation("deltaforce", "textures/gui/button.png");

    // === alpha版游戏开关按钮（DFGAME 状态切换）===
    private static final ResourceLocation BTN_START_ALPHA = new ResourceLocation("deltaforce", "textures/temp/mod-gbutton-hover.png");
    private static final ResourceLocation BTN_CLOSE_ALPHA = new ResourceLocation("deltaforce", "textures/temp/mod-gbutton-update-hover.png");
    private static final int ALPHA_TEX_W = 994;
    private static final int ALPHA_TEX_H = 218;
    private static final float ALPHA_BTN_SCALE = 0.5F;

    // === 右侧底栏按钮（与 StartGameScreen 一致）===
    private static final ResourceLocation BTN_LEADERBOARD = new ResourceLocation("deltaforce", "textures/gui/start_game/leaderboard.png");
    private static final ResourceLocation BTN_RECRUIT     = new ResourceLocation("deltaforce", "textures/gui/start_game/recruit.png");
    private static final ResourceLocation BTN_SOCIAL      = new ResourceLocation("deltaforce", "textures/gui/start_game/social.png");
    private static final ResourceLocation BTN_PLAYER_SLOTS = new ResourceLocation("deltaforce", "textures/gui/start_game/player_slots.png");

    private static final ResourceLocation[] NEW_BTN_TEXTURES = {
        BTN_LEADERBOARD, BTN_RECRUIT, BTN_SOCIAL, BTN_PLAYER_SLOTS, BTN_PLAYER_SLOTS, BTN_PLAYER_SLOTS
    };
    private static final int NEW_BTN_COUNT = 6;

    private static final int ICON_TEX_SIZE = 64;
    private static final int ICON_DRAW_SIZE = 20;
    private static final float ICON_TEX_SCALE = 0.45F;

    private static final int MARGIN_RIGHT = 30;
    private static final int GAP_DEPLOY = 3;
    private static final int GAP_SOCIAL_DEPLOY = 7;
    private static final int GAP_RECRUIT_SOCIAL = 6;
    private static final int GAP_LEADERBOARD_RECRUIT = 6;
    private static final int SOCIAL_DRAW_W = 30;
    private static final float DEPLOY_TEX_SCALE = ICON_TEX_SCALE * 0.5F;

    private static final int TAB_COUNT = 7;
    private static final float UI_SCALE = 0.75F;
    private static final int TAB_BASE_WIDTH = 91;
    private static final int TAB_BASE_HEIGHT = 32;
    private static final ResourceLocation TABS_OFF = new ResourceLocation("deltaforce", "textures/gui/main_tabs_off.png");
    private static final ResourceLocation TABS_ON = new ResourceLocation("deltaforce", "textures/gui/main_tabs_on.png");
    private static final String[] TAB_LANG_KEYS = {
        "tab.deltaforce.start_game",
        "tab.deltaforce.warehouse",
        "tab.deltaforce.operator",
        "tab.deltaforce.department",
        "tab.deltaforce.exchange",
        "tab.deltaforce.logistics",
        "tab.deltaforce.gunsmith"
    };

    private int tabWidth;
    private int tabHeight;
    private int tabStartX;
    private int tabStartY;

    private static final int BUTTON_TEX_W = 128;
    private static final int BUTTON_TEX_H = 48;
    private static final int BUTTON_DRAW_W = 80;
    private static final int BUTTON_DRAW_H = 20;
    private static final float BUTTON_TEX_SCALE = 0.5F;

    private static final int RETURN_START_X = 28;
    private static final int ESC_TEXT_X_OFFSET = -15;
    private static final int ESC_TEXT_Y_OFFSET = 2;
    private static final float ESC_TEXT_SCALE = 0.75F;

    private int escX, escY, escW, escH;
    private int returnX, returnY, returnW, returnH;

    private int hoveredNewBtn = -1;

    private int alphaBtnX, alphaBtnY, alphaBtnW, alphaBtnH;

    public GameModeSelectionScreen() {
        super(Component.translatable("screen.deltaforce.game_mode_selection"));
    }

    @Override
    protected void init() {
        super.init();
        DFClientConfig cfg = DFClientConfig.getInstance();
        this.tabWidth = (int) (TAB_BASE_WIDTH * UI_SCALE);
        this.tabHeight = (int) (TAB_BASE_HEIGHT * UI_SCALE);
        this.tabStartX = 10 + cfg.getTabXOffset();
        this.tabStartY = 10 + cfg.getTabYOffset();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < TAB_COUNT; i++) {
            ResourceLocation tex = (i == 0) ? TABS_ON : TABS_OFF;
            int x = tabStartX + i * tabWidth;
            guiGraphics.blit(tex, x, tabStartY, 0, 0, tabWidth, tabHeight, tabWidth, tabHeight);

            Component tabTitle = Component.translatable(TAB_LANG_KEYS[i]);
            int textWidth = this.font.width(tabTitle);
            int textX = x + (tabWidth - textWidth) / 2;
            int textY = tabStartY + (tabHeight - this.font.lineHeight) / 2;
            int textColor = (i == 0) ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, tabTitle, textX, textY, textColor, false);
        }

        int barY = this.height - BAR_HEIGHT;
        guiGraphics.fill(0, barY, this.width, this.height, BOTTOM_BAR_COLOR);

        returnX = RETURN_START_X;
        returnY = barY;
        returnW = BUTTON_DRAW_W;
        returnH = BUTTON_DRAW_H;

        int texSrcW = (int) (BUTTON_TEX_W * BUTTON_TEX_SCALE);
        int texSrcH = (int) (BUTTON_TEX_H * BUTTON_TEX_SCALE);
        int texX = returnX + (returnW - texSrcW) / 2;
        int texY = returnY + (returnH - texSrcH) / 2;

        guiGraphics.blit(BTN_BUTTON, texX, texY, 0, 0, texSrcW, texSrcH, texSrcW, texSrcH);

        Component returnText = Component.translatable("screen.deltaforce.return");
        int returnTextWidth = this.font.width(returnText);
        float returnTextScale = 1.4F;
        int scaledReturnTextWidth = (int) (returnTextWidth * returnTextScale);
        int returnTextX = returnX + returnW - scaledReturnTextWidth - 15;
        int returnTextY = returnY + (returnH - (int) (this.font.lineHeight * returnTextScale)) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(returnTextX, returnTextY, 0);
        guiGraphics.pose().scale(returnTextScale, returnTextScale, 1.0F);
        guiGraphics.drawString(this.font, returnText, 0, 0, 0xFFFFFFFF, false);
        guiGraphics.pose().popPose();

        escX = returnX + ESC_TEXT_X_OFFSET;
        escY = returnY + ESC_TEXT_Y_OFFSET;
        escW = BUTTON_DRAW_W;
        escH = BUTTON_DRAW_H;

        Component escText = Component.literal("ESC");
        int escTextWidth = this.font.width(escText);
        float scaledEscTextWidth = escTextWidth * ESC_TEXT_SCALE;
        int escTextX = escX + (int)((escW - scaledEscTextWidth) / 2);
        int escTextY = escY + (int)((escH - this.font.lineHeight * ESC_TEXT_SCALE) / 2);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(escTextX, escTextY, 0);
        guiGraphics.pose().scale(ESC_TEXT_SCALE, ESC_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, escText, 0, 0, 0xFF000000, false);
        guiGraphics.pose().popPose();

        int rightEdge = this.width - MARGIN_RIGHT;
        int[] nx = new int[NEW_BTN_COUNT];
        int[] nw = new int[NEW_BTN_COUNT];

        nx[5] = rightEdge - ICON_DRAW_SIZE;
        nw[5] = ICON_DRAW_SIZE;
        nx[4] = nx[5] - GAP_DEPLOY - ICON_DRAW_SIZE;
        nw[4] = ICON_DRAW_SIZE;
        nx[3] = nx[4] - GAP_DEPLOY - ICON_DRAW_SIZE;
        nw[3] = ICON_DRAW_SIZE;
        nx[2] = nx[3] - GAP_SOCIAL_DEPLOY - SOCIAL_DRAW_W;
        nw[2] = SOCIAL_DRAW_W;
        nx[1] = nx[2] - GAP_RECRUIT_SOCIAL - ICON_DRAW_SIZE;
        nw[1] = ICON_DRAW_SIZE;
        nx[0] = nx[1] - GAP_LEADERBOARD_RECRUIT - ICON_DRAW_SIZE;
        nw[0] = ICON_DRAW_SIZE;

        hoveredNewBtn = -1;
        for (int i = 0; i < NEW_BTN_COUNT; i++) {
            int btnX = nx[i];
            int drawW = nw[i];
            int drawH = ICON_DRAW_SIZE;
            float texScale = (i >= 3) ? DEPLOY_TEX_SCALE : ICON_TEX_SCALE;
            int texSrc = (int)(ICON_TEX_SIZE * texScale);
            int btnY = barY;

            int iconTexX = btnX + (drawW - texSrc) / 2;
            int iconTexY = btnY + (drawH - texSrc) / 2;

            guiGraphics.blit(NEW_BTN_TEXTURES[i], iconTexX, iconTexY, 0, 0, texSrc, texSrc, texSrc, texSrc);

            if (mouseX >= btnX && mouseX < btnX + drawW && mouseY >= btnY && mouseY < btnY + drawH) {
                hoveredNewBtn = i;
            }

            if (i == 2) {
                int friendCount = StartGameScreen.getOnlineFriendsCount();
                Component friendsText = Component.literal(String.valueOf(friendCount));
                int textWidth = this.font.width(friendsText);
                float textScale = 0.55F;
                int scaledTextWidth = (int)(textWidth * textScale);
                int textX = btnX + drawW - scaledTextWidth - 2;
                int textY = btnY + (drawH - (int)(this.font.lineHeight * textScale)) / 2;

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(textX, textY, 0);
                guiGraphics.pose().scale(textScale, textScale, 1.0F);
                guiGraphics.drawString(this.font, friendsText, 0, 0, 0xFFFFFFFF, false);
                guiGraphics.pose().popPose();
            }
        }

        if (hoveredNewBtn >= 0) {
            guiGraphics.fill(nx[hoveredNewBtn], barY, nx[hoveredNewBtn] + nw[hoveredNewBtn], barY + ICON_DRAW_SIZE, 0x80FFFFFF);
        }

        boolean dfGameOn = DeltaForce.isDFGameEnabled();
        ResourceLocation alphaTex = dfGameOn ? BTN_CLOSE_ALPHA : BTN_START_ALPHA;
        alphaBtnW = (int) (ALPHA_TEX_W * ALPHA_BTN_SCALE);
        alphaBtnH = (int) (ALPHA_TEX_H * ALPHA_BTN_SCALE);
        alphaBtnX = (this.width - alphaBtnW) / 2;
        alphaBtnY = (this.height - alphaBtnH) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(alphaBtnX, alphaBtnY, 0);
        guiGraphics.pose().scale(ALPHA_BTN_SCALE, ALPHA_BTN_SCALE, 1.0F);
        guiGraphics.blit(alphaTex, 0, 0, 0, 0, ALPHA_TEX_W, ALPHA_TEX_H, ALPHA_TEX_W, ALPHA_TEX_H);
        guiGraphics.pose().popPose();

        Component alphaText = Component.translatable(
            dfGameOn ? "screen.deltaforce.alpha.close" : "screen.deltaforce.alpha.start");
        float alphaTextScale = 1.8F;
        int alphaTextScaledW = (int) (this.font.width(alphaText) * alphaTextScale);
        int alphaTextScaledH = (int) (this.font.lineHeight * alphaTextScale);
        int alphaTextX = alphaBtnX + (alphaBtnW - alphaTextScaledW) / 2;
        int alphaTextY = alphaBtnY + (alphaBtnH - alphaTextScaledH) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(alphaTextX, alphaTextY, 0);
        guiGraphics.pose().scale(alphaTextScale, alphaTextScale, 1.0F);
        guiGraphics.drawString(this.font, alphaText, 0, 0, 0xFFFFFFFF, false);
        guiGraphics.pose().popPose();

        Component alphaNote = Component.translatable("screen.deltaforce.alpha.note");
        float alphaNoteScale = 0.75F;
        int alphaNoteScaledW = (int) (this.font.width(alphaNote) * alphaNoteScale);
        int alphaNoteX = alphaBtnX + (alphaBtnW - alphaNoteScaledW) / 2;
        int alphaNoteY = alphaBtnY + alphaBtnH + 10;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(alphaNoteX, alphaNoteY, 0);
        guiGraphics.pose().scale(alphaNoteScale, alphaNoteScale, 1.0F);
        guiGraphics.drawString(this.font, alphaNote, 0, 0, 0xFFAAAAAA, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (hoveredNewBtn >= 0 && hoveredNewBtn < NEW_BTN_COUNT) {
                if (hoveredNewBtn == 2) {
                    StartGameScreen.handleSocialClick((int)mouseX, (int)mouseY, this.width, this.height, button);
                    return true;
                }
                return true;
            }
        }

        if (button == 0) {
            if (mouseX >= alphaBtnX && mouseX < alphaBtnX + alphaBtnW &&
                mouseY >= alphaBtnY && mouseY < alphaBtnY + alphaBtnH) {
                DeltaForce.setDFGame(!DeltaForce.isDFGameEnabled());
                Minecraft.getInstance().setScreen(null);
                return true;
            }

            for (int i = 0; i < TAB_COUNT; i++) {
                int x = tabStartX + i * tabWidth;
                int y = tabStartY;
                if (mouseX >= x && mouseX < x + tabWidth &&
                    mouseY >= y && mouseY < y + tabHeight) {
                    if (i != 0) {
                        Minecraft.getInstance().setScreen(new DFGUI_MAIN());
                    }
                    return true;
                }
            }

            if (mouseX >= escX && mouseX < escX + escW &&
                mouseY >= escY && mouseY < escY + escH) {
                openMainScreen();
                return true;
            }

            if (mouseX >= returnX && mouseX < returnX + returnW &&
                mouseY >= returnY && mouseY < returnY + returnH) {
                openMainScreen();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            openMainScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void openMainScreen() {
        Minecraft.getInstance().setScreen(new DFGUI_MAIN());
    }
}
