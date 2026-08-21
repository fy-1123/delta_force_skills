package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderSupportManager;
import mccfk.DeltaForce.java.network.MarkerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_CommanderSupport(CommanderSupport support, Vec3 position, int targetId, String vehicleId) {
    public static void encode(C2S_CommanderSupport msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.support.ordinal());
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeVarInt(msg.targetId);
        buf.writeUtf(msg.vehicleId == null ? "" : msg.vehicleId, 256);
    }

    public static C2S_CommanderSupport decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderSupport[] values = CommanderSupport.values();
        CommanderSupport support = ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new C2S_CommanderSupport(support, position, buf.readVarInt(), buf.readUtf(256));
    }

    public static void handle(C2S_CommanderSupport msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && CommanderStateManager.isCommander(player)) {
                Vec3 validated = msg.support.targeted()
                        ? MarkerManager.validatePosition(player, msg.position).orElse(null)
                        : player.position();
                CommanderSupportManager.use(player, msg.support, validated, msg.targetId, msg.vehicleId);
            }
        });
        context.get().setPacketHandled(true);
    }
}
