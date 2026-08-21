package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.boss.AcceleratedTroopsSquadManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class AcceleratedTroopsSpawnEggItem extends Item {
    public AcceleratedTroopsSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Vec3 center = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        if (AcceleratedTroopsSquadManager.spawnSquad(level, center) == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
