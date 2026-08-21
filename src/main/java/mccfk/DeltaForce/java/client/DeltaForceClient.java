package mccfk.DeltaForce.java.client;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.overlay.GroundItemOverlay;
import mccfk.DeltaForce.java.client.overlay.ItemDescriptionLoader;
import mccfk.DeltaForce.java.client.renderer.GroundItemRenderer;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeltaForce.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DeltaForceClient {
    public static final KeyMapping MARKER_KEY = new KeyMapping(
            "key.deltaforce.marker", InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, "key.categories.deltaforce");

    private DeltaForceClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DeltaForceEntities.GROUND_ITEM.get(), GroundItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ground_item_overlay", GroundItemOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MARKER_KEY);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                ItemDescriptionLoader.clearCache());
    }

}
