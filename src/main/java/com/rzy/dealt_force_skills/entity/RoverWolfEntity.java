package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.character.rover.RoverSounds;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_LunaRevealEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public final class RoverWolfEntity extends Wolf {
    public RoverWolfEntity(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new CommandMovementGoal());
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                5, true, false, this::isValidTarget));
    }

    private boolean isValidTarget(LivingEntity target) {
        double range = Math.max(1, RoverConfig.DOG_TARGET_RANGE);
        return target != this && getOwner() instanceof ServerPlayer owner
                && TargetingUtil.isHostileLivingFor(owner, target)
                && target.distanceToSqr(this) <= range * range;
    }

    @Override public boolean canAttack(LivingEntity target) {
        return isValidTarget(target) && super.canAttack(target);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || !RoverStateManager.isRover(owner)
                || !RoverStateManager.data(owner).hasUUID("Dog")
                || !getUUID().equals(RoverStateManager.data(owner).getUUID("Dog"))
                || owner.level() != level() || SkillCooldownHelper.now(this) >= getPersistentData().getLong("RoverExpires")) {
            discard(); return;
        }
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(owner.getAttributeValue(Attributes.MOVEMENT_SPEED) * 4.0);
        double range = Math.max(1, RoverConfig.DOG_TARGET_RANGE);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);
        LivingEntity target = getTarget();
        if (target != null && !isValidTarget(target)) {
            setTarget(null);
        }
    }

    private final class CommandMovementGoal extends Goal {
        CommandMovementGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            return getTarget() == null && getOwner() instanceof ServerPlayer;
        }

        @Override public void tick() {
            if (!(getOwner() instanceof ServerPlayer owner)) return;
            Vec3 point = RoverStateManager.waypoint(owner);
            if (point == null) point = owner.position();
            if (position().distanceToSqr(point) > 2.25) getNavigation().moveTo(point.x, point.y, point.z, 1.0);
            else getNavigation().stop();
            getLookControl().setLookAt(point.x, point.y + owner.getEyeHeight(), point.z);
        }

        @Override public void stop() { getNavigation().stop(); }
    }

    @Override public boolean doHurtTarget(Entity entity) {
        if (!(getOwner() instanceof ServerPlayer owner) || !(entity instanceof LivingEntity target)
                || !TargetingUtil.isHostileLivingFor(owner, target)) return false;
        boolean applied = SkillDamageHelper.hurt(target, damageSources().mobAttack(this), owner,
                (float) Math.max(0, RoverConfig.DOG_DAMAGE));
        if (applied) {
            NetworkHandler.sendToPlayer(new S2C_LunaRevealEntities(java.util.List.of(target.getId()),
                    Math.max(1, RoverConfig.DOG_REVEAL_DURATION)), owner);
            RoverStateManager.sound(owner, RoverSounds.DOG_ATTACK);
            if (level().getGameRules().getBoolean(ModGameRules.DEALT_WOUND) && random.nextDouble() < RoverConfig.DOG_INJURY_CHANCE) {
                boolean abdomen = target.hasEffect(ModEffects.ABDOMEN_INJURY.get());
                boolean chest = target.hasEffect(ModEffects.CHEST_INJURY.get());
                var effect = abdomen != chest ? (abdomen ? ModEffects.CHEST_INJURY.get() : ModEffects.ABDOMEN_INJURY.get())
                        : random.nextBoolean() ? ModEffects.ABDOMEN_INJURY.get() : ModEffects.CHEST_INJURY.get();
                target.addEffect(new MobEffectInstance(effect, Math.max(1, RoverConfig.DOG_INJURY_DURATION)), owner);
            }
        }
        return applied;
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)
                && getOwner() instanceof ServerPlayer owner
                && RoverStateManager.data(owner).hasUUID("Dog")
                && getUUID().equals(RoverStateManager.data(owner).getUUID("Dog"))) {
            RoverStateManager.finishDog(owner);
        }
        super.remove(reason);
    }
}
