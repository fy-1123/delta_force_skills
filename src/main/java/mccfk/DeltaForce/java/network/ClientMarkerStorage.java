package mccfk.DeltaForce.java.network;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端标点存储 - 接收服务端同步的标点数据
 * 所有客户端可见同一套标点
 */
public class ClientMarkerStorage {
    private static final Map<UUID, Marker> MARKERS = new ConcurrentHashMap<>();
    private static final long MARKER_LIFETIME = 30000; // 30秒

    /**
     * 添加单个标点（纯客户端模式）
     */
    public static void addMarker(Marker marker) {
        MARKERS.put(marker.owner(), marker);
    }

    /**
     * 更新所有标点（从服务端同步）
     */
    public static void updateMarkers(Collection<Marker> markers) {
        MARKERS.clear();
        for (Marker marker : markers) {
            if (marker != null && marker.owner() != null) {
                MARKERS.put(marker.owner(), marker);
            }
        }
    }

    /**
     * 获取所有标点用于渲染
     */
    public static Collection<Marker> getAllMarkers() {
        return MARKERS.values();
    }

    /**
     * 清空所有标点（断开连接时调用）
     */
    public static void clearAll() {
        MARKERS.clear();
    }

    /**
     * 清理过期标点
     */
    public static void cleanExpired() {
        long now = System.currentTimeMillis();
        MARKERS.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().createTime();
            return age < 0 || age > MARKER_LIFETIME;
        });
    }
}
