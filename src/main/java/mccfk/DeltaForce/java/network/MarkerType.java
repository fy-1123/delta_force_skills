package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;
import java.util.UUID;

/**
 * 标点类型枚举
 */
public enum MarkerType {
    ENTITY,      // 指向实体（红色）
    CONTAINER,   // 指向容器（黄色描边）
    POSITION     // 指向位置（绿色）
}
