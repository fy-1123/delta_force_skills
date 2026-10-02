package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record S2C_FoxMusicPlayback(UUID device, UUID owner, int entityId, ResourceLocation sound,
                                   int generation, int elapsed, boolean playing, boolean removed) {
    public static void encode(S2C_FoxMusicPlayback msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.device); buf.writeUUID(msg.owner); buf.writeVarInt(msg.entityId); buf.writeResourceLocation(msg.sound);
        buf.writeInt(msg.generation); buf.writeVarInt(msg.elapsed); buf.writeBoolean(msg.playing); buf.writeBoolean(msg.removed);
    }
    public static S2C_FoxMusicPlayback decode(FriendlyByteBuf buf) {
        return new S2C_FoxMusicPlayback(buf.readUUID(), buf.readUUID(), buf.readVarInt(), buf.readResourceLocation(),
                buf.readInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }
    public static void handle(S2C_FoxMusicPlayback msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.FoxMusicClient", "update", msg));
        ctx.get().setPacketHandled(true);
    }
}
