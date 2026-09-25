package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 标点数据记录类
 */
public record Marker(
    UUID owner,              // 创建者UUID
    BlockPos position,       // 位置
    MarkerType type,         // 类型
    long createTime,         // 创建时间戳（毫秒）
    Optional<EntityType<?>> entityType,  // 若指向实体
    Optional<BlockPos> containerPos,      // 若指向容器
    Optional<Vec3> precisePosition        // ENTITY/POSITION类型的精确实体/命中点坐标
) {
    public Marker(UUID owner, BlockPos position, MarkerType type, long createTime) {
        this(owner, position, type, createTime, Optional.empty(), Optional.empty(), Optional.empty());
    }
}