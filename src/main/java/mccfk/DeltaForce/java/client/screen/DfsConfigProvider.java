package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.client.ClientHudLayout;
import com.rzy.dealt_force_skills.config.ConfigAccess;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import com.rzy.dealt_force_skills.config.ConfigFileId;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import com.rzy.dealt_force_skills.network.C2S_ApplyConfigChanges;
import com.rzy.dealt_force_skills.network.C2S_RequestConfigSnapshot;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Uses the core editor's server-authorized snapshot/apply protocol. */
final class DfsConfigProvider implements IConfigProvider {
    private final Map<String, ConfigEntryData> entries = new LinkedHashMap<>();
    private final Map<String, String> dirty = new LinkedHashMap<>();
    private final Map<String, String> submitted = new LinkedHashMap<>();
    private boolean canEditServer;
    private boolean awaitingSnapshot;
    private String status = "";

    DfsConfigProvider() {
        ClientHudLayout.snapshotEntries().forEach(this::putEntry);
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            canEditServer = true;
            ConfigAccess.snapshotAllServerFiles().forEach(this::putEntry);
        } else if (mc.player != null && mc.player.hasPermissions(2)) {
            awaitingSnapshot = true;
            status = Component.translatable("config.dealt_force_skills.status.loading_server").getString();
            NetworkHandler.sendToServer(new C2S_RequestConfigSnapshot());
        } else {
            status = Component.translatable("config.dealt_force_skills.status.hud_only").getString();
        }
    }

    private static String key(ConfigEntryData entry) {
        return entry.file().name() + "|" + entry.path();
    }

    private void putEntry(ConfigEntryData entry) {
        entries.put(key(entry), entry);
    }

    ConfigEntryData entry(String path) {
        return entries.get(path);
    }

    void acceptSnapshot(List<ConfigEntryData> snapshot, boolean allowed) {
        if (!awaitingSnapshot) return;
        awaitingSnapshot = false;
        canEditServer = allowed;
        entries.values().removeIf(entry -> !entry.file().clientLocal());
        if (allowed) snapshot.forEach(this::putEntry);
        status = Component.translatable(allowed
                ? "config.dealt_force_skills.status.server_op"
                : "config.dealt_force_skills.status.server_denied", snapshot.size()).getString();
    }

    void acceptApplyResult(boolean success, int count, String code) {
        if (submitted.isEmpty()) return;
        // A partial acknowledgement must leave the submitted values available to retry.
        if (success && count == submitted.size()) {
            submitted.forEach((key, value) -> {
                ConfigEntryData entry = entries.get(key);
                if (entry != null) {
                    entries.put(key, new ConfigEntryData(entry.file(), entry.path(), entry.kind(), value, entry.comment()));
                    dirty.remove(key, value);
                }
            });
            status = Component.translatable("config.dealt_force_skills.status.saved", count).getString();
        } else {
            status = Component.translatable("config.dealt_force_skills.status.save_failed",
                    code == null ? "partial" : code).getString();
        }
        submitted.clear();
    }

    @Override
    public List<ConfigEntry> getConfigEntries() {
        return entries.entrySet().stream().map(pair -> {
            ConfigEntryData entry = pair.getValue();
            return new ConfigEntry(pair.getKey(), entry.path(), getValue(pair.getKey()), entry.valueText(),
                    entry.comment(), ConfigValueKind.valueOf(entry.kind().name()),
                    ConfigAccess.categoryKey(entry), ConfigAccess.subcategoryKey(entry));
        }).toList();
    }

    @Override
    public String getValue(String path) {
        ConfigEntryData entry = entries.get(path);
        return dirty.getOrDefault(path, entry == null ? "" : entry.valueText());
    }

    @Override
    public boolean setValue(String path, String value) {
        ConfigEntryData entry = entries.get(path);
        if (entry == null || !submitted.isEmpty() || (!entry.file().clientLocal() && !canEditServer)) return false;
        if (entry.valueText().equals(value)) dirty.remove(path);
        else dirty.put(path, value);
        return true;
    }

    @Override
    public boolean validate() {
        if (!submitted.isEmpty() || awaitingSnapshot) return false;
        for (var change : dirty.entrySet()) {
            ConfigEntryData entry = entries.get(change.getKey());
            try {
                if (entry == null || change.getValue().length() > 1024) throw new IllegalArgumentException();
                ConfigEntryData.parse(entry.kind(), change.getValue());
            } catch (RuntimeException error) {
                status = Component.translatable("config.dealt_force_skills.status.invalid",
                        entry == null ? change.getKey() : entry.path()).getString();
                return false;
            }
        }
        if (dirty.keySet().stream().filter(key -> !entries.get(key).file().clientLocal()).count() > 512) {
            status = "Too many server changes (maximum 512).";
            return false;
        }
        return true;
    }

    @Override
    public void save() {
        if (!validate()) return;
        List<C2S_ApplyConfigChanges.Change> changes = new ArrayList<>();
        List<String> saved = new ArrayList<>();
        boolean inWorld = Minecraft.getInstance().getConnection() != null;
        try {
            for (var change : dirty.entrySet()) {
                ConfigEntryData entry = entries.get(change.getKey());
                String value = change.getValue();
                if (entry.file().clientLocal()) {
                    if (ClientHudLayout.setValue(entry.path(), ConfigEntryData.parse(entry.kind(), value))) {
                        saved.add(change.getKey());
                    }
                } else if (canEditServer && inWorld) {
                    changes.add(C2S_ApplyConfigChanges.Change.from(entry, value));
                    submitted.put(change.getKey(), value);
                } else if (canEditServer && ConfigAccess.applyChange(entry.file(), entry.path(), entry.kind(), value)) {
                    ConfigAccess.flush(entry.file());
                    saved.add(change.getKey());
                }
            }
            if (!inWorld && !saved.isEmpty()) ConfigHotReload.reloadAll();
            for (String key : saved) {
                ConfigEntryData entry = entries.get(key);
                entries.put(key, new ConfigEntryData(entry.file(), entry.path(), entry.kind(), dirty.remove(key), entry.comment()));
            }
            if (!changes.isEmpty()) {
                status = Component.translatable("config.dealt_force_skills.status.uploading", changes.size()).getString();
                NetworkHandler.sendToServer(new C2S_ApplyConfigChanges(changes));
            } else {
                status = Component.translatable("config.dealt_force_skills.status.saved_local", saved.size()).getString();
            }
        } catch (RuntimeException error) {
            submitted.clear();
            status = Component.translatable("config.dealt_force_skills.status.save_failed", error.getMessage()).getString();
        }
    }

    @Override public void cancel() { if (submitted.isEmpty()) dirty.clear(); }
    @Override public boolean isDirty() { return !dirty.isEmpty(); }
    @Override public boolean isBusy() { return !submitted.isEmpty(); }
    @Override public String status() { return status; }
}
