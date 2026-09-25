package mccfk.DeltaForce.java.client.overlay;

import com.rzy.dealt_force_skills.item.DfsItemQuality;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 物品介绍加载器 - 从物品自身模组命名空间的descriptions文件夹读取txt文件
 * 文件格式:
 * 第一行: .level(X); (可选,设置品质等级0-8)
 * 后续行: 介绍文本(支持\n等转义字符)
 */
public class ItemDescriptionLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemDescriptionLoader.class);
    private static final Map<ResourceLocation, String> descriptionCache = new HashMap<>();
    private static final Map<ResourceLocation, Integer> levelCache = new HashMap<>();
    private static final Set<ResourceLocation> missingCache = new HashSet<>();
    private static final Set<ResourceLocation> failedCache = new HashSet<>();
    private static final int MAX_CACHE_SIZE = 2000;

    private static void safePutCache() {
        if (descriptionCache.size() > MAX_CACHE_SIZE) {
            descriptionCache.clear();
            levelCache.clear();
            missingCache.clear();
            failedCache.clear();
        }
    }

    /**
     * 获取物品的介绍文本
     */
    public static synchronized String getDescription(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            return null;
        }

        if (descriptionCache.containsKey(itemId)) {
            return descriptionCache.get(itemId);
        }
        if (missingCache.contains(itemId)) {
            return null;
        }

        FileContent content = loadDescriptionFromFile(itemId);

        if (content != null) {
            safePutCache();
            descriptionCache.put(itemId, content.description);
            if (content.level != null) {
                levelCache.put(itemId, content.level);
                DfsItemQuality.applyTagQualityOverride(itemId, content.level);
            }
            return content.description;
        }

        missingCache.add(itemId);
        return null;
    }

    /**
     * 检查物品是否有描述文件（不加载内容）
     */
    public static synchronized boolean hasDescriptionFile(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            return false;
        }

        if (descriptionCache.containsKey(itemId)) {
            return true;
        }
        if (missingCache.contains(itemId)) {
            return false;
        }

        getDescription(itemStack);
        return descriptionCache.containsKey(itemId);
    }

    /**
     * 获取物品的品质等级
     */
    public static synchronized Integer getLevel(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            return null;
        }

        if (!descriptionCache.containsKey(itemId) && !missingCache.contains(itemId)) {
            getDescription(itemStack);
        }

        return levelCache.get(itemId);
    }

    /**
     * 从文件加载介绍
     */
    private static FileContent loadDescriptionFromFile(ResourceLocation itemId) {
        try {
            Minecraft mc = Minecraft.getInstance();

            String resourcePath = "descriptions/" + itemId.getPath() + ".txt";

            var resourceManager = mc.getResourceManager();
            var resourceOpt = resourceManager.getResource(
                    ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), resourcePath)
            );

            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {

                    StringBuilder content = new StringBuilder();
                    Integer level = null;
                    boolean firstLine = true;
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                            if (line.startsWith("\uFEFF")) {
                                line = line.substring(1);
                            }
                            if (line.startsWith(".level(")) {
                                level = parseLevel(line);
                                continue;
                            }
                        }

                        if (content.length() > 0) {
                            content.append("\n");
                        }
                        content.append(processEscapeCharacters(line));
                    }

                    String result = content.toString();
                    if (result.isEmpty()) {
                        return null;
                    }

                    return new FileContent(result, level);
                }
            }
        } catch (Exception error) {
            if (failedCache.add(itemId)) {
                if (failedCache.size() > MAX_CACHE_SIZE) {
                    failedCache.clear();
                }
                LOGGER.warn("Unable to load item description for {}", itemId, error);
            }
        }

        return null;
    }

    /**
     * 解析品质等级
     * 格式: .level(0-8);
     */
    private static Integer parseLevel(String line) {
        try {
            int startIndex = line.indexOf(".level(");
            if (startIndex == -1) {
                return null;
            }

            startIndex += 7;
            int endIndex = line.indexOf(')', startIndex);
            if (endIndex == -1) {
                return null;
            }

            String levelStr = line.substring(startIndex, endIndex).trim();
            return Integer.parseInt(levelStr);
        } catch (RuntimeException error) {
            LOGGER.warn("Ignoring invalid item-description level line {}", line);
            return null;
        }
    }

    /**
     * 处理转义字符
     */
    private static String processEscapeCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case '\\':
                        result.append('\\');
                        break;
                    case '"':
                        result.append('"');
                        break;
                    default:
                        result.append('\\').append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }

    /**
     * 清空缓存
     */
    public static synchronized void clearCache() {
        descriptionCache.clear();
        levelCache.clear();
        missingCache.clear();
        failedCache.clear();
    }

    /**
     * 文件内容类
     */
    private static class FileContent {
        String description;
        Integer level;

        FileContent(String description, Integer level) {
            this.description = description;
            this.level = level;
        }
    }
}