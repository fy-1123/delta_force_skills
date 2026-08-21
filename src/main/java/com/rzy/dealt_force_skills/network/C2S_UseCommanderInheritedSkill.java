package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2S_UseCommanderInheritedSkill {
    private final CommanderSkillChoice choice;
    private final boolean inheritedAlternate;

    public C2S_UseCommanderInheritedSkill(CommanderSkillChoice choice, boolean inheritedAlternate) {
        this.choice = choice;
        this.inheritedAlternate = inheritedAlternate;
    }

    public static void encode(C2S_UseCommanderInheritedSkill msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.choice);
        buf.writeBoolean(msg.inheritedAlternate);
    }

    public static C2S_UseCommanderInheritedSkill decode(FriendlyByteBuf buf) {
        return new C2S_UseCommanderInheritedSkill(buf.readEnum(CommanderSkillChoice.class), buf.readBoolean());
    }

    public static void handle(C2S_UseCommanderInheritedSkill msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isSpectator()) {
                return;
            }
            SkillDispatcher.useCommanderSelectedSkill(player, msg.choice, msg.inheritedAlternate);
        });
        ctx.get().setPacketHandled(true);
    }
}
