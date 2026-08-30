package mccfk.DeltaForce.java;

import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.S2C_SyncMarkers;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import mccfk.DeltaForce.java.item.DeltaForceItems;
import mccfk.DeltaForce.java.item.creative.DeltaForceCreativeTabs;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.command.DFCommand;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;
import mccfk.DeltaForce.java.dfgame.DFGameManager;

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
    private static boolean dfMainEnabled = false;

    public DeltaForce() {
        instance = this;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册DF物品
        DeltaForceItems.register(modEventBus);

        // 注册DF创造模式标签页
        DeltaForceCreativeTabs.register(modEventBus);

        // 注册DF实体
        DeltaForceEntities.ENTITY_TYPES.register(modEventBus);

        // 注册服务端事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册客户端 DFGAME 事件（仅客户端侧）
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            registerClientEventHandler("mccfk.DeltaForce.java.client.dfgame.DFGameGuiEventHandler");
            registerClientEventHandler("mccfk.DeltaForce.java.client.dfgame.DFGameDamageEventHandler");
        }
    }

    private static void registerClientEventHandler(String className) {
        try {
            MinecraftForge.EVENT_BUS.register(Class.forName(className));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register client event handler " + className, exception);
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
     * 设置"禁用全部界面"开关（默认关闭，仅可通过指令开启）
     */
    public static void setDFLockAllScreens(boolean enabled) {
        dfLockAllScreens = enabled;
    }

    /**
     * 获取"禁用全部界面"开关状态
     */
    public static boolean isDFLockAllScreensEnabled() {
        return dfLockAllScreens;
    }

    /**
     * 设置"使用自定义背包"开关（默认关闭，仅可通过指令开启）
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

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DataInitializer.init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    public static void setDFMain(boolean enabled) {
        dfMainEnabled = enabled;
    }

    public static boolean isDFMainEnabled() {
        return dfMainEnabled;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DFCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        String playerName = player.getGameProfile().getName();

        // 初始化玩家经济数据（文件不存在则创建默认）
        EeconomyManager.getInstance().initPlayerData(playerName);

        // 初始化玩家角色数据目录（干员定义已统一合并到 friend 模组，此处仅确保目录存在）
        PlayerCharacterStore.init();

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(new S2C_SyncMarkers(MarkerManager.getAllMarkers()), serverPlayer);
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
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarkerManager.removeMarker(player.getUUID());
            NetworkHandler.sendToAll(new S2C_SyncMarkers(MarkerManager.getAllMarkers()));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && MarkerManager.tick()) {
            NetworkHandler.sendToAll(new S2C_SyncMarkers(MarkerManager.getAllMarkers()));
        }
    }
}
