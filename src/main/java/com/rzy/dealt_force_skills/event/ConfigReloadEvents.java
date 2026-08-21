package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class ConfigReloadEvents {
    private static final ResourceManagerReloadListener LISTENER = resourceManager -> {
        ConfigHotReload.reloadAll();
        CharacterBranchPackManager.reload(resourceManager);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                ConfigHotReload.refreshTaczCatalog();
                server.getPlayerList().getPlayers().forEach(CharacterAvailability::syncToClient);
            });
        }
    };

    private ConfigReloadEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(LISTENER);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CharacterBranchPackManager.reload(event.getServer().getResourceManager());
        ConfigHotReload.refreshTaczCatalog();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            CharacterBranchPackManager.reload(player.getServer().getResourceManager());
            CharacterAvailability.syncToClient(player);
        }
    }
}
