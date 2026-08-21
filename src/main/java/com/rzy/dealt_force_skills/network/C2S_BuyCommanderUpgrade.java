package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_BuyCommanderUpgrade(CommanderUpgrade upgrade) {
    public static void encode(C2S_BuyCommanderUpgrade msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.upgrade.ordinal());
    }

    public static C2S_BuyCommanderUpgrade decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderUpgrade[] values = CommanderUpgrade.values();
        return new C2S_BuyCommanderUpgrade(ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0]);
    }

    public static void handle(C2S_BuyCommanderUpgrade msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.buyUpgrade(player, msg.upgrade);
            }
        });
        context.get().setPacketHandled(true);
    }
}
