package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_ClearSelectedCharacter {
    private final boolean becomeNormalPlayer;

    public C2S_ClearSelectedCharacter(boolean becomeNormalPlayer) {
        this.becomeNormalPlayer = becomeNormalPlayer;
    }

    public static void encode(C2S_ClearSelectedCharacter msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.becomeNormalPlayer);
    }

    public static C2S_ClearSelectedCharacter decode(FriendlyByteBuf buf) {
        return new C2S_ClearSelectedCharacter(buf.readBoolean());
    }

    public static void handle(C2S_ClearSelectedCharacter msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isSpectator()) {
                return;
            }
            CharacterSelectionManager.clearSelectedCharacter(player, msg.becomeNormalPlayer);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.character_cleared"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
