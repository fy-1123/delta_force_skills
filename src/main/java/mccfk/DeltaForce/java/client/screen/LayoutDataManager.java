package mccfk.DeltaForce.java.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理布局配置数据的读写
 * 存储路径：.MCCFK_ALL_MODDATA/deltaforce/LCS/{modId}/{screenName}.json
 */
public final class LayoutDataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LCS_ROOT = Paths.get(".MCCFK_ALL_MODDATA", "deltaforce", "LCS");

    /** 从配置项 entry id 中提取 modId */
    public static String extractModId(String entryId) {
        if (entryId.startsWith("container.")) {
            String inner = entryId.substring("container.".length());
            int dot = inner.indexOf('.');
            return dot > 0 ? inner.substring(0, dot) : "unknown";
        }
        int dot = entryId.indexOf('.');
        if (dot > 0 && entryId.contains("/")) {
            return entryId.substring(0, entryId.indexOf('/'));
        }
        return dot > 0 ? entryId.substring(0, dot) : "unknown";
    }

    /** 从配置项 entry id 中提取界面名 */
    public static String extractScreenName(String entryId) {
        if (entryId.startsWith("container.")) {
            String inner = entryId.substring("container.".length());
            int dot = inner.indexOf('.');
            return dot > 0 ? inner.substring(dot + 1) : inner;
        }
        if (entryId.contains("/")) {
            return entryId.substring(entryId.lastIndexOf('/') + 1);
        }
        int dot = entryId.indexOf('.');
        return dot > 0 ? entryId.substring(dot + 1) : entryId;
    }

    /** 获取某个界面的布局配置文件路径 */
    public static Path getLayoutPath(String modId, String screenName) {
        Path dir = LCS_ROOT.resolve(modId);
        return dir.resolve(sanitizeFileName(screenName) + ".json");
    }

    /** 加载布局数据，不存在则返回空容器 */
    public static LayoutContainer load(String modId, String screenName) {
        Path path = getLayoutPath(modId, screenName);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                LayoutContainer container = GSON.fromJson(reader, LayoutContainer.class);
                if (container != null) {
                    return container;
                }
            } catch (IOException e) {
                System.err.println("[LayoutDataManager] 加载布局失败 " + path + ": " + e.getMessage());
            }
        }
        return new LayoutContainer();
    }

    /** 保存布局数据 */
    public static void save(String modId, String screenName, LayoutContainer container) {
        Path path = getLayoutPath(modId, screenName);
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("[LayoutDataManager] 创建目录失败 " + path + ": " + e.getMessage());
            return;
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(container, writer);
        } catch (IOException e) {
            System.err.println("[LayoutDataManager] 保存布局失败 " + path + ": " + e.getMessage());
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    /**
     * 加载单个元素的布局数据，不存在则返回默认值。
     * 供游戏内渲染调用，实现编辑器中改的位置/缩放真实生效。
     */
    public static LayoutEntry loadElement(String modId, String screenName,
            String elementId, float defaultRelX, float defaultRelY,
            float defaultRelW, float defaultRelH) {
        LayoutContainer container = load(modId, screenName);
        LayoutEntry e = container.elements.get(elementId);
        if (e == null || !e.enabled) {
            e = new LayoutEntry();
            e.id = elementId;
            e.anchorX = 0.5f;
            e.anchorY = 0.5f;
            e.offsetX = defaultRelX - 0.5f;
            e.offsetY = defaultRelY - 0.5f;
            e.relWidth = defaultRelW;
            e.relHeight = defaultRelH;
            e.scaleX = 1.0f;
            e.scaleY = 1.0f;
            e.enabled = true;
        }
        return e;
    }

    /** 布局容器，存储一个界面下所有元素的布局 */
    public static class LayoutContainer {
        @SerializedName("version")
        public int version = 1;

        @SerializedName("elements")
        public Map<String, LayoutEntry> elements = new LinkedHashMap<>();

        public LayoutEntry getOrCreate(String id, String displayName) {
            return elements.computeIfAbsent(id, k -> {
                LayoutEntry e = new LayoutEntry();
                e.id = id;
                e.displayName = displayName;
                e.anchorX = 0.5f;
                e.anchorY = 0.5f;
                e.relWidth = 0.15f;
                e.relHeight = 0.12f;
                return e;
            });
        }
    }
}