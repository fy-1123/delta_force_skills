package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_ToggleCommanderPassive(String characterId, boolean enabled) {
    public static void encode(C2S_ToggleCommanderPassive msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.characterId, 256);
        buf.writeBoolean(msg.enabled);
    }

    public static C2S_ToggleCommanderPassive decode(FriendlyByteBuf buf) {
        return new C2S_ToggleCommanderPassive(buf.readUtf(256), buf.readBoolean());
    }

    public static void handle(C2S_ToggleCommanderPassive msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.setPassiveEnabled(player, msg.characterId, msg.enabled);
            }
        });
        context.get().setPacketHandled(true);
    }
}
