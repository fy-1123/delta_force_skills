package mccfk.DeltaForce.java.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 玩家角色档案存储（统一存储 friend 模组角色选择）
 * <p>
 * 干员定义已统一合并到 friend 模组角色目录（ModCharacters），本类只负责
 * 按玩家名.json 的读写，格式: {"id": "&lt;friend 模组角色id&gt;"}
 * 目录: world/.MCCFK_ALL_MODDATA/deltaforce/operator/player_data/
 */
public final class PlayerCharacterStore {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ID_KEY = "id";
    private static final String LEGACY_ID_KEY = "character_id";

    // 玩家角色数据目录（world/.MCCFK_ALL_MODDATA/deltaforce/operator/player_data/）
    private static Path playerDataDir;

    private PlayerCharacterStore() {
    }

    /**
     * 设置玩家角色数据目录（由 DataInitializer 在服务器启动时调用）
     */
    public static void setPlayerDataDir(Path dir) {
        playerDataDir = dir;
    }

    /**
     * 确保数据目录存在（登录时调用，不再创建默认干员，角色选择由 friend 模组流程管理）
     */
    public static void init() {
        if (playerDataDir == null) {
            return;
        }
        try {
            Files.createDirectories(playerDataDir);
        } catch (IOException e) {
            LOGGER.error("[DF] 无法创建玩家角色数据目录: {}", playerDataDir);
        }
    }

    private static boolean hasData(String playerName) {
        return playerDataDir != null && Files.exists(playerDataDir.resolve(playerName + ".json"));
    }

    private static JsonObject load(String playerName) {
        if (!hasData(playerName)) {
            return null;
        }
        Path file = playerDataDir.resolve(playerName + ".json");
        try (Reader reader = Files.newBufferedReader(file)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            LOGGER.warn("[DF] 读取玩家角色数据失败: {}/{}.json", playerDataDir, playerName);
        }
        return null;
    }

    private static void save(String playerName, String characterId) {
        if (playerDataDir == null) {
            return;
        }
        try {
            Files.createDirectories(playerDataDir);
            Path file = playerDataDir.resolve(playerName + ".json");
            JsonObject obj = new JsonObject();
            obj.addProperty(ID_KEY, characterId == null ? "" : characterId);
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[DF] 无法保存玩家角色数据: {}/{}.json", playerDataDir, playerName);
        }
    }

    /**
     * 获取指定玩家的已选角色ID（friend 模组角色 id）
     * 无角色选择或文件缺失时返回 null
     */
    public static String getSelectedCharacterId(String playerName) {
        JsonObject obj = load(playerName);
        if (obj == null) {
            return null;
        }
        String key = obj.has(ID_KEY) ? ID_KEY : LEGACY_ID_KEY;
        if (!obj.has(key)) {
            return null;
        }
        String id = obj.get(key).getAsString();
        return id == null || id.isBlank() ? null : id;
    }

    /**
     * 设置指定玩家的角色选择
     *
     * @param playerName  玩家名
     * @param characterId friend 模组角色 id（空串表示清除）
     * @return 是否成功（数据目录未初始化时失败）
     */
    public static boolean setSelectedCharacter(String playerName, String characterId) {
        if (playerDataDir == null) {
            return false;
        }
        save(playerName, characterId);
        return true;
    }

    /**
     * 清除指定玩家的角色选择（写入空 id）
     *
     * @return 是否成功（数据目录未初始化时失败）
     */
    public static boolean clearSelectedCharacter(String playerName) {
        if (playerDataDir == null) {
            return false;
        }
        save(playerName, "");
        return true;
    }

    /**
     * 从 player_data 目录中收集已有玩家名（存在角色数据的玩家）
     */
    public static java.util.List<String> getPlayerNamesFromFiles() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        if (playerDataDir == null || !Files.exists(playerDataDir)) {
            return java.util.List.copyOf(names);
        }
        try (Stream<Path> files = Files.list(playerDataDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .forEach(names::add);
        } catch (IOException ignored) {
        }
        return java.util.List.copyOf(names);
    }

    // ========== 测试变量（%operatortype% / %operatorname%，供 /df test 调用）==========

    /**
     * 获取当前玩家的角色类型（无传入时显示自身角色类型）
     */
    public static Component getOperatorType() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Component.translatable("deltaforce.operator.no_operator", "");
        }
        return getOperatorType(player.getName().getString());
    }

    /**
     * 获取指定玩家的角色类型（friend 模组角色 role 的本地化）
     */
    public static Component getOperatorType(String playerName) {
        CharacterDefinition def = findCharacter(playerName);
        if (def == null) {
            return Component.translatable("deltaforce.operator.no_operator", playerName);
        }
        return Component.translatable(def.role().translationKey());
    }

    /**
     * 获取当前玩家的角色名（无传入时显示自身角色名字）
     */
    public static Component getOperatorName() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Component.translatable("deltaforce.operator.no_operator", "");
        }
        return getOperatorName(player.getName().getString());
    }

    /**
     * 获取指定玩家的角色名（friend 模组角色显示名）
     */
    public static Component getOperatorName(String playerName) {
        CharacterDefinition def = findCharacter(playerName);
        if (def == null) {
            return Component.translatable("deltaforce.operator.no_operator", playerName);
        }
        return def.displayName();
    }

    private static CharacterDefinition findCharacter(String playerName) {
        String id = getSelectedCharacterId(playerName);
        return id == null ? null : CharacterBranchPackManager.findCharacter(id).orElse(null);
    }
}
