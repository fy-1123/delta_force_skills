package mccfk.DeltaForce.java.client;

import com.rzy.dealt_force_skills.config.ConfigEntryData;
import mccfk.DeltaForce.java.client.screen.IConfigProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-local values are validated together before any value is written. */
public final class CustomDropConfigProvider implements IConfigProvider {
    private static final String PREFIX = "customdrop.hud.";
    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("enabled", true);
        DEFAULTS.put("scale", 0.0D);
        DEFAULTS.put("x_offset", 0);
        DEFAULTS.put("y_offset", 0);
        DEFAULTS.put("simple_mode", false);
        DEFAULTS.put("hotkey_detailed", false);
        DEFAULTS.put("hotkey", "73");
    }
    private final Map<String, String> saved = new LinkedHashMap<>();
    private final Map<String, String> dirty = new LinkedHashMap<>();
    private String status = "";

    public CustomDropConfigProvider() {
        CustomDropConfig.bootstrap();
        DEFAULTS.forEach((name, value) -> {
            Object current = CustomDropConfig.rawConfig().get(PREFIX + name);
            saved.put(PREFIX + name, String.valueOf(current == null ? value : current));
        });
    }

    private static ConfigValueKind kind(String path) {
        Object value = DEFAULTS.get(path.substring(PREFIX.length()));
        if (value instanceof Boolean) return ConfigValueKind.BOOLEAN;
        if (value instanceof Integer) return ConfigValueKind.INTEGER;
        if (value instanceof Double) return ConfigValueKind.DOUBLE;
        return ConfigValueKind.STRING;
    }

    @Override public List<ConfigEntry> getConfigEntries() {
        return saved.keySet().stream().map(path -> {
            String parent = path.endsWith(".enabled") ? null
                    : path.endsWith(".hotkey") ? PREFIX + "hotkey_detailed"
                    : path.endsWith(".hotkey_detailed") ? PREFIX + "simple_mode" : PREFIX + "enabled";
            return new ConfigEntry(path, path, getValue(path), saved.get(path), "", kind(path),
                    "customdrop", "hud", parent == null ? 0 : 1, parent);
        }).toList();
    }
    @Override public String getValue(String path) { return dirty.getOrDefault(path, saved.getOrDefault(path, "")); }
    @Override public boolean setValue(String path, String value) {
        if (!saved.containsKey(path)) return false;
        if (saved.get(path).equals(value)) dirty.remove(path);
        else dirty.put(path, value);
        return true;
    }
    private Object parse(String path, String value) {
        if (path.endsWith(".hotkey")) {
            if ("-1".equals(value)) return value;
            String[] keys = value.split("\\+", -1);
            if (keys.length == 0 || keys.length > 5) throw new IllegalArgumentException();
            for (String key : keys) {
                int code = Integer.parseInt(key.trim());
                if (code < 32 || code > 348) throw new IllegalArgumentException();
            }
            return value;
        }
        Object parsed = ConfigEntryData.parse(com.rzy.dealt_force_skills.config.ConfigValueKind.valueOf(kind(path).name()), value);
        if (path.endsWith(".scale") && ((double) parsed < -0.9 || (double) parsed > 1.0)) throw new IllegalArgumentException();
        if (path.endsWith("_offset") && Math.abs((long) (int) parsed) > 10000) throw new IllegalArgumentException();
        return parsed;
    }
    @Override public boolean validate() {
        for (var change : dirty.entrySet()) {
            try { parse(change.getKey(), change.getValue()); }
            catch (RuntimeException error) {
                status = net.minecraft.network.chat.Component.translatable(
                        "config.dealt_force_skills.status.invalid", change.getKey()).getString();
                return false;
            }
        }
        return true;
    }
    @Override public void save() {
        if (!validate()) return;
        var config = CustomDropConfig.rawConfig();
        Map<String, Object> before = new LinkedHashMap<>();
        try {
            dirty.forEach((path, value) -> {
                before.put(path, config.get(path));
                config.set(path, parse(path, value));
            });
            CustomDropConfig.markDirty();
            CustomDropConfig.flush();
            saved.putAll(dirty);
            dirty.clear();
            status = "";
        } catch (RuntimeException error) {
            before.forEach(config::set);
            status = net.minecraft.network.chat.Component.translatable(
                    "config.dealt_force_skills.status.save_failed", error.getMessage()).getString();
        }
    }
    @Override public void cancel() { dirty.clear(); status = ""; }
    @Override public boolean isDirty() { return !dirty.isEmpty(); }
    @Override public String status() { return status; }
}
