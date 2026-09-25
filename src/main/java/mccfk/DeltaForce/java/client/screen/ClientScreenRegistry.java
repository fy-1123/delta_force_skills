package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端HUD注册中心 - 仅注册可编辑位置的HUD叠加层
 * 不注册Screen界面（Screen自己渲染自己，编辑器无法控制）
 */
public final class ClientScreenRegistry {

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private ClientScreenRegistry() {}

    /** 注册一个HUD叠加层条目 */
    public static void register(String id, Component displayName,
            Class<?> screenClass, List<LayoutEntry> defaultElements) {
        ENTRIES.putIfAbsent(id, new Entry(id, displayName, screenClass,
                defaultElements));
    }

    /** 获取全部已注册条目（不可变） */
    public static List<Entry> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));
    }

    /** HUD条目 */
    public record Entry(String id, Component displayName,
            Class<?> screenClass,
            List<LayoutEntry> defaultElements) {
        public boolean hasScreenClass() {
            return screenClass != null
                    && Screen.class.isAssignableFrom(screenClass);
        }
    }
}