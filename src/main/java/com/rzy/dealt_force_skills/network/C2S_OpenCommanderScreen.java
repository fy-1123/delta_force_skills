package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_OpenCommanderScreen(boolean support) {
    public static void encode(C2S_OpenCommanderScreen msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.support);
    }

    public static C2S_OpenCommanderScreen decode(FriendlyByteBuf buf) {
        return new C2S_OpenCommanderScreen(buf.readBoolean());
    }

    public static void handle(C2S_OpenCommanderScreen msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.initializeIfNeeded(player);
                CommanderStateManager.syncToClient(player);
                NetworkHandler.sendToPlayer(new S2C_OpenCommanderScreen(msg.support), player);
            }
        });
        context.get().setPacketHandled(true);
    }
}
