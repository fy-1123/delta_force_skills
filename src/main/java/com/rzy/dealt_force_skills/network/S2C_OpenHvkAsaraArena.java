package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import com.rzy.dealt_force_skills.client.screen.HvkAsaraArenaScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2C_OpenHvkAsaraArena(BlockPos pos, String selectedStructure, long modifierMask,
                                     List<String> structures) {
    public static void encode(S2C_OpenHvkAsaraArena msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.selectedStructure, HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        buf.writeLong(msg.modifierMask);
        int count = Math.min(HvkAsaraArenaBlockEntity.MAX_STRUCTURES, msg.structures.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUtf(msg.structures.get(i), HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        }
    }

    public static S2C_OpenHvkAsaraArena decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String selected = buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        long mask = buf.readLong();
        int count = Math.min(HvkAsaraArenaBlockEntity.MAX_STRUCTURES, Math.max(0, buf.readVarInt()));
        List<String> structures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) structures.add(buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH));
        return new S2C_OpenHvkAsaraArena(pos, selected, mask, List.copyOf(structures));
    }

    public static void handle(S2C_OpenHvkAsaraArena msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HvkAsaraArenaScreen.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
