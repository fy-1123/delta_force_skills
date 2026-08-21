package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarkerManager {
    private static final Map<UUID, Marker> MARKERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CREATION_TIMES = new ConcurrentHashMap<>();
    private static final long MARKER_LIFETIME_MS = 30_000L;
    private static final long CREATE_COOLDOWN_MS = 250L;
    public static final double MAX_MARKER_DISTANCE = 256.0D;

    private MarkerManager() {
    }

    public static boolean addMarker(UUID playerId, Marker marker) {
        long now = System.currentTimeMillis();
        Long previous = LAST_CREATION_TIMES.get(playerId);
        if (previous != null && now - previous < CREATE_COOLDOWN_MS) {
            return false;
        }
        LAST_CREATION_TIMES.put(playerId, now);
        MARKERS.put(playerId, marker);
        return true;
    }

    public static Optional<Marker> validateRequest(ServerPlayer player, Marker request) {
        if (request == null || request.type() == null || request.position() == null) {
            return Optional.empty();
        }
        BlockPos position = request.position();
        Vec3 precise = request.precisePosition().orElse(Vec3.atCenterOf(position));
        if (!isFinite(precise)
                || precise.distanceToSqr(Vec3.atCenterOf(position)) > 9.0D
                || precise.distanceToSqr(player.position()) > MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE
                || position.getY() < player.serverLevel().getMinBuildHeight() - 64
                || position.getY() > player.serverLevel().getMaxBuildHeight() + 64
                || !player.serverLevel().getWorldBorder().isWithinBounds(position)) {
            return Optional.empty();
        }
        return Optional.of(new Marker(
                player.getUUID(),
                position.immutable(),
                request.type(),
                System.currentTimeMillis(),
                request.type() == MarkerType.ENTITY ? request.entityType() : Optional.empty(),
                request.type() == MarkerType.CONTAINER ? request.containerPos().map(BlockPos::immutable) : Optional.empty(),
                request.precisePosition().map(value -> new Vec3(value.x, value.y, value.z))
        ));
    }

    public static Optional<Vec3> validatePosition(ServerPlayer player, Vec3 position) {
        if (player == null || position == null) {
            return Optional.empty();
        }
        BlockPos blockPos = BlockPos.containing(position);
        Marker request = new Marker(player.getUUID(), blockPos, MarkerType.POSITION,
                System.currentTimeMillis(), Optional.empty(), Optional.empty(), Optional.of(position));
        return validateRequest(player, request).flatMap(Marker::precisePosition);
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public static void removeMarker(UUID playerId) {
        MARKERS.remove(playerId);
        LAST_CREATION_TIMES.remove(playerId);
    }

    public static Collection<Marker> getAllMarkers() {
        return MARKERS.values();
    }

    public static boolean tick() {
        long now = System.currentTimeMillis();
        return MARKERS.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().createTime();
            return age < 0L || age > MARKER_LIFETIME_MS;
        });
    }

    public static void clearAll() {
        MARKERS.clear();
        LAST_CREATION_TIMES.clear();
    }
}
