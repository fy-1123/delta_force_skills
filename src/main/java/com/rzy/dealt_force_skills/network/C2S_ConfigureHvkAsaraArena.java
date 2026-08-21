package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_ConfigureHvkAsaraArena(BlockPos pos, String structureId, long modifierMask, boolean start) {
    public static void encode(C2S_ConfigureHvkAsaraArena msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.structureId, HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        buf.writeLong(msg.modifierMask);
        buf.writeBoolean(msg.start);
    }

    public static C2S_ConfigureHvkAsaraArena decode(FriendlyByteBuf buf) {
        return new C2S_ConfigureHvkAsaraArena(buf.readBlockPos(),
                buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH), buf.readLong(), buf.readBoolean());
    }

    public static void handle(C2S_ConfigureHvkAsaraArena msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getBlockEntity(msg.pos) instanceof HvkAsaraArenaBlockEntity arena) {
                arena.configure(player, msg.structureId, msg.modifierMask, msg.start);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
