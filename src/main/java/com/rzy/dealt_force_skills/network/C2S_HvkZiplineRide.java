package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_HvkZiplineRide(BlockPos origin) {
    public static void encode(C2S_HvkZiplineRide msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.origin);
    }
    public static C2S_HvkZiplineRide decode(FriendlyByteBuf buf) {
        return new C2S_HvkZiplineRide(buf.readBlockPos());
    }
    public static void handle(C2S_HvkZiplineRide msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> HvkZiplineManager.requestRide(ctx.get().getSender(), msg.origin));
        ctx.get().setPacketHandled(true);
    }
}
