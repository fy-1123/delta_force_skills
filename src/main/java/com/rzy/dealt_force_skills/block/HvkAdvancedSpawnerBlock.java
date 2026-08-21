package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class HvkAdvancedSpawnerBlock extends BaseEntityBlock {
    public HvkAdvancedSpawnerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .requiresCorrectToolForDrops()
                .strength(5.0F, 1200.0F)
                .sound(SoundType.METAL));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HVK_ADVANCED_SPAWNER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.HVK_ADVANCED_SPAWNER.get(),
                (tickerLevel, pos, tickerState, spawner) -> {
                    if (tickerLevel instanceof ServerLevel serverLevel) {
                        HvkAdvancedSpawnerBlockEntity.serverTick(serverLevel, pos, tickerState, spawner);
                    }
                });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isCreative()) {
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof SpawnEggItem spawnEgg) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel
                    && level.getBlockEntity(pos) instanceof HvkAdvancedSpawnerBlockEntity spawner
                    && spawner.setEntityTypeFromEgg(spawnEgg.getType(held.getTag()), serverLevel)) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HvkAdvancedSpawnerBlockEntity spawner) {
            spawner.openConfiguration(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
