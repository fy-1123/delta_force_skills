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

public final class DFClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get(".MCCFK_ALL_MODCONFIG", "deltaforce", "deltaforce-client.json");

    private static DFClientConfig instance;

    @SerializedName("tab_x_offset")
    private int tabXOffset = 0;

    @SerializedName("tab_y_offset")
    private int tabYOffset = 0;

    @SerializedName("layout_custom_enabled")
    private boolean layoutCustomEnabled = false;

    @SerializedName("df_main_enabled")
    private boolean dfMainEnabled = false;

    @SerializedName("admin_auth_enabled")
    private boolean adminAuthEnabled = true;

    private DFClientConfig() {
    }

    public static DFClientConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int getTabXOffset() {
        return tabXOffset;
    }

    public void setTabXOffset(int tabXOffset) {
        this.tabXOffset = tabXOffset;
    }

    public int getTabYOffset() {
        return tabYOffset;
    }

    public void setTabYOffset(int tabYOffset) {
        this.tabYOffset = tabYOffset;
    }

    public boolean isLayoutCustomEnabled() {
        return layoutCustomEnabled;
    }

    public void setLayoutCustomEnabled(boolean layoutCustomEnabled) {
        this.layoutCustomEnabled = layoutCustomEnabled;
    }

    public boolean isDfMainEnabled() {
        return dfMainEnabled;
    }

    public void setDfMainEnabled(boolean dfMainEnabled) {
        this.dfMainEnabled = dfMainEnabled;
    }

    /** 客户端管理员验证开关（默认开启，关闭后不弹窗不发送管理员凭据） */
    public boolean isAdminAuthEnabled() {
        return adminAuthEnabled;
    }

    public void setAdminAuthEnabled(boolean adminAuthEnabled) {
        this.adminAuthEnabled = adminAuthEnabled;
    }

    public static DFClientConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DFClientConfig cfg = GSON.fromJson(reader, DFClientConfig.class);
                if (cfg != null) {
                    instance = cfg;
                    System.out.println("[DFConfig] 配置已加载: dfMain=" + cfg.isDfMainEnabled() + " adminAuth=" + cfg.isAdminAuthEnabled() + " 路径=" + CONFIG_PATH.toAbsolutePath());
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[DFClientConfig] 加载配置失败: " + e.getMessage());
            }
        }
        System.out.println("[DFConfig] 配置不存在或无效，创建默认配置(dfMain=false) 路径=" + CONFIG_PATH.toAbsolutePath());
        DFClientConfig cfg = new DFClientConfig();
        cfg.save();
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
            System.err.println("[DFClientConfig] 保存配置失败: " + e.getMessage());
        }
    }
}