package mccfk.DeltaForce.java.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: 客户端→服务端 - 管理员验证凭据
 * 客户端登录时发送SHA-256哈希，服务端验证后决定是否启用管理员调试验证权限
 */
public record C2S_AdminAuth(String hash) {

    public static void encode(C2S_AdminAuth message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.hash != null ? message.hash : "", 64);
    }

    public static C2S_AdminAuth decode(FriendlyByteBuf buffer) {
        return new C2S_AdminAuth(buffer.readUtf(64));
    }

    public static void handle(C2S_AdminAuth message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (message.hash != null && message.hash.equals(
                        mccfk.DeltaForce.java.client.AdminAuthManager.getCorrectHash())) {
                    DeltaForceNetwork.addAdminVerifiedPlayer(player.getUUID());
                } else {
                    DeltaForceNetwork.removeAdminVerifiedPlayer(player.getUUID());
                }
        });
    }
}