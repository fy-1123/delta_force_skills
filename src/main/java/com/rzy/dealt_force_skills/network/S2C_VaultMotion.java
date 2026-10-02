package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2C_VaultMotion(double x, double y, double z, double endX, double endY, double endZ) {
    public static void encode(S2C_VaultMotion msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
        buf.writeDouble(msg.endX); buf.writeDouble(msg.endY); buf.writeDouble(msg.endZ);
    }
    public static S2C_VaultMotion decode(FriendlyByteBuf buf) {
        return new S2C_VaultMotion(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(S2C_VaultMotion msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.VaultCameraMotion", "update",
                msg.x, msg.y, msg.z, msg.endX, msg.endY, msg.endZ));
        ctx.get().setPacketHandled(true);
    }
}
