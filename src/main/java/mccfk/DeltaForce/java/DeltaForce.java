package mccfk.DeltaForce.java;

import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.item.DeltaForceItems;
import mccfk.DeltaForce.java.item.creative.DeltaForceCreativeTabs;
import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.S2C_SyncMarkers;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DeltaForce.MODID)
public final class DeltaForce {
    public static final String MODID = "deltaforce";

    public DeltaForce() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DeltaForceItems.register(modBus);
        DeltaForceCreativeTabs.register(modBus);
        DeltaForceEntities.ENTITY_TYPES.register(modBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DataInitializer.init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(new S2C_SyncMarkers(MarkerManager.getAllMarkers()), player);
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && MarkerManager.tick()) {
            NetworkHandler.sendToAll(new S2C_SyncMarkers(MarkerManager.getAllMarkers()));
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MarkerManager.clearAll();
    }

}
