package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public final class RespawnBeaconMarkerEntity extends Entity implements ItemSupplier {
    public static final int MAX_USES = 4;
    private static final String REMAINING_USES = "RemainingUses";

    private UUID ownerId;
    private int remainingUses = MAX_USES;

    public RespawnBeaconMarkerEntity(EntityType<? extends RespawnBeaconMarkerEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isOwnedBy(UUID ownerId) {
        return ownerId != null && ownerId.equals(this.ownerId);
    }

    public int remainingUses() {
        return remainingUses;
    }

    public void setRemainingUses(int remainingUses) {
        this.remainingUses = Math.max(0, Math.min(MAX_USES, remainingUses));
    }

    public int consumeUse() {
        setRemainingUses(remainingUses - 1);
        return remainingUses;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        setRemainingUses(tag.contains(REMAINING_USES) ? tag.getInt(REMAINING_USES) : MAX_USES);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt(REMAINING_USES, remainingUses);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.RESPAWN_BEACON.get());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
