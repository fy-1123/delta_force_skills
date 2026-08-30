package mccfk.DeltaForce.java;

import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 模组数据初始化器
 * 服务器启动时检测世界文件夹/.MCCFK_ALL_MODDATA/deltaforce/是否存在
 * 不存在则从模组JAR内复制默认数据，存在则直接从文件读取
 */
public class DataInitializer {

    private static final String MOD_DATA_ROOT = "/.MCCFK_ALL_MODDATA/deltaforce";
    private static final String WORLD_DATA_ROOT = ".MCCFK_ALL_MODDATA/deltaforce";

    // 所有需创建的目录（相对于MOD_DATA_ROOT）
    private static final List<String> DIRS = List.of(
        "economy",
        "economy/delta_coin",
        "economy/havoc_coin",
        "operator",
        "operator/all_operator",
        "operator/player_data"
    );

    // 所有需复制的文件（相对于MOD_DATA_ROOT）
    private static final List<String> FILES = List.of(
        "economy/manager.json"
    );

    /**
     * 初始化世界数据
     * @param worldSavePath 世界存档根目录路径
     */
    public static void init(Path worldSavePath) {
        Path dataFolder = worldSavePath.resolve(WORLD_DATA_ROOT);

        if (Files.notExists(dataFolder)) {
            copyDefaultData(dataFolder);
        }

        // 无论复制还是已存在，都从文件加载经济配置
        loadEeconomyConfig(dataFolder);

        // 初始化 DF 仓库目录（世界/.MCCFK_ALL_MODDATA/deltaforce/仓库）
        DfWarehouseManager.init(worldSavePath);
    }

    private static void loadEeconomyConfig(Path dataFolder) {
        Path configPath = dataFolder.resolve("economy/manager.json");
        EeconomyManager.getInstance().loadConfig(configPath);

        // 若读取失败（文件被重命名为.no），从模组重新复制默认文件
        Path noPath = configPath.resolveSibling(configPath.getFileName() + ".no");
        if (Files.exists(noPath) && Files.notExists(configPath)) {
            copySingleFile("economy/manager.json", dataFolder);
            // 重新加载修复后的文件
            EeconomyManager.getInstance().loadConfig(configPath);
        }

        // 设置各货币的数据目录
        EeconomyManager.getInstance().setCurrencyDir(
            CurrencyType.HAVOC_COIN, dataFolder.resolve("economy/havoc_coin"));
        EeconomyManager.getInstance().setCurrencyDir(
            CurrencyType.DELTA_COIN, dataFolder.resolve("economy/delta_coin"));

        // 设置玩家角色数据目录
        PlayerCharacterStore.setPlayerDataDir(dataFolder.resolve("operator/player_data"));
    }

    private static void copySingleFile(String relativePath, Path dataFolder) {
        String resourcePath = MOD_DATA_ROOT + "/" + relativePath.replace("\\", "/");
        try (InputStream is = DataInitializer.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Path target = dataFolder.resolve(relativePath);
                Files.createDirectories(target.getParent());
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法复制模组默认文件: " + relativePath, e);
        }
    }

    private static void copyDefaultData(Path dataFolder) {
        try {
            // 创建所有目录
            for (String dir : DIRS) {
                Files.createDirectories(dataFolder.resolve(dir));
            }

            // 复制所有文件
            for (String file : FILES) {
                String resourcePath = MOD_DATA_ROOT + "/" + file.replace("\\", "/");
                try (InputStream is = DataInitializer.class.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        Path target = dataFolder.resolve(file);
                        Files.createDirectories(target.getParent());
                        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("无法复制模组默认数据到世界文件夹: " + dataFolder, e);
        }
    }
}