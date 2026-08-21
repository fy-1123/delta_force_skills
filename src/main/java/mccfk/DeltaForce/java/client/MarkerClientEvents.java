package mccfk.DeltaForce.java.client;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.client.screen.GtiTradingHallScreen;
import com.rzy.dealt_force_skills.client.screen.SpecialOperationsBaseScreen;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.screen.Operator;
import mccfk.DeltaForce.java.client.screen.WarehousePanel;
import mccfk.DeltaForce.java.client.renderer.MarkerRenderer;
import mccfk.DeltaForce.java.entity.GroundItemEntity;
import mccfk.DeltaForce.java.network.C2S_CreateMarker;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.network.Marker;
import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.MarkerType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = DeltaForce.MODID, value = Dist.CLIENT)
public final class MarkerClientEvents {
    private MarkerClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientMarkerStorage.cleanExpired();
        while (DeltaForceClient.MARKER_KEY.consumeClick()) createMarkerFromView();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMarkerStorage.clearAll();
        WarehousePanel.resetInteractionState();
        Operator.clearPending();
        GtiTradingHallScreen.resetSessionState();
        SpecialOperationsBaseScreen.clearPendingGtiAccess();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            MarkerRenderer.renderAllMarkers(event.getPoseStack(), event.getCamera());
        }
    }

    private static void createMarkerFromView() {
        captureMarkerFromView().ifPresent(view ->
                NetworkHandler.sendToServer(new C2S_CreateMarker(view.marker())));
    }

    public static Optional<ViewMarker> captureMarkerFromView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return Optional.empty();
        }
        double distance = MarkerManager.MAX_MARKER_DISTANCE;
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle();
        Vec3 end = eye.add(look.scale(distance));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(minecraft.player, eye, end,
                minecraft.player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isPickable(), distance * distance);
        HitResult blockHit = minecraft.player.pick(distance, 0.0F, false);
        HitResult hit = nearer(eye, entityHit, blockHit);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }

        Marker marker;
        int entityId = -1;
        long now = System.currentTimeMillis();
        if (hit instanceof EntityHitResult entityResult) {
            Vec3 position = entityResult.getEntity().position();
            entityId = entityResult.getEntity().getId();
            marker = new Marker(minecraft.player.getUUID(), BlockPos.containing(position), MarkerType.ENTITY, now,
                    Optional.of(entityResult.getEntity().getType()), Optional.empty(), Optional.of(position));
        } else if (hit instanceof BlockHitResult blockResult) {
            BlockPos position = blockResult.getBlockPos();
            BlockEntity blockEntity = minecraft.level.getBlockEntity(position);
            boolean container = isContainer(blockEntity);
            marker = new Marker(minecraft.player.getUUID(), position, container ? MarkerType.CONTAINER : MarkerType.POSITION,
                    now, Optional.empty(), container ? Optional.of(position) : Optional.empty(),
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

    private static boolean isContainer(BlockEntity blockEntity) {
        return blockEntity instanceof Container
                || blockEntity != null && blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    public record ViewMarker(Marker marker, int entityId) {
    }
}
