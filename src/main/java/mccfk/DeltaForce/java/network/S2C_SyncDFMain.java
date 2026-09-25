package mccfk.DeltaForce.java.network;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 服务端→客户端 - 同步 DF 主界面快捷键开关状态
 * 发送时机：/df main t/f 执行后广播全服；玩家登录时单独同步
 */
public record S2C_SyncDFMain(boolean enabled) {

    public static void encode(S2C_SyncDFMain message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
    }

    public static S2C_SyncDFMain decode(FriendlyByteBuf buffer) {
        return new S2C_SyncDFMain(buffer.readBoolean());
    }

    /** 客户端接收处理：更新本地状态 */
    public static void handle(S2C_SyncDFMain message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DeltaForce.setDFMain(message.enabled()));
        context.setPacketHandled(true);
    }
}