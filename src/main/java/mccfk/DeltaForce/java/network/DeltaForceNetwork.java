package mccfk.DeltaForce.java.network;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.Collection;

/**
 * 网络包注册器 - Forge 1.20.1 适配版
 * 复用 dealt_force_skills 的共享通道注册 DeltaForce 专属网络包
 */
@Mod.EventBusSubscriber(modid = DeltaForce.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DeltaForceNetwork {

    /** 包ID起始偏移（避免与 dealt_force_skills 冲突） */
    private static int packetId = 201;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

            NetworkHandler.CHANNEL.messageBuilder(C2S_CreateMarker.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_CreateMarker::encode)
                    .decoder(C2S_CreateMarker::decode)
                    .consumerMainThread(C2S_CreateMarker::handle)
                    .add();

            NetworkHandler.CHANNEL.messageBuilder(S2C_SyncMarkers.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncMarkers::encode)
                    .decoder(S2C_SyncMarkers::decode)
                    .consumerMainThread(S2C_SyncMarkers::handle)
                    .add();

            NetworkHandler.CHANNEL.messageBuilder(C2S_DfWarehouseAction.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_DfWarehouseAction::encode)
                    .decoder(C2S_DfWarehouseAction::decode)
                    .consumerMainThread(C2S_DfWarehouseAction::handle)
                    .add();

            NetworkHandler.CHANNEL.messageBuilder(S2C_DfWarehouseSnapshot.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_DfWarehouseSnapshot::encode)
                    .decoder(S2C_DfWarehouseSnapshot::decode)
                    .consumerMainThread(S2C_DfWarehouseSnapshot::handle)
                    .add();

        });
    }

    /** 向指定玩家同步当前所有标点 */
    public static void syncMarkers(ServerPlayer player) {
        Collection<Marker> allMarkers = MarkerManager.getAllMarkers();
        NetworkHandler.sendToPlayer(new S2C_SyncMarkers(allMarkers), player);
    }

    /** 向全服广播标点变更 */
    public static void broadcastMarkers(MinecraftServer server) {
        if (server == null) return;
        Collection<Marker> allMarkers = MarkerManager.getAllMarkers();
        var packet = new S2C_SyncMarkers(allMarkers);
        server.getPlayerList().getPlayers().forEach(player ->
                NetworkHandler.sendToPlayer(packet, player));
    }

}