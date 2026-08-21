package mccfk.DeltaForce.java.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record S2C_SyncMarkers(List<Marker> markers) {
    public static final int MAX_MARKERS = 1024;

    public S2C_SyncMarkers(Collection<Marker> markers) {
        this(markers == null ? List.of() : List.copyOf(markers));
    }

    public S2C_SyncMarkers {
        markers = markers == null ? List.of() : List.copyOf(markers);
        if (markers.size() > MAX_MARKERS) {
            throw new IllegalArgumentException("Too many markers: " + markers.size());
        }
    }

    public static void encode(S2C_SyncMarkers message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.markers.size());
        message.markers.forEach(marker -> C2S_CreateMarker.writeMarker(buffer, marker));
    }

    public static S2C_SyncMarkers decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_MARKERS) {
            throw new DecoderException("Invalid marker count: " + count);
        }
        List<Marker> markers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            markers.add(C2S_CreateMarker.readMarker(buffer));
        }
        return new S2C_SyncMarkers(markers);
    }

    public static void handle(S2C_SyncMarkers message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientMarkerStorage.updateMarkers(message.markers));
        context.setPacketHandled(true);
    }
}
