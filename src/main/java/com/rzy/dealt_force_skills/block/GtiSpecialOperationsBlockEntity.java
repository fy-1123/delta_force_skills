package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GtiSpecialOperationsBlockEntity extends BlockEntity {
    public GtiSpecialOperationsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GTI_SPECIAL_OPERATIONS.get(), pos, state);
    }
}
