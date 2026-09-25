package mccfk.DeltaForce.java;

import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.S2C_SyncMarkers;
import mccfk.DeltaForce.java.network.DeltaForceNetwork;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.client.screen.IConfigProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerPlayer;

import mccfk.DeltaForce.java.item.DeltaForceItems;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.command.DFCommand;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;
import mccfk.DeltaForce.java.dfgame.DFGameManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 主类
 * 单例模式，统一管理所有DF（三角洲行动）相关物品、实体和事件
 * 数据持久化路径：世界文件夹/.mccfk_alldata/DeltaForce/
 *
 * 公共API：
 * - DeltaForce.getInstance()         获取单例
 */
@Mod(DeltaForce.MODID)
public class DeltaForce {

    public static final String MODID = "deltaforce";

    private static DeltaForce instance;

    // 玩家游戏状态（由 /df game t/f 控制）
    private static boolean dfGameEnabled = false;

    // DFGAME 功能开关：默认全部关闭，仅可通过指令开启
    // 禁用全部界面：开启后除白名单（聊天/暂停/死亡/本模组界面等）外，所有界面强制关闭
    private static boolean dfLockAllScreens = false;
    // 使用自定义背包：背包打开时替换为自定义背包界面
    private static boolean dfCustomInventory = false;

    // 三角洲主界面快捷键：默认关闭，开启后按U键打开三角洲主界面
    // volatile：服务端命令线程写入，客户端渲染线程读取，保证跨线程可见性
    private static volatile boolean dfMainEnabled = false;

    // 调试模式：默认关闭，开启后输出详细日志（如翻译缺失警告）
    private static boolean debugMode = false;

    // 管理员调试总开关：默认关闭，仅服务端可通过 /df debug admin on 开启
    // 开启后无敌/无限体力/技能无冷却等调试功能才实际生效
    private static boolean allowAdminDebug = false;

    // 设置界面白名单：df 和 dfs 为永久白名单，其他模组在主类中标注即可自动识别
    // 标注方式：DeltaForce.registerConfigurableMod(MODID) 或 InterModComms.sendTo("deltaforce", "register_configurable", () -> MODID)
    private static final Set<String> configWhitelist = new HashSet<>(Arrays.asList(
        "deltaforce",
        "dealt_force_skills"
    ));

    // 配置提供者注册表：modId -> IConfigProvider
    private static final Map<String, IConfigProvider> configProviders = new HashMap<>();

    public DeltaForce() {
        instance = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册 IMC 侦听：接收其他模组的"注册可配置"消息
        modEventBus.addListener(this::onInterModEnqueue);

        // 注册DF物品
        DeltaForceItems.register(modEventBus);

        // 注册DF实体
        DeltaForceEntities.ENTITY_TYPES.register(modEventBus);

        // 注册服务端事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册管理员调试事件
        MinecraftForge.EVENT_BUS.register(new AdminDebugManager());

        // 客户端专用初始化（通过反射调用，避免服务端类加载触发 RuntimeDistCleaner）
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            try {
                Class<?> clientSetup = Class.forName("mccfk.DeltaForce.java.client.DeltaForceClientSetup");
                clientSetup.getMethod("init", IEventBus.class).invoke(null, modEventBus);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(DeltaForce.class)
                    .error("客户端初始化失败", e);
            }
        }
    }

    /**
     * 获取单例实例
     */
    public static DeltaForce getInstance() {
        return instance;
    }

    /**
     * 设置 DFGAME 状态
     */
    public static void setDFGame(boolean enabled) {
        dfGameEnabled = enabled;
    }

    /**
     * 获取 DFGAME 状态
     */
    public static boolean isDFGameEnabled() {
        return dfGameEnabled;
    }

    /**
     * 设置“禁用全部界面”开关（默认关闭，仅可通过指令开启）
     */
    public static void setDFLockAllScreens(boolean enabled) {
        dfLockAllScreens = enabled;
    }

    /**
     * 获取“禁用全部界面”开关状态
     */
    public static boolean isDFLockAllScreensEnabled() {
        return dfLockAllScreens;
    }

    /**
     * 设置“使用自定义背包”开关（默认关闭，仅可通过指令开启）
     */
    public static void setDFCustomInventory(boolean enabled) {
        dfCustomInventory = enabled;
    }

    /**
     * 获取"使用自定义背包"开关状态
     */
    public static boolean isDFCustomInventoryEnabled() {
        return dfCustomInventory;
    }

    /**
     * 设置调试模式
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    /**
     * 获取调试模式状态
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 设置管理员调试总开关（仅服务端调用）
     */
    public static void setAllowAdminDebug(boolean enabled) {
        allowAdminDebug = enabled;
    }

    /**
     * 获取管理员调试总开关状态
     */
    public static boolean isAllowAdminDebug() {
        return allowAdminDebug;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DataInitializer.init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    public static void setDFMain(boolean enabled) {
        // 打印调用来源，方便诊断状态不一致问题
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        System.out.println("[DF调试] setDFMain(" + enabled + ") 调用者: " + caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber());
        dfMainEnabled = enabled;
        // 客户端侧持久化到配置文件（通过反射避免服务端类加载错误）
        try {
            Class<?> clientSetup = Class.forName("mccfk.DeltaForce.java.client.DeltaForceClientSetup");
            clientSetup.getMethod("onDfMainChanged", boolean.class).invoke(null, enabled);
            System.out.println("[DF调试] setDFMain → onDfMainChanged(" + enabled + ") 反射调用成功，配置已持久化");
        } catch (Exception e) {
            System.out.println("[DF调试] setDFMain → onDfMainChanged 反射失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public static boolean isDFMainEnabled() {
        return dfMainEnabled;
    }

    /**
     * 获取设置界面白名单（允许显示配置的模组ID集合）
     */
    public static Set<String> getConfigWhitelist() {
        return configWhitelist;
    }

    /**
     * 其他模组在主类中调用此方法注册自身，使其出现在 DF 设置界面中
     */
    public static void registerConfigurableMod(String modId) {
        configWhitelist.add(modId);
    }

    /**
     * 检查指定模组ID是否在设置界面白名单中
     */
    public static boolean isConfigWhitelisted(String modId) {
        return configWhitelist.contains(modId);
    }

    /**
     * 注册配置提供者（其他模组在主类中调用）
     * @param modId 模组ID
     * @param provider 配置提供者实例
     */
    public static void registerConfigProvider(String modId, IConfigProvider provider) {
        configProviders.put(modId, provider);
    }

    /**
     * 获取指定模组的配置提供者
     */
    public static IConfigProvider getConfigProvider(String modId) {
        return configProviders.get(modId);
    }

    /**
     * 获取所有已注册的配置提供者
     */
    public static Map<String, IConfigProvider> getConfigProviders() {
        return configProviders;
    }

    /**
     * 接收其他模组通过 IMC 发送的注册请求
     */
    private void onInterModEnqueue(InterModEnqueueEvent event) {
        InterModComms.getMessages(MODID, method -> method.equals("register_configurable"))
            .forEach(msg -> configWhitelist.add((String) msg.messageSupplier().get()));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DFCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        var player = event.getEntity();
        String playerName = player.getGameProfile().getName();

        // 初始化玩家经济数据（文件不存在则创建默认）
        EeconomyManager.getInstance().initPlayerData(playerName);

        // 初始化玩家角色数据目录（干员定义已统一合并到 friend 模组，此处仅确保目录存在）
        PlayerCharacterStore.init();

        if (player instanceof ServerPlayer serverPlayer) {
            DeltaForceNetwork.syncMarkers(serverPlayer);
            // 不再在登录时强制同步dfMain开关，避免覆盖客户端已保存的配置
        }

        // 提示读取失败的配置文件
        var failedFiles = EeconomyManager.getInstance().getFailedFiles();
        if (!failedFiles.isEmpty()) {
            for (var f : failedFiles) {
                player.sendSystemMessage(
                    Component.literal("§c[DF] 文件 ")
                        .append(Component.literal(f.fileName).withStyle(style -> style.withUnderlined(true)))
                        .append(Component.literal(" 损坏，文件路径: "))
                        .append(Component.literal(f.filePath + ".no")
                            .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, f.filePath + ".no"))
                                .withUnderlined(true)
                            )
                        )
                );
                org.slf4j.LoggerFactory.getLogger(DeltaForce.class)
                    .warn("[DF] 文件 {} 损坏，路径: {}.no", f.fileName, f.filePath);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarkerManager.removeMarker(player.getUUID());
            DeltaForceNetwork.broadcastMarkers(player.getServer());
            DeltaForceNetwork.clearPlayerAdminVerification(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getEntity();
        String playerName = player.getGameProfile().getName();

        // DFGAME 死亡后重生：重置自定义血量为满值
        DFGameManager.setHealth(playerName, DFGameManager.MAX_HEALTH);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MarkerManager.clearAll();
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (MarkerManager.tick()) {
            var server = event.getServer();
            if (server != null) {
                DeltaForceNetwork.broadcastMarkers(server);
            }
        }
    }
}