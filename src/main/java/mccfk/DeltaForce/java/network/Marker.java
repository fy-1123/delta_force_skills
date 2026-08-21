package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public record Marker(
        UUID owner,
        BlockPos position,
        MarkerType type,
        long createTime,
        Optional<EntityType<?>> entityType,
        Optional<BlockPos> containerPos,
        Optional<Vec3> precisePosition
) {
    public Marker(UUID owner, BlockPos position, MarkerType type, long createTime) {
        this(owner, position, type, createTime, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
