package mccfk.DeltaForce.java.network;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record C2S_CreateMarker(Marker marker) {
    public static void encode(C2S_CreateMarker message, FriendlyByteBuf buffer) {
        writeMarker(buffer, message.marker);
    }

    public static C2S_CreateMarker decode(FriendlyByteBuf buffer) {
        return new C2S_CreateMarker(readMarker(buffer));
    }

    public static void handle(C2S_CreateMarker message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            MarkerManager.validateRequest(player, message.marker).ifPresent(marker -> {
                if (MarkerManager.addMarker(player.getUUID(), marker)) {
                    NetworkHandler.sendToAll(new S2C_SyncMarkers(MarkerManager.getAllMarkers().stream().toList()));
                }
            });
        });
        context.setPacketHandled(true);
    }

    static void writeMarker(FriendlyByteBuf buffer, Marker marker) {
        buffer.writeUUID(marker.owner());
        buffer.writeBlockPos(marker.position());
        buffer.writeByte(marker.type().ordinal());
        buffer.writeLong(marker.createTime());
        buffer.writeBoolean(marker.entityType().isPresent());
        marker.entityType().ifPresent(type -> buffer.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(type)));
        buffer.writeBoolean(marker.containerPos().isPresent());
        marker.containerPos().ifPresent(buffer::writeBlockPos);
        buffer.writeBoolean(marker.precisePosition().isPresent());
        marker.precisePosition().ifPresent(value -> {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
        });
    }

    static Marker readMarker(FriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        BlockPos position = buffer.readBlockPos();
        int ordinal = buffer.readUnsignedByte();
        MarkerType[] values = MarkerType.values();
        if (ordinal >= values.length) {
            throw new DecoderException("Unknown marker type: " + ordinal);
        }
        long createTime = buffer.readLong();
        Optional<EntityType<?>> entityType = Optional.empty();
        if (buffer.readBoolean()) {
            ResourceLocation id = buffer.readResourceLocation();
            entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(value -> (EntityType<?>) value);
        }
        Optional<BlockPos> containerPos = buffer.readBoolean()
                ? Optional.of(buffer.readBlockPos()) : Optional.empty();
        Optional<Vec3> precisePosition = buffer.readBoolean()
                ? Optional.of(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()))
                : Optional.empty();
        return new Marker(owner, position, values[ordinal], createTime, entityType, containerPos, precisePosition);
    }
}
