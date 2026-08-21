package mccfk.DeltaForce.java.client.overlay;

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
 * 物品介绍加载器 - 从descriptions文件夹读取txt文件
 * 文件格式:
 * 第一行: .tag("#deltaforce:df_age_X") (可选,设置标签)
 * 后续行: 介绍文本(支持\n等转义字符)
 */
public class ItemDescriptionLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemDescriptionLoader.class);
    private static final Map<ResourceLocation, String> descriptionCache = new HashMap<>();
    private static final Map<ResourceLocation, Integer> tagCache = new HashMap<>();
    private static final Set<ResourceLocation> missingCache = new HashSet<>();
    private static final Set<ResourceLocation> failedCache = new HashSet<>();

    /**
     * friend 模组（dealt_force_skills）酒 → 我们的酒名映射。
     * 原 friend 酒的 id 与我们的酒 id 不一致时映射到我们的酒名，
     * 使 friend 酒在掉落物 HUD 上读取 deltaforce:descriptions/<我们的酒名>.txt 显示我们的介绍。
     * 同名酒（tulip/mountain_rain/highball/wild_rose/sky_island/heartbeat）无需映射。
     */
    private static final Map<String, String> WINE_ALIAS = Map.of(
            "dealt_force_skills:reis_dance", "leisurely_dance",
            "dealt_force_skills:asara_style", "asara_charm",
            "dealt_force_skills:starlight_glitter", "twinkling_starlight",
            "dealt_force_skills:long_goodbye", "long_farewell",
            "dealt_force_skills:guanshan_nanyue", "mountain_crossing",
            "dealt_force_skills:mist_morning", "misty_morning"
    );

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
            descriptionCache.put(itemId, content.description);
            if (content.tagAge != null) {
                tagCache.put(itemId, content.tagAge);
            }
            return content.description;
        }

        missingCache.add(itemId);
        return null;
    }

    /**
     * 获取物品的标签等级
     */
    public static synchronized Integer getTagAge(ItemStack itemStack) {
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

        return tagCache.get(itemId);
    }

    /**
     * 从文件加载介绍
     */
    private static FileContent loadDescriptionFromFile(ResourceLocation itemId) {
        try {
            Minecraft mc = Minecraft.getInstance();

            // friend 模组酒：映射到我们的酒名，读取我们的介绍文本
            String itemName = WINE_ALIAS.getOrDefault(itemId.toString(), itemId.getPath());

            String resourcePath = "descriptions/" + itemName + ".txt";

            var resourceManager = mc.getResourceManager();
            var resourceOpt = resourceManager.getResource(
                ResourceLocation.fromNamespaceAndPath("deltaforce", resourcePath)
            );

            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {

                    StringBuilder content = new StringBuilder();
                    Integer tagAge = null;
                    boolean firstLine = true;
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                            if (line.startsWith(".tag(")) {
                                tagAge = parseTagAge(line);
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

                    return new FileContent(result, tagAge);
                }
            }
        } catch (Exception error) {
            if (failedCache.add(itemId)) {
                LOGGER.warn("Unable to load item description for {}", itemId, error);
            }
        }

        return null;
    }

    /**
     * 解析标签年龄等级
     */
    private static Integer parseTagAge(String tagLine) {
        try {
            int startIndex = tagLine.indexOf("df_age_");
            if (startIndex == -1) {
                return null;
            }

            startIndex += 7;
            int endIndex = tagLine.indexOf('"', startIndex);
            if (endIndex == -1) {
                endIndex = tagLine.indexOf(')', startIndex);
            }

            if (endIndex == -1) {
                return null;
            }

            String ageStr = tagLine.substring(startIndex, endIndex).trim();
            return Integer.parseInt(ageStr);
        } catch (RuntimeException error) {
            LOGGER.warn("Ignoring invalid item-description tag {}", tagLine);
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
        tagCache.clear();
        missingCache.clear();
        failedCache.clear();
    }

    /**
     * 文件内容类
     */
    private static class FileContent {
        String description;
        Integer tagAge;

        FileContent(String description, Integer tagAge) {
            this.description = description;
            this.tagAge = tagAge;
        }
    }
}
