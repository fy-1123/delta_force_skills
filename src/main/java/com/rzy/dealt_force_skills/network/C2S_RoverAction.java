package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_RoverAction(int action) {
    public static void encode(C2S_RoverAction message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.action); }
    public static C2S_RoverAction decode(FriendlyByteBuf buffer) { return new C2S_RoverAction(buffer.readVarInt()); }
    public static void handle(C2S_RoverAction message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null || !player.isAlive() || !RoverStateManager.isRover(player) || message.action < 0 || message.action > 3) return;
            SkillSlot slot = message.action >= 2 ? SkillSlot.ACTIVE_2 : SkillSlot.ACTIVE_1;
            if (!SkillDispatcher.passesInputGuards(player, slot)) return;
            if (message.action != 0 && ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.ROVER, slot)) return;
            RoverStateManager.toolAction(player, message.action);
        });
        context.get().setPacketHandled(true);
    }
}
