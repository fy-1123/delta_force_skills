package mccfk.DeltaForce.java.network;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientMarkerStorage {
    private static final Map<UUID, Marker> MARKERS = new ConcurrentHashMap<>();
    private static final long MARKER_LIFETIME_MS = 30_000L;

    private ClientMarkerStorage() {
    }

    public static void updateMarkers(Collection<Marker> markers) {
        MARKERS.clear();
        if (markers == null) {
            return;
        }
        for (Marker marker : markers) {
            if (marker != null && marker.owner() != null) {
                MARKERS.put(marker.owner(), marker);
            }
        }
    }

    public static Collection<Marker> getAllMarkers() {
        return MARKERS.values();
    }

    public static void clearAll() {
        MARKERS.clear();
    }

    public static void cleanExpired() {
        long now = System.currentTimeMillis();
        MARKERS.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().createTime();
            return age < 0L || age > MARKER_LIFETIME_MS;
        });
    }
}
