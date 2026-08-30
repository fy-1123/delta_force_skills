package mccfk.DeltaForce.java.client.dfgame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.screen.DFInventoryScreen;

/**
 * DFGAME GUI 事件处理器
 * DFGAME 开启且非创造模式时：
 * 1. 渲染自定义血量条（左下角，替换原版血量显示）
 * 2. "禁用全部界面"开启时拦截原版 GUI：聊天栏/暂停菜单/死亡界面/本模组界面放行；
 *    带 #deltaforce:dfgame 标签的方块界面放行；"使用自定义背包"开启时背包替换为自定义背包界面；其余强制关闭
 *    "禁用全部界面"关闭时不拦截任何界面
 */
public class DFGameGuiEventHandler {

    /**
     * 方块标签：拥有此标签的方块，其界面在 DFGAME 模式下允许打开
     */
    public static final TagKey<Block> DFGAME_BLOCK_TAG = TagKey.create(
        Registries.BLOCK,
        new ResourceLocation(DeltaForce.MODID, "dfgame")
    );

    /**
     * 客户端最近右键的方块状态（用于判断容器界面对应的方块是否带 dfgame 标签）
     * 右键方块触发事件在界面打开之前，时序上可准确对应
     */
    private static BlockState lastRightClickedState = null;

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        // 只在热键栏渲染后执行一次，避免每个 overlay 都触发
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            return;
        }

        // 创造模式例外：不渲染自定义血量条（使用原版）
        if (player.getAbilities().instabuild) {
            return;
        }

        // 如果当前在菜单界面，不渲染
        if (mc.screen != null) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        var guiGraphics = event.getGuiGraphics();

        // === 第一步：渲染自定义血量条 ===
        ClientDFGameHandler.renderHealthBar(guiGraphics, screenWidth, screenHeight);
    }

    /**
     * 屏幕打开前拦截：
     * DFGAME 开启且非创造模式、且"禁用全部界面"开启时：
     * - 本模组界面/聊天栏/暂停菜单/死亡界面：放行
     * - 背包："使用自定义背包"开启时替换为自定义背包界面，否则禁用
     * - 容器界面：带 #deltaforce:dfgame 标签的放行，否则禁用
     * - 其他所有原版 GUI：强制关闭
     * "禁用全部界面"关闭时不拦截任何界面
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            return;
        }

        // 创造模式例外：不拦截任何界面
        if (player.getAbilities().instabuild) {
            return;
        }

        Screen screen = event.getNewScreen();
        if (screen == null) {
            return;
        }

        // "禁用全部界面"开关未开启：不拦截任何界面
        // （"使用自定义背包"开启时，仍将背包替换为自定义背包界面）
        if (!DeltaForce.isDFLockAllScreensEnabled()) {
            if (screen instanceof InventoryScreen && DeltaForce.isDFCustomInventoryEnabled()) {
                event.setNewScreen(new DFInventoryScreen());
            }
            return;
        }

        // 本模组自身界面默认白名单（放行）
        if (isDeltaForceScreen(screen)) {
            return;
        }

        // 聊天栏例外
        if (screen instanceof ChatScreen) {
            return;
        }

        // 暂停菜单（ESC）例外
        if (screen instanceof PauseScreen) {
            return;
        }

        // 游戏模式切换（F3+F4）例外
        if (screen instanceof GameModeSwitcherScreen) {
            return;
        }

        // 死亡界面：原版死亡流程必须放行（死亡消息、重生、物品掉落）
        if (screen instanceof DeathScreen) {
            return;
        }

        // 背包：使用自定义背包开启时替换为自定义背包界面，否则禁用
        if (screen instanceof InventoryScreen) {
            if (DeltaForce.isDFCustomInventoryEnabled()) {
                event.setNewScreen(new DFInventoryScreen());
            } else {
                event.setCanceled(true); // 禁用全部界面 → 背包也禁用
            }
            return;
        }

        // 容器界面：带 #deltaforce:dfgame 标签的放行，否则禁用
        if (screen instanceof ContainerScreen) {
            BlockState state = lastRightClickedState;
            if (state != null && state.is(DFGAME_BLOCK_TAG)) {
                lastRightClickedState = null; // 消费记录
                return; // 放行
            }
            lastRightClickedState = null;
            // 无标签 → 关闭服务端菜单并强制关闭界面
            player.closeContainer();
            event.setCanceled(true);
            return;
        }

        // 其他所有原版 GUI → 强制关闭
        event.setCanceled(true);
    }

    /**
     * 右键方块时记录方块状态（仅客户端）：
     * 用于在容器界面打开时判断对应方块是否带 #deltaforce:dfgame 标签。
     * 注意：不取消事件，因此不影响放置方块等正常交互。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        if (!event.getSide().isClient()) {
            return;
        }

        // 仅在"禁用全部界面"开启时才需要记录右键方块状态
        if (!DeltaForce.isDFLockAllScreensEnabled()) {
            return;
        }

        var player = event.getEntity();
        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        BlockPos pos = event.getHitVec().getBlockPos();
        lastRightClickedState = event.getLevel().getBlockState(pos);
    }

    /**
     * 判断界面是否为本模组的界面（包名前缀 mccfk.DeltaForce.java.client.screen）
     * 本模组全部界面默认白名单，DFGAME 模式下始终放行
     */
    private static boolean isDeltaForceScreen(Screen screen) {
        return screen.getClass().getName().startsWith("mccfk.DeltaForce.java.client.screen.");
    }
}