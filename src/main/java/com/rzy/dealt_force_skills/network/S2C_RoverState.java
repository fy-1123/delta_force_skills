package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2C_RoverState(CompoundTag state) {
    public static void encode(S2C_RoverState message, FriendlyByteBuf buffer) { buffer.writeNbt(message.state); }
    public static S2C_RoverState decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt(); return new S2C_RoverState(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(S2C_RoverState message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.rzy.dealt_force_skills.client.RoverClient.sync(message.state)));
        context.get().setPacketHandled(true);
    }
}
