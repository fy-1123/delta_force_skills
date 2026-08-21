package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2C_CommanderSnapshot(long tacticalPoints, String[] sources, String[] enabledPassives,
                                   int[] upgrades, int[] cooldowns, boolean[] branchUpgrades) {
    public static S2C_CommanderSnapshot from(ServerPlayer player) {
        CommanderSkillChoice[] choices = CommanderSkillChoice.values();
        String[] sources = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            sources[i] = CommanderStateManager.selectedSource(player, choices[i]).orElse("");
        }
        CommanderUpgrade[] upgradeValues = CommanderUpgrade.values();
        int[] upgrades = new int[upgradeValues.length];
        for (int i = 0; i < upgradeValues.length; i++) {
            upgrades[i] = CommanderStateManager.upgradeLevel(player, upgradeValues[i]);
        }
        CommanderSupport[] supportValues = CommanderSupport.values();
        int[] cooldowns = new int[supportValues.length];
        for (int i = 0; i < supportValues.length; i++) {
            cooldowns[i] = CommanderStateManager.supportCooldownTicks(player, supportValues[i]);
        }
        CommanderBranchUpgrade[] branchValues = CommanderBranchUpgrade.values();
        boolean[] branchUpgrades = new boolean[branchValues.length];
        for (int i = 0; i < branchValues.length; i++) {
            branchUpgrades[i] = CommanderStateManager.ownsBranchUpgrade(player, branchValues[i]);
        }
        String[] enabledPassives = CommanderStateManager.enabledPassives(player).toArray(String[]::new);
        return new S2C_CommanderSnapshot(CommanderStateManager.tacticalPoints(player), sources, enabledPassives,
                upgrades, cooldowns, branchUpgrades);
    }

    public static void encode(S2C_CommanderSnapshot msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.tacticalPoints);
        buf.writeVarInt(msg.sources.length);
        for (String source : msg.sources) {
            buf.writeUtf(source, 256);
        }
        buf.writeVarInt(msg.enabledPassives.length);
        for (String passive : msg.enabledPassives) {
            buf.writeUtf(passive, 256);
        }
        buf.writeVarInt(msg.upgrades.length);
        for (int level : msg.upgrades) {
            buf.writeVarInt(level);
        }
        buf.writeVarInt(msg.cooldowns.length);
        for (int cooldown : msg.cooldowns) {
            buf.writeVarInt(cooldown);
        }
        buf.writeVarInt(msg.branchUpgrades.length);
        for (boolean owned : msg.branchUpgrades) {
            buf.writeBoolean(owned);
        }
    }

    public static S2C_CommanderSnapshot decode(FriendlyByteBuf buf) {
        long points = buf.readVarLong();
        String[] sources = new String[Math.min(32, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = buf.readUtf(256);
        }
        String[] enabledPassives = new String[Math.min(64, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < enabledPassives.length; i++) {
            enabledPassives[i] = buf.readUtf(256);
        }
        int[] upgrades = new int[Math.min(16, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < upgrades.length; i++) {
            upgrades[i] = buf.readVarInt();
        }
        int[] cooldowns = new int[Math.min(32, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = buf.readVarInt();
        }
        boolean[] branchUpgrades = new boolean[Math.min(128, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < branchUpgrades.length; i++) {
            branchUpgrades[i] = buf.readBoolean();
        }
        return new S2C_CommanderSnapshot(points, sources, enabledPassives, upgrades, cooldowns, branchUpgrades);
    }

    public static void handle(S2C_CommanderSnapshot msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCommanderState.sync(msg.tacticalPoints, msg.sources, msg.enabledPassives,
                        msg.upgrades, msg.cooldowns, msg.branchUpgrades)));
        context.get().setPacketHandled(true);
    }
}
