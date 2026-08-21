package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.client.character.ClientCommanderVehicleState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record S2C_CommanderVehicleState(int entityId, UUID entityUuid, boolean active) {
    public static void encode(S2C_CommanderVehicleState message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.entityUuid);
        buffer.writeBoolean(message.active);
    }

    public static S2C_CommanderVehicleState decode(FriendlyByteBuf buffer) {
        return new S2C_CommanderVehicleState(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(S2C_CommanderVehicleState message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCommanderVehicleState.sync(
                        message.entityId, message.entityUuid, message.active)));
        contextSupplier.get().setPacketHandled(true);
    }
}
