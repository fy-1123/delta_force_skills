package mccfk.DeltaForce.java.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.rzy.dealt_force_skills.client.screen.GtiTradingHallScreen;
import com.rzy.dealt_force_skills.client.screen.SpecialOperationsBaseScreen;
import com.rzy.dealt_force_skills.network.C2S_GtiSpecialOperationsAction;
import com.rzy.dealt_force_skills.network.C2S_SpecialOperationsBaseAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SpecialOperationsBaseSnapshot;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import mccfk.DeltaForce.java.network.C2S_DfWarehouseAction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * DFGUI_MAIN 主界面 - 纯色 #0e1819 全屏背景
 * 底部绘制7个标签页按钮，点击切换选中态
 * 通过按 Y 键打开/关闭
 */
public class DFGUI_MAIN extends Screen {

    private static final int BG_COLOR = 0xFF0E1819;

    // 标签页常量（基于原版UI缩放比例）
    private static final int TAB_COUNT = 7;
    private static final float UI_SCALE = 0.75F; // 相对于原版的缩放
    private static final int TAB_BASE_WIDTH = 91;
    private static final int TAB_BASE_HEIGHT = 32;
    
    private int tabWidth;
    private int tabHeight;
    private int tabStartX;
    private int tabStartY;

    // 共用贴图：每张贴图包含7个横向排列标签（总尺寸637×32）
    private static final ResourceLocation TABS_OFF = new ResourceLocation("deltaforce", "textures/gui/main_tabs_off.png");
    private static final ResourceLocation TABS_ON = new ResourceLocation("deltaforce", "textures/gui/main_tabs_on.png");

    // 仓库标签页索引（自定义仓库页渲染）
    private static final int WAREHOUSE_TAB_INDEX = 1;
    // 特战干员标签页索引（干员选择栏仅在该界面显示）
    private static final int OPERATOR_TAB_INDEX = 2;
    // 交易行标签页索引（点击打开 GTI 交易行界面）
    private static final int EXCHANGE_TAB_INDEX = 4;
    // 特勤处标签页索引（点击打开 GTI 特勤处界面）
    private static final int LOGISTICS_TAB_INDEX = 5;

    // 标签页本地化键名数组（对应7个按钮）
    private static final String[] TAB_LANG_KEYS = {
        "tab.deltaforce.start_game",
        "tab.deltaforce.warehouse",
        "tab.deltaforce.operator",
        "tab.deltaforce.department",
        "tab.deltaforce.exchange",
        "tab.deltaforce.logistics",
        "tab.deltaforce.gunsmith"
    };

    // 客户端内存持久化：上次选中的标签页索引
    private static int lastSelectedTab = 0;

    private int selectedTab; // 当前选中的标签页

    // 内嵌子面板：GTI交易行/特勤处界面（在主机面内显示，避免关闭主机面导致的问题）
    private Screen subScreen;

    // 仓库页数据（仓库标签页自定义渲染用，来自服务端独立仓库系统）
    private int warehouseTotalSlots = 120;
    private int warehousePage = 0;
    private String warehouseContainer = "主仓库";
    private List<ItemStack> warehouseItems = List.of();

    public DFGUI_MAIN() {
        super(Component.literal("DFGUI_MAIN"));
        this.selectedTab = lastSelectedTab; // 恢复上次打开的页（首次打开为"开始游戏"页）
    }

    /**
     * 打开三角洲主界面并直接选中"特战干员"标签页（供外部按键/入口调用）
     */
    public static void openToOperator() {
        lastSelectedTab = OPERATOR_TAB_INDEX;
        StartGameScreen.closeOverlayPanels();
        Minecraft.getInstance().setScreen(new DFGUI_MAIN());
    }

    /** Opens the redesigned special-operations panel inside the Delta Force shell. */
    public static void openToLogistics() {
        lastSelectedTab = LOGISTICS_TAB_INDEX;
        StartGameScreen.closeOverlayPanels();
        SpecialOperationsBaseScreen.clearPendingGtiAccess();
        Minecraft.getInstance().setScreen(new DFGUI_MAIN());
        NetworkHandler.sendToServer(C2S_SpecialOperationsBaseAction.open());
    }

    /**
     * 在主界面内显示 GTI 交易行面板（服务端回发数据后调用，避免关闭主机面）
     */
    public void showGtiMarket(long coins, java.util.List<GtiTradingHallManager.Entry> entries) {
        if (selectedTab != EXCHANGE_TAB_INDEX) {
            return;
        }
        selectedTab = EXCHANGE_TAB_INDEX;
        lastSelectedTab = EXCHANGE_TAB_INDEX;
        StartGameScreen.closeOverlayPanels();
        if (subScreen instanceof GtiTradingHallScreen screen) {
            screen.receive(coins, entries);
            return;
        }
        subScreen = GtiTradingHallScreen.createEmbedded(coins, entries);
        subScreen.init(Minecraft.getInstance(), this.width, this.height);
    }

    /**
     * 在主界面内显示 GTI 特勤处面板（服务端回发快照后调用，避免关闭主机面）
     */
    public void showGtiBase(S2C_SpecialOperationsBaseSnapshot snapshot) {
        if (selectedTab != LOGISTICS_TAB_INDEX) {
            return;
        }
        selectedTab = LOGISTICS_TAB_INDEX;
        lastSelectedTab = LOGISTICS_TAB_INDEX;
        StartGameScreen.closeOverlayPanels();
        if (subScreen instanceof SpecialOperationsBaseScreen screen) {
            screen.acceptSnapshot(snapshot);
            return;
        }
        subScreen = SpecialOperationsBaseScreen.createEmbedded(snapshot);
        subScreen.init(Minecraft.getInstance(), this.width, this.height);
    }

    /**
     * 接收仓库页快照（仓库标签页自定义渲染，不打开特勤处面板）
     */
    public void showWarehouse(int page, String container, int pageSlots, List<ItemStack> items) {
        this.warehousePage = page;
        this.warehouseContainer = container;
        this.warehouseTotalSlots = pageSlots;
        this.warehouseItems = items;
        this.subScreen = null;
    }

    /**
     * 当前是否处于仓库标签页（供快照处理器判断渲染方式）
     */
    public boolean isWarehouseTab() {
        return selectedTab == WAREHOUSE_TAB_INDEX;
    }

    /**
     * 在主界面内显示干员切换确认弹窗（生存模式消耗浮木，不关闭主机面）
     */
    public void showOperatorConfirm(String characterId) {
        if (subScreen instanceof OperatorSwitchConfirmScreen) {
            return;
        }
        subScreen = new OperatorSwitchConfirmScreen(characterId);
        subScreen.init(Minecraft.getInstance(), this.width, this.height);
    }

    public void showOperatorMissingConfirm() {
        if (!(subScreen instanceof OperatorSwitchConfirmScreen)) {
            subScreen = new OperatorSwitchConfirmScreen(true);
            subScreen.init(Minecraft.getInstance(), this.width, this.height);
        }
    }

    /**
     * 关闭干员切换确认弹窗
     */
    public void closeOperatorConfirm() {
        if (subScreen instanceof OperatorSwitchConfirmScreen) {
            subScreen = null;
        }
    }

    @Override
    protected void init() {
        super.init();
        // 根据屏幕大小计算缩放后的尺寸和位置
        int preferredTabWidth = (int) (TAB_BASE_WIDTH * UI_SCALE);
        int sideReserve = this.width < 620 ? Math.min(120, Math.max(56, this.width / 5)) : 120;
        int availableTabWidth = Math.max(TAB_COUNT, this.width - sideReserve * 2);
        this.tabWidth = Math.max(1, Math.min(preferredTabWidth, availableTabWidth / TAB_COUNT));
        this.tabHeight = Math.max(18, Math.min((int) (TAB_BASE_HEIGHT * UI_SCALE), this.height / 8));
        this.tabStartX = Math.max(4, (this.width - this.tabWidth * TAB_COUNT) / 2);
        this.tabStartY = 6;
        StartGameScreen.requestSnapshot();
        // 恢复仓库标签页时重新请求仓库快照（否则重开界面仓库内容显示为空）
        if (selectedTab == WAREHOUSE_TAB_INDEX) {
            NetworkHandler.sendToServer(new C2S_DfWarehouseAction(C2S_DfWarehouseAction.Action.OPEN, 0, 0, 0));
        }
        if (subScreen != null) {
            subScreen.resize(Minecraft.getInstance(), this.width, this.height);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 绘制底部灰色半透明背景条
        int bottomBarColor = 0x30333333;
        guiGraphics.fill(0, this.height - 20, this.width, this.height, bottomBarColor);

        boolean hasEmbeddedPage = subScreen != null;
        if (!hasEmbeddedPage) {
            // 如果选中"开始游戏"标签（索引0），调用子界面绘制玩家模型和模式面板
            if (selectedTab == 0) {
                StartGameScreen.renderPlayerModel(guiGraphics, this.width / 2, this.height / 2 - 40);
                StartGameScreen.renderModePanels(guiGraphics, mouseX, mouseY, this.width, this.height);
            }

            // 干员选择栏：仅特战干员界面，位于底栏上方
            if (selectedTab == OPERATOR_TAB_INDEX) {
                // 角色预览大图：预选（待确认）状态即显示，无预选时显示当前已选角色
                Operator.renderSelectedPreview(guiGraphics, this.width, this.height);
                Operator.render(guiGraphics, mouseX, mouseY, this.width, this.height);
            }

            // 仓库页：仅仓库标签页
            if (selectedTab == WAREHOUSE_TAB_INDEX) {
                WarehousePanel.render(guiGraphics, mouseX, mouseY, this.width, this.height,
                        warehouseItems, warehouseTotalSlots, warehousePage, warehouseContainer);
            }

        }

        // 底栏按钮：左侧仅开始游戏显示，右侧（排行榜/招募/社交/玩家头像）常显
        StartGameScreen.renderBottomBarButtons(guiGraphics, mouseX, mouseY, this.width, this.height, selectedTab);

        // 右上角信息区：settings/mail 常显，货币显示仅在特战干员界面隐藏，网络验证仅在开始游戏显示
        if (!hasEmbeddedPage) {
            StartGameScreen.renderTopRightInfo(guiGraphics, mouseX, mouseY, this.width, this.height, selectedTab);
        }

        // 绘制标签页
        for (int i = 0; i < TAB_COUNT; i++) {
            ResourceLocation tex = (i == selectedTab) ? TABS_ON : TABS_OFF;
            int x = tabStartX + i * tabWidth;
            guiGraphics.blit(tex, x, tabStartY, 0, 0, tabWidth, tabHeight, tabWidth, tabHeight);

            // 绘制居中标题文字
            Component tabTitle = Component.translatable(TAB_LANG_KEYS[i]);
            int textWidth = this.font.width(tabTitle);
            int textX = x + (tabWidth - textWidth) / 2;
            int textY = tabStartY + (tabHeight - this.font.lineHeight) / 2;
            int textColor = (i == selectedTab) ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, tabTitle, textX, textY, textColor, false);
        }

        // 绘制内嵌子面板（GTI交易行/特勤处）置于最上层
        if (subScreen != null) {
            subScreen.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (!hasEmbeddedPage) {
            StartGameScreen.renderOverlayOnTop(guiGraphics, mouseX, mouseY, this.width, this.height);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (StartGameScreen.hasOpenOverlay()) {
            StartGameScreen.handleOverlayClick((int) mouseX, (int) mouseY, button, this.width, this.height);
            return true;
        }
        if (button == 0 && handleTabClick(mouseX, mouseY)) {
            return true;
        }
        // 内嵌子面板优先处理点击（未命中时回落主界面逻辑，允许点击标签切换/关闭面板）
        if (subScreen != null && subScreen.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (StartGameScreen.handleBottomButtonClick((int) mouseX, (int) mouseY, button,
                this.width, this.height, selectedTab)) {
            return true;
        }

        if (button == 0) {
            // 开始游戏标签下的模式面板点击：打开玩法选择界面
            if (selectedTab == 0) {
                if (StartGameScreen.isPreBattleRepClicked((int)mouseX, (int)mouseY, this.width, this.height)) {
                    return true;
                }
                if (StartGameScreen.isNoTargetClicked((int)mouseX, (int)mouseY, this.width, this.height)) {
                    return true;
                }
            }

            // 右上角 settings/mail 空触发（常显，与底栏按钮一致，无实际功能）
            if (StartGameScreen.isTopRightIconClicked((int)mouseX, (int)mouseY, this.width, this.height)) {
                return true;
            }

            // 干员选择栏：按住拖拽滑动（仅特战干员界面）
            if (selectedTab == OPERATOR_TAB_INDEX && Operator.handleMouseClicked((int)mouseX, (int)mouseY, this.width, this.height)) {
                return true;
            }

            // 仓库页：长按拖拽/短按闪白（仅仓库标签页）
            if (selectedTab == WAREHOUSE_TAB_INDEX && WarehousePanel.handleMousePressed((int)mouseX, (int)mouseY, this.width, this.height, button)) {
                return true;
            }

        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        for (int i = 0; i < TAB_COUNT; i++) {
            int x = tabStartX + i * tabWidth;
            if (mouseX < x || mouseX >= x + tabWidth
                    || mouseY < tabStartY || mouseY >= tabStartY + tabHeight) {
                continue;
            }
            if (selectedTab == WAREHOUSE_TAB_INDEX && i != WAREHOUSE_TAB_INDEX) {
                WarehousePanel.resetInteractionState();
            }
            selectedTab = i;
            lastSelectedTab = i;
            subScreen = null;
            StartGameScreen.closeOverlayPanels();
            if (i == WAREHOUSE_TAB_INDEX) {
                NetworkHandler.sendToServer(new C2S_DfWarehouseAction(C2S_DfWarehouseAction.Action.OPEN, 0, 0, 0));
            } else if (i == EXCHANGE_TAB_INDEX) {
                NetworkHandler.sendToServer(new C2S_GtiSpecialOperationsAction(
                        C2S_GtiSpecialOperationsAction.Action.OPEN_MARKET, 0, 0));
            } else if (i == LOGISTICS_TAB_INDEX) {
                NetworkHandler.sendToServer(C2S_SpecialOperationsBaseAction.open());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (StartGameScreen.handlePanelScroll(delta)) {
            return true;
        }
        // 内嵌子面板优先处理滚轮（交易行/特勤处列表滚动）
        if (subScreen != null && subScreen.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        // 仓库页：滚轮滚动仓库/物品栏
        if (selectedTab == WAREHOUSE_TAB_INDEX) {
            WarehousePanel.handleMouseScrolled(mouseX, mouseY, delta, this.width, this.height);
            return true;
        }
        // 特战干员界面：滚轮滑动干员选择栏
        if (selectedTab == OPERATOR_TAB_INDEX) {
            Operator.handleMouseScrolled(delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 内嵌子面板优先处理拖拽（仓库滚动条等）
        if (subScreen != null && subScreen.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        // 仓库页：拖动滑动条/长按拖动物品
        if (selectedTab == WAREHOUSE_TAB_INDEX && button == 0) {
            WarehousePanel.handleMouseDragged((int) mouseX, (int) mouseY);
            return true;
        }
        // 特战干员界面：按住左键拖拽干员选择栏
        if (selectedTab == OPERATOR_TAB_INDEX && button == 0) {
            Operator.handleMouseDragged((int) mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        WarehousePanel.handleMouseReleased((int) mouseX, (int) mouseY, button);
        Operator.handleMouseReleased();
        // 转发鼠标松开给内嵌子面板（仓库滚动条拖拽结束等）
        if (subScreen != null) {
            subScreen.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        WarehousePanel.resetInteractionState();
        Operator.clearPending();
        StartGameScreen.resetSessionState();
        subScreen = null;
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_Y) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        // 转发按键给内嵌子面板（输入框、Tab切换焦点等）
        if (subScreen != null && subScreen.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 转发字符输入给内嵌子面板（交易行/特勤处搜索与数量输入框）
        if (subScreen != null && subScreen.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        // 刷新内嵌子面板（输入框光标等）
        if (subScreen != null) {
            subScreen.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
