package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2S_StartPageAction(Action action, UUID playerId, String teamName) {
    private static final UUID NO_PLAYER = new UUID(0L, 0L);

    public enum Action {
        REFRESH,
        CREATE,
        REQUEST_JOIN,
        INVITE,
        ACCEPT_INVITE,
        DECLINE_INVITE,
        ACCEPT_JOIN,
        DECLINE_JOIN,
        LEAVE,
        KICK,
        PROMOTE
    }

    public static C2S_StartPageAction refresh() {
        return new C2S_StartPageAction(Action.REFRESH, NO_PLAYER, "");
    }

    public static void encode(C2S_StartPageAction msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUUID(msg.playerId == null ? NO_PLAYER : msg.playerId);
        buf.writeUtf(msg.teamName == null ? "" : msg.teamName, 64);
    }

    public static C2S_StartPageAction decode(FriendlyByteBuf buf) {
        return new C2S_StartPageAction(buf.readEnum(Action.class), buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(C2S_StartPageAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            runAction(player, msg);
            if (msg.action == Action.REFRESH) {
                NetworkHandler.sendToPlayer(S2C_StartPageSnapshot.capture(player), player);
                return;
            }
            for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
                NetworkHandler.sendToPlayer(S2C_StartPageSnapshot.capture(online), online);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void runAction(ServerPlayer player, C2S_StartPageAction msg) {
        switch (msg.action) {
            case REFRESH -> CharacterAvailability.syncToClient(player);
            case CREATE -> DealtTeamManager.createPersistentSquad(player);
            case REQUEST_JOIN -> DealtTeamManager.requestJoin(player, msg.teamName);
            case INVITE -> {
                ServerPlayer target = player.server.getPlayerList().getPlayer(msg.playerId);
                if (target == null) {
                    player.displayClientMessage(Component.literal("该玩家当前不在线。"), false);
                } else {
                    DealtTeamManager.invite(player, target);
                }
            }
            case ACCEPT_INVITE -> DealtTeamManager.respondInvite(player, msg.teamName, true);
            case DECLINE_INVITE -> DealtTeamManager.respondInvite(player, msg.teamName, false);
            case ACCEPT_JOIN -> DealtTeamManager.respondPersistentJoin(player, msg.playerId, true);
            case DECLINE_JOIN -> DealtTeamManager.respondPersistentJoin(player, msg.playerId, false);
            case LEAVE -> DealtTeamManager.leavePersistentSquad(player);
            case KICK -> DealtTeamManager.kickPersistentMember(player, msg.playerId);
            case PROMOTE -> DealtTeamManager.promotePersistentMember(player, msg.playerId);
        }
    }
}
