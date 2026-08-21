package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_SetCommanderSkill(CommanderSkillChoice choice, String characterId) {
    public static void encode(C2S_SetCommanderSkill msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.choice.ordinal());
        buf.writeUtf(msg.characterId, 256);
    }

    public static C2S_SetCommanderSkill decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderSkillChoice[] values = CommanderSkillChoice.values();
        CommanderSkillChoice choice = ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
        return new C2S_SetCommanderSkill(choice, buf.readUtf(256));
    }

    public static void handle(C2S_SetCommanderSkill msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.setSelectedSource(player, msg.choice, msg.characterId);
            }
        });
        context.get().setPacketHandled(true);
    }
}
