package mccfk.DeltaForce.java.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * DFGAME 密码验证配置
 * 记录密码和尝试次数，配置文件存储在 .MCCFK_ALL_MODCONFIG/dfgame-auth.json
 * 密码正确后不再要求输入；配置文件被删除后可以重新输入
 */
public final class DFGameAuthConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get(".MCCFK_ALL_MODCONFIG", "dfgame-auth.json");

    private static final String PASSWORD = "fdaa2ddc00e66e4c7d1f9ed426b4f8e176aad15c17029648b86eba40ae20afbb";

    private static DFGameAuthConfig instance;

    @SerializedName("password")
    private String password = PASSWORD;

    @SerializedName("attempts")
    private int attempts = 0;

    private transient boolean verified = false;

    private DFGameAuthConfig() {
    }

    public static DFGameAuthConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * 是否已验证通过
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * 是否已被永久锁定（尝试次数大于0）
     */
    public boolean isLocked() {
        return !verified && attempts > 0;
    }

    /**
     * 获取尝试次数
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * 尝试验证密码
     * @param input 用户输入的密码
     * @return true 表示密码正确且验证通过
     */
    public boolean tryPassword(String input) {
        if (verified) {
            return true;
        }
        if (attempts > 0) {
            return false;
        }

        if (PASSWORD.equals(input)) {
            verified = true;
            attempts = 0;
            save();
            return true;
        }

        attempts++;
        save();
        return false;
    }

    /**
     * 重置验证状态（配置文件被删除后调用）
     */
    public void reset() {
        verified = false;
        attempts = 0;
        password = PASSWORD;
        instance = new DFGameAuthConfig();
        save();
    }

    private static DFGameAuthConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DFGameAuthConfig cfg = GSON.fromJson(reader, DFGameAuthConfig.class);
                if (cfg != null) {
                    if (PASSWORD.equals(cfg.password)) {
                        cfg.verified = true;
                    }
                    instance = cfg;
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[DFGameAuth] 加载验证配置失败: " + e.getMessage());
            }
        }
        DFGameAuthConfig cfg = new DFGameAuthConfig();
        instance = cfg;
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(this, writer);
            }
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[DFGameAuth] 保存验证配置失败: " + e.getMessage());
        }
    }
}