package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Server-driven zipline position sample.
 *
 * <p>The traversal is authoritative on the server; this packet mirrors the player's own position on the
 * riding client so the ride is smooth without a carrier entity. A plain position sample (rather than a
 * {@code ClientboundPlayerPositionPacket}) keeps the connection out of {@code awaitingPositionFromClient},
 * so look rotation and local packets are never dropped while riding.</p>
 */
public record S2C_HvkZiplineMotion(boolean active, double x, double y, double z) {
    public static void encode(S2C_HvkZiplineMotion msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
    }
    public static S2C_HvkZiplineMotion decode(FriendlyByteBuf buf) {
        return new S2C_HvkZiplineMotion(buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(S2C_HvkZiplineMotion msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.HvkZiplineMotionClient", "apply",
                msg.active, msg.x, msg.y, msg.z));
        ctx.get().setPacketHandled(true);
    }
}
