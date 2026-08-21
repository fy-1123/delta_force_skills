package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import com.rzy.dealt_force_skills.client.screen.HvkAdvancedSpawnerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2C_OpenHvkAdvancedSpawner(
        BlockPos pos,
        String entityId,
        int spawnInterval,
        int spawnCount,
        int livingLimit,
        int totalLimit,
        int randomRange,
        int activityRange
) {
    public static void encode(S2C_OpenHvkAdvancedSpawner msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.entityId, HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH);
        buf.writeInt(msg.spawnInterval);
        buf.writeInt(msg.spawnCount);
        buf.writeInt(msg.livingLimit);
        buf.writeInt(msg.totalLimit);
        buf.writeInt(msg.randomRange);
        buf.writeInt(msg.activityRange);
    }

    public static S2C_OpenHvkAdvancedSpawner decode(FriendlyByteBuf buf) {
        return new S2C_OpenHvkAdvancedSpawner(buf.readBlockPos(),
                buf.readUtf(HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt());
    }

    public static void handle(S2C_OpenHvkAdvancedSpawner msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HvkAdvancedSpawnerScreen.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
