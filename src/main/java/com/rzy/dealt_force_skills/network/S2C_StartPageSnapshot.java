package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import io.netty.handler.codec.DecoderException;
import mccfk.DeltaForce.java.client.screen.StartGameScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record S2C_StartPageSnapshot(
        List<RosterEntry> roster,
        String teamName,
        UUID leaderId,
        List<MemberEntry> members,
        List<TeamEntry> teams,
        List<InviteEntry> invitations,
        List<JoinRequestEntry> joinRequests) {
    private static final int MAX_ENTRIES = 512;

    public record RosterEntry(UUID playerId, String name, String characterId, float health, float maxHealth,
                              int armor, long playTimeTicks, int deaths) {
    }

    public record MemberEntry(UUID playerId, String name, boolean online) {
    }

    public record TeamEntry(String name, String leaderName, int memberCount, boolean joinRequested) {
    }

    public record InviteEntry(String teamName, String leaderName) {
    }

    public record JoinRequestEntry(UUID playerId, String name) {
    }

    public static S2C_StartPageSnapshot capture(ServerPlayer viewer) {
        List<RosterEntry> roster = new ArrayList<>();
        for (ServerPlayer player : viewer.server.getPlayerList().getPlayers()) {
            roster.add(new RosterEntry(player.getUUID(), player.getGameProfile().getName(),
                    CharacterSelectionManager.getSelectedCharacterId(player).orElse(""),
                    player.getHealth(), player.getMaxHealth(), player.getArmorValue(),
                    player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)),
                    player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS))));
        }
        DealtTeamManager.PersistentSquadView squad = DealtTeamManager.persistentSquadView(viewer);
        return new S2C_StartPageSnapshot(List.copyOf(roster), squad.teamName(), squad.leaderId(),
                squad.members().stream().map(member -> new MemberEntry(
                        member.playerId(), member.name(), member.online())).toList(),
                squad.teams().stream().map(team -> new TeamEntry(
                        team.name(), team.leaderName(), team.memberCount(), team.joinRequested())).toList(),
                squad.invitations().stream().map(invite -> new InviteEntry(
                        invite.teamName(), invite.leaderName())).toList(),
                squad.joinRequests().stream().map(request -> new JoinRequestEntry(
                        request.playerId(), request.name())).toList());
    }

    public static void encode(S2C_StartPageSnapshot msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.roster.size());
        for (RosterEntry entry : msg.roster) {
            buf.writeUUID(entry.playerId);
            buf.writeUtf(entry.name, 64);
            buf.writeUtf(entry.characterId, 128);
            buf.writeFloat(entry.health);
            buf.writeFloat(entry.maxHealth);
            buf.writeVarInt(entry.armor);
            buf.writeVarLong(entry.playTimeTicks);
            buf.writeVarInt(entry.deaths);
        }
        buf.writeUtf(msg.teamName, 64);
        buf.writeBoolean(msg.leaderId != null);
        if (msg.leaderId != null) {
            buf.writeUUID(msg.leaderId);
        }
        buf.writeVarInt(msg.members.size());
        for (MemberEntry entry : msg.members) {
            buf.writeUUID(entry.playerId);
            buf.writeUtf(entry.name, 64);
            buf.writeBoolean(entry.online);
        }
        buf.writeVarInt(msg.teams.size());
        for (TeamEntry entry : msg.teams) {
            buf.writeUtf(entry.name, 64);
            buf.writeUtf(entry.leaderName, 64);
            buf.writeVarInt(entry.memberCount);
            buf.writeBoolean(entry.joinRequested);
        }
        buf.writeVarInt(msg.invitations.size());
        for (InviteEntry entry : msg.invitations) {
            buf.writeUtf(entry.teamName, 64);
            buf.writeUtf(entry.leaderName, 64);
        }
        buf.writeVarInt(msg.joinRequests.size());
        for (JoinRequestEntry entry : msg.joinRequests) {
            buf.writeUUID(entry.playerId);
            buf.writeUtf(entry.name, 64);
        }
    }

    public static S2C_StartPageSnapshot decode(FriendlyByteBuf buf) {
        List<RosterEntry> roster = new ArrayList<>();
        for (int i = 0, count = readCount(buf, "roster"); i < count; i++) {
            roster.add(new RosterEntry(buf.readUUID(), buf.readUtf(64), buf.readUtf(128),
                    buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));
        }
        String teamName = buf.readUtf(64);
        UUID leaderId = buf.readBoolean() ? buf.readUUID() : null;
        List<MemberEntry> members = new ArrayList<>();
        for (int i = 0, count = readCount(buf, "members"); i < count; i++) {
            members.add(new MemberEntry(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
        }
        List<TeamEntry> teams = new ArrayList<>();
        for (int i = 0, count = readCount(buf, "teams"); i < count; i++) {
            teams.add(new TeamEntry(buf.readUtf(64), buf.readUtf(64), buf.readVarInt(), buf.readBoolean()));
        }
        List<InviteEntry> invitations = new ArrayList<>();
        for (int i = 0, count = readCount(buf, "invitations"); i < count; i++) {
            invitations.add(new InviteEntry(buf.readUtf(64), buf.readUtf(64)));
        }
        List<JoinRequestEntry> joinRequests = new ArrayList<>();
        for (int i = 0, count = readCount(buf, "join requests"); i < count; i++) {
            joinRequests.add(new JoinRequestEntry(buf.readUUID(), buf.readUtf(64)));
        }
        return new S2C_StartPageSnapshot(List.copyOf(roster), teamName, leaderId, List.copyOf(members),
                List.copyOf(teams), List.copyOf(invitations), List.copyOf(joinRequests));
    }

    public static void handle(S2C_StartPageSnapshot msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> StartGameScreen.acceptSnapshot(msg)));
        ctx.get().setPacketHandled(true);
    }

    private static int readCount(FriendlyByteBuf buf, String label) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Invalid start-page " + label + " count: " + count);
        }
        return count;
    }
}
