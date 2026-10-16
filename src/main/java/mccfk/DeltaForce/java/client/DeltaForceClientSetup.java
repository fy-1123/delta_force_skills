package mccfk.DeltaForce.java.client;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.screen.DfsConfigProvider;
import mccfk.DeltaForce.java.client.screen.SettingsScreen;
import mccfk.DeltaForce.java.item.creative.DeltaForceCreativeTabs;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;


/**
 * DeltaForce 客户端专用初始化
 * 与 DeltaForce 主类隔离，避免服务端类加载时触发 RuntimeDistCleaner
 */
public final class DeltaForceClientSetup {

    private DeltaForceClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        // 注册配置界面入口（Forge 模组列表"Config"按钮 → 打开设置界面）
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, parent) -> new SettingsScreen(parent)));

        // 注册自定义掉落物配置
        CustomDropConfig.bootstrap();
        DeltaForce.registerConfigProvider(DeltaForce.MODID, new CustomDropConfigProvider());

        // 注册核心模组配置（dealt_force_skills），联网时同步服务端配置
        DeltaForce.registerConfigProvider("dealt_force_skills", new DfsConfigProvider());

        // 注册设置界面UI状态配置（独立配置，不显示在设置界面中）
        SettingsUIConfig.bootstrap();

        // 注册DF创造模式标签页
        DeltaForceCreativeTabs.register(modEventBus);
    }
}
