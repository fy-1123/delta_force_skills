package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.music.FoxMusicPlayerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record C2S_FoxMusicAction(UUID device, FoxMusicPlayerManager.Action action, int index, int page) {
    public static void encode(C2S_FoxMusicAction msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.device); buf.writeEnum(msg.action); buf.writeVarInt(msg.index); buf.writeVarInt(msg.page);
    }
    public static C2S_FoxMusicAction decode(FriendlyByteBuf buf) {
        return new C2S_FoxMusicAction(buf.readUUID(), buf.readEnum(FoxMusicPlayerManager.Action.class), buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(C2S_FoxMusicAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) FoxMusicPlayerManager.action(player, msg.device, msg.action, msg.index, msg.page);
        });
        ctx.get().setPacketHandled(true);
    }
}
