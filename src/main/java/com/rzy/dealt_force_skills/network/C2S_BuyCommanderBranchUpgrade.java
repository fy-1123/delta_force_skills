package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_BuyCommanderBranchUpgrade(CommanderBranchUpgrade upgrade) {
    public static void encode(C2S_BuyCommanderBranchUpgrade msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.upgrade.ordinal());
    }

    public static C2S_BuyCommanderBranchUpgrade decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderBranchUpgrade[] values = CommanderBranchUpgrade.values();
        return new C2S_BuyCommanderBranchUpgrade(
                ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0]);
    }

    public static void handle(C2S_BuyCommanderBranchUpgrade msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.buyBranchUpgrade(player, msg.upgrade);
            }
        });
        context.get().setPacketHandled(true);
    }
}
