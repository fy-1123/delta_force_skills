package mccfk.DeltaForce.java.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.compat.SubLevelProjectionHelper;
import mccfk.DeltaForce.java.client.overlay.GroundItemOverlay;
import mccfk.DeltaForce.java.client.overlay.ItemDescriptionLoader;
import mccfk.DeltaForce.java.client.renderer.GroundItemRenderer;
import mccfk.DeltaForce.java.client.renderer.MarkerRenderer;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import mccfk.DeltaForce.java.client.screen.Operator;
import mccfk.DeltaForce.java.client.screen.WarehousePanel;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.network.C2S_AdminAuth;
import mccfk.DeltaForce.java.network.C2S_CreateMarker;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.network.Marker;
import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.MarkerType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import java.util.Optional;
import java.util.UUID;

/**
 * DeltaForce 客户端入口
 * Forge 1.20.1 适配版
 */
@Mod.EventBusSubscriber(modid = DeltaForce.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DeltaForceClient {

    static {
        // 强制启用AWT/Swing：Minecraft JVM默认headless=true会阻断Swing弹窗
        System.setProperty("java.awt.headless", "false");
    }

    // 鼠标中键标点系统
    public static final KeyMapping MARKER_KEY = new KeyMapping(
            "key.deltaforce.marker",
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE,
            "key.categories.deltaforce"
    );

    private DeltaForceClient() {
    }

    // ===== MOD 总线事件 =====

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DeltaForceEntities.GROUND_ITEM.get(), GroundItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ground_item_overlay", GroundItemOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MARKER_KEY);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            ItemDescriptionLoader.clearCache();
            DfsItemQuality.clearTagQualityOverrides();
        });
    }

    // ===== FORGE 总线事件（在内部类中注册，避免与主类注解冲突） =====

    @Mod.EventBusSubscriber(modid = DeltaForce.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {

        // 首次tick是否已执行管理员弹窗（阻塞式弹窗，true后不再重入）
        private static boolean authDialogShown = false;
        // 上次tick的玩家名称，用于检测In-Game Account Switcher切换账户
        private static String lastKnownPlayerName = null;
        // U键手动跟踪（非KeyMapping注册，仅通过GLFW检测）
        private static boolean uKeyWasDown = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();

            String currentName = mc.getUser().getName();

            // 检测In-Game Account Switcher账户切换：玩家名变化时重置验证并重新弹窗
            if (lastKnownPlayerName != null && !lastKnownPlayerName.equals(currentName)) {
                AdminAuthManager.resetVerification();
                authDialogShown = false;
            }
            lastKnownPlayerName = currentName;

            // 首次tick或账户切换后弹出管理员验证
            if (!authDialogShown) {
                authDialogShown = true;
                // 从磁盘加载已验证的哈希，避免每次启动都弹窗
                AdminAuthManager.verifyFromDisk();
                if (DFClientConfig.getInstance().isAdminAuthEnabled()
                        && "MC_CaoFangKuai".equals(currentName)
                        && !AdminAuthManager.isVerified(currentName)) {
                    showAdminAuthDialog(mc);
                }
            }

            // 清理客户端过期标点
            ClientMarkerStorage.cleanExpired();

            // 处理鼠标中键创建标点
            while (MARKER_KEY.consumeClick()) {
                createMarkerFromHitResult();
            }

            // 处理U键打开三角洲主界面（df main t开启监听，df main f关闭监听）
            // U键通过GLFW裸输入检测，与核心模组KeyMapping系统独立运行，不互斥
            boolean dfMainNow = DeltaForce.isDFMainEnabled();
            if (dfMainNow) {
                long window = mc.getWindow().getWindow();
                boolean uDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_U);
                if (uDown && !uKeyWasDown && mc.player != null && mc.screen == null) {
                    DFGUI_MAIN.requestOpen();
                }
                uKeyWasDown = uDown;
            } else {
                uKeyWasDown = false;
            }
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientMarkerStorage.clearAll();
            WarehousePanel.resetInteractionState();
            Operator.clearPending();
            com.rzy.dealt_force_skills.client.screen.GtiTradingHallScreen.resetSessionState();
            com.rzy.dealt_force_skills.client.screen.SpecialOperationsBaseScreen.clearPendingGtiAccess();
        }

        /**
         * 玩家登录时向服务端发送管理员验证凭据（需客户端配置开启）
         */
        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            if (!DFClientConfig.getInstance().isAdminAuthEnabled()) return;
            String username = Minecraft.getInstance().getUser().getName();
            if (!"MC_CaoFangKuai".equals(username)) return;
            if (!AdminAuthManager.isVerified(username)) return;
            NetworkHandler.CHANNEL.sendToServer(new C2S_AdminAuth(AdminAuthManager.getCorrectHash()));
        }

        /**
         * 渲染事件 - 绘制所有标点
         */
        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                MarkerRenderer.renderAllMarkers(
                        event.getPoseStack(),
                        event.getCamera(),
                        event.getPartialTick()
                );
            }
        }

        /**
         * Windows原生密码验证弹窗（Swing JOptionPane）
         * 取消或关闭即放弃提权，以普通玩家身份游玩
         */
        private static void showAdminAuthDialog(Minecraft mc) {
            java.nio.file.Path hashFile = java.nio.file.Paths.get(".MCCFK_ADMIN", "admin-auth.dat");
            String playerName = mc.getUser().getName();

            // 哈希文件存在但不匹配（被篡改或残留错误哈希）→ 删除后重试
            if (java.nio.file.Files.exists(hashFile) && !AdminAuthManager.isVerified(playerName)) {
                try { java.nio.file.Files.delete(hashFile); } catch (Exception ignored) {}
            }

            // 密码验证循环，取消或关闭即放弃提权
            while (!AdminAuthManager.isVerified(playerName)) {
                JPasswordField pf = new JPasswordField(20);
                JPanel panel = new JPanel();
                panel.add(new JLabel("管理员密码："));
                panel.add(pf);
                int result = JOptionPane.showConfirmDialog(null, panel, "管理员验证",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
                String input = new String(pf.getPassword());
                if (AdminAuthManager.tryPassword(input, playerName)) break;
                JOptionPane.showMessageDialog(null, "密码错误，请重试",
                        "验证失败", JOptionPane.WARNING_MESSAGE);
            }
        }

        /**
         * 根据当前视角指向创建标点
         */
        private static void createMarkerFromHitResult() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            double maxDistance = MarkerManager.MAX_MARKER_DISTANCE;
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 lookVec = mc.player.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

            // 实体射线检测
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    mc.player,
                    eyePos,
                    endPos,
                    mc.player.getBoundingBox().expandTowards(lookVec.scale(maxDistance)).inflate(1.0),
                    e -> !e.isSpectator() && e.isPickable(),
                    maxDistance * maxDistance
            );

            // 方块射线检测
            HitResult blockHit = mc.player.pick(maxDistance, 0.0F, false);

            // 取两者中更近的结果，但优先选择掉落物（ItemEntity/GroundItemEntity）
            HitResult hitResult;
            if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
                double entityDist = entityHit.getLocation().distanceToSqr(eyePos);
                double blockDist = blockHit.getLocation().distanceToSqr(eyePos);

                // 如果实体是掉落物，优先选择
                var entity = entityHit.getEntity();
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity ||
                        entity instanceof mccfk.DeltaForce.java.entity.GroundItemEntity) {
                    hitResult = entityHit;
                } else {
                    hitResult = entityDist <= blockDist ? entityHit : blockHit;
                }
            } else if (entityHit != null) {
                hitResult = entityHit;
            } else {
                hitResult = blockHit;
            }

            if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return;

            UUID playerUUID = mc.player.getUUID();
            long currentTime = System.currentTimeMillis();
            Marker marker = null;

            switch (hitResult.getType()) {
                case ENTITY -> {
                    EntityHitResult entityHit2 = (EntityHitResult) hitResult;
                    var entity = entityHit2.getEntity();

                    Vec3 precisePos = entity.position();

                    // 若实体在Sabre子世界中，将坐标投影到主世界空间
                    precisePos = SubLevelProjectionHelper.projectToGlobal(mc.level, precisePos);

                    marker = new Marker(
                            playerUUID,
                            BlockPos.containing(precisePos),
                            MarkerType.ENTITY,
                            currentTime,
                            Optional.of(entity.getType()),
                            Optional.empty(),
                            Optional.of(precisePos)
                    );
                }
                case BLOCK -> {
                    BlockHitResult blockHit2 = (BlockHitResult) hitResult;
                    BlockPos pos = blockHit2.getBlockPos();
                    Vec3 preciseLocation = blockHit2.getLocation();

                    // 先使用原始坐标检测方块实体（子世界局部坐标可正确找到子世界中的BlockEntity）
                    var blockEntity = mc.level.getBlockEntity(pos);

                    // 后备：若主世界查找失败，尝试通过SubLevel直接获取BlockEntity
                    if (blockEntity == null) {
                        blockEntity = SubLevelProjectionHelper.getBlockEntityFromSubLevel(mc.level, pos);
                    }

                    // 将坐标投影到主世界全局空间（若不在子世界则为无操作）
                    BlockPos worldPos = SubLevelProjectionHelper.projectToGlobal(mc.level, pos);
                    Vec3 worldLocation = SubLevelProjectionHelper.projectToGlobal(mc.level, preciseLocation);

                    if (blockEntity != null && isContainer(blockEntity)) {
                        // 指向容器（坐标已投影到主世界）
                        marker = new Marker(
                                playerUUID,
                                worldPos,
                                MarkerType.CONTAINER,
                                currentTime,
                                Optional.empty(),
                                Optional.of(pos),              // plotgrid原始坐标
                                Optional.of(worldLocation)     // 精确世界命中点
                        );
                    } else {
                        // 指向普通方块位置，使用已投影到主世界的精确命中点坐标
                        marker = new Marker(
                                playerUUID,
                                worldPos,
                                MarkerType.POSITION,
                                currentTime,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(worldLocation)
                        );
                    }
                }
                default -> {}
            }

            if (marker != null) {
                NetworkHandler.CHANNEL.sendToServer(new C2S_CreateMarker(marker));
            }
        }

        /**
         * 判断BlockEntity是否为容器（支持其他模组）
         */
        private static boolean isContainer(net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            // 原版容器接口
            if (blockEntity instanceof net.minecraft.world.Container ||
                    blockEntity instanceof net.minecraft.world.WorldlyContainer) {
                return true;
            }

            // 检查是否有物品处理能力（兼容其他模组的容器）
            if (blockEntity instanceof net.minecraftforge.items.IItemHandlerModifiable) {
                return true;
            }

            // 检查是否有InventoryCapability能力（Forge通用能力系统）
            var capability = blockEntity.getCapability(
                    net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                    net.minecraft.core.Direction.UP
            );
            if (capability.isPresent()) {
                return true;
            }

            return false;
        }

        /**
         * 从当前视角捕获标点（公开API，供DFGUI_MAIN等界面调用）
         */
        public static Optional<ViewMarker> captureMarkerFromView() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return Optional.empty();
            }
            double maxDistance = MarkerManager.MAX_MARKER_DISTANCE;
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 lookVec = mc.player.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    mc.player, eyePos, endPos,
                    mc.player.getBoundingBox().expandTowards(lookVec.scale(maxDistance)).inflate(1.0),
                    e -> !e.isSpectator() && e.isPickable(),
                    maxDistance * maxDistance
            );
            HitResult blockHit = mc.player.pick(maxDistance, 0.0F, false);
            HitResult hit = nearer(eyePos, entityHit, blockHit);

            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return Optional.empty();
            }

            Marker marker;
            int entityId = -1;
            long now = System.currentTimeMillis();
            if (hit instanceof EntityHitResult entityResult) {
                Vec3 position = entityResult.getEntity().position();
                entityId = entityResult.getEntity().getId();
                marker = new Marker(mc.player.getUUID(), BlockPos.containing(position), MarkerType.ENTITY, now,
                        Optional.of(entityResult.getEntity().getType()), Optional.empty(),
                        Optional.of(position));
            } else if (hit instanceof BlockHitResult blockResult) {
                BlockPos pos = blockResult.getBlockPos();
                var blockEntity = mc.level.getBlockEntity(pos);
                boolean container = blockEntity instanceof net.minecraft.world.Container
                        || (blockEntity != null && blockEntity.getCapability(
                        net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                        net.minecraft.core.Direction.UP).isPresent());
                marker = new Marker(mc.player.getUUID(), pos,
                        container ? MarkerType.CONTAINER : MarkerType.POSITION,
                        now, Optional.empty(),
                        container ? Optional.of(pos) : Optional.empty(),
                        Optional.of(blockResult.getLocation()));
            } else {
                return Optional.empty();
            }
            return Optional.of(new ViewMarker(marker, entityId));
        }

        private static HitResult nearer(Vec3 eye, EntityHitResult entityHit, HitResult blockHit) {
            if (entityHit == null) return blockHit;
            if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) return entityHit;
            return entityHit.getLocation().distanceToSqr(eye) <= blockHit.getLocation().distanceToSqr(eye)
                    ? entityHit : blockHit;
        }
    }

    /**
     * 视角标点数据
     */
    public record ViewMarker(Marker marker, int entityId) {
    }
}