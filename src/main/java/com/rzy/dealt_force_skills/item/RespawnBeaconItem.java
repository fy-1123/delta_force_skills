package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.entity.RespawnBeaconMarkerEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class RespawnBeaconItem extends QualityTooltipItem {
    private static final String MARKER_ID = "DealtForceRespawnBeaconMarker";
    private static final String MARKER_DIMENSION = "DealtForceRespawnBeaconDimension";
    private static final String MARKER_POSITION = "DealtForceRespawnBeaconPosition";
    private static final String MARKER_REMAINING_USES = "DealtForceRespawnBeaconRemainingUses";

    public RespawnBeaconItem(Properties properties) {
        super(properties.stacksTo(16), DfsItemQuality.RED,
                "tooltip.dealt_force_skills.respawn_beacon");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            removeExistingMarker(serverPlayer);
            RespawnBeaconMarkerEntity marker = ModEntities.RESPAWN_BEACON_MARKER.get().create(serverPlayer.serverLevel());
            if (marker == null) {
                return InteractionResultHolder.fail(stack);
            }
            marker.setOwnerId(serverPlayer.getUUID());
            marker.setRemainingUses(RespawnBeaconMarkerEntity.MAX_USES);
            marker.moveTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 0.0F, 0.0F);
            serverPlayer.serverLevel().addFreshEntity(marker);
            serverPlayer.getPersistentData().putUUID(MARKER_ID, marker.getUUID());
            serverPlayer.getPersistentData().putString(MARKER_DIMENSION,
                    serverPlayer.serverLevel().dimension().location().toString());
            serverPlayer.getPersistentData().putLong(MARKER_POSITION, marker.blockPosition().asLong());
            serverPlayer.getPersistentData().putInt(MARKER_REMAINING_USES,
                    RespawnBeaconMarkerEntity.MAX_USES);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.respawn_beacon.bound",
                    RespawnBeaconMarkerEntity.MAX_USES), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static RespawnBeaconMarkerEntity marker(ServerPlayer player) {
        if (player == null || !player.getPersistentData().hasUUID(MARKER_ID)) {
            return null;
        }
        UUID id = player.getPersistentData().getUUID(MARKER_ID);
        ResourceLocation dimensionId = ResourceLocation.tryParse(
                player.getPersistentData().getString(MARKER_DIMENSION));
        ServerLevel level = dimensionId == null ? null : player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level != null) {
            BlockPos markerPos = BlockPos.of(player.getPersistentData().getLong(MARKER_POSITION));
            level.getChunkAt(markerPos);
            Entity entity = level.getEntity(id);
            if (entity instanceof RespawnBeaconMarkerEntity marker && marker.isOwnedBy(player.getUUID())) {
                if (player.getPersistentData().contains(MARKER_REMAINING_USES)) {
                    marker.setRemainingUses(player.getPersistentData().getInt(MARKER_REMAINING_USES));
                } else {
                    player.getPersistentData().putInt(MARKER_REMAINING_USES, marker.remainingUses());
                }
                if (marker.remainingUses() <= 0) {
                    marker.discard();
                    clearBinding(player);
                    return null;
                }
                return marker;
            }
        }
        clearBinding(player);
        return null;
    }

    public static void consumeMarker(ServerPlayer player, RespawnBeaconMarkerEntity marker) {
        if (player == null) {
            return;
        }
        if (marker == null) {
            clearBinding(player);
            return;
        }
        int remaining = marker.consumeUse();
        if (remaining <= 0) {
            marker.discard();
            clearBinding(player);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.respawn_beacon.exhausted"), true);
        } else {
            player.getPersistentData().putInt(MARKER_REMAINING_USES, remaining);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.respawn_beacon.used", remaining,
                    RespawnBeaconMarkerEntity.MAX_USES), true);
        }
    }

    public static void displayMarkerStatus(ServerPlayer player) {
        if (player == null || player.tickCount % 20 != 0
                || !player.getPersistentData().hasUUID(MARKER_ID)) {
            return;
        }
        BlockPos markerPos = BlockPos.of(player.getPersistentData().getLong(MARKER_POSITION));
        int remaining = player.getPersistentData().contains(MARKER_REMAINING_USES)
                ? Math.max(0, Math.min(RespawnBeaconMarkerEntity.MAX_USES,
                player.getPersistentData().getInt(MARKER_REMAINING_USES)))
                : RespawnBeaconMarkerEntity.MAX_USES;
        player.getPersistentData().putInt(MARKER_REMAINING_USES, remaining);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.respawn_beacon.status",
                player.getPersistentData().getString(MARKER_DIMENSION),
                markerPos.getX(), markerPos.getY(), markerPos.getZ(), remaining,
                RespawnBeaconMarkerEntity.MAX_USES), true);
    }

    private static void removeExistingMarker(ServerPlayer player) {
        RespawnBeaconMarkerEntity marker = marker(player);
        if (marker != null) {
            marker.discard();
        }
        clearBinding(player);
    }

    private static void clearBinding(ServerPlayer player) {
        player.getPersistentData().remove(MARKER_ID);
        player.getPersistentData().remove(MARKER_DIMENSION);
        player.getPersistentData().remove(MARKER_POSITION);
        player.getPersistentData().remove(MARKER_REMAINING_USES);
    }
}
