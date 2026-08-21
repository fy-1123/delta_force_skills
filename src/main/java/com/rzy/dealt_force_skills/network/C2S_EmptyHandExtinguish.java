package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.util.EmptyHandExtinguishHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_EmptyHandExtinguish(boolean holding) {
    public static void encode(C2S_EmptyHandExtinguish message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.holding);
    }

    public static C2S_EmptyHandExtinguish decode(FriendlyByteBuf buffer) {
        return new C2S_EmptyHandExtinguish(buffer.readBoolean());
    }

    public static void handle(C2S_EmptyHandExtinguish message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                EmptyHandExtinguishHandler.signal(player, message.holding);
            }
        });
        context.setPacketHandled(true);
    }
}
