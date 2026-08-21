package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.client.screen.GtiSpecialOperationsScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class S2C_OpenGtiSpecialOperations {
    public static void encode(S2C_OpenGtiSpecialOperations msg, FriendlyByteBuf buf) { }
    public static S2C_OpenGtiSpecialOperations decode(FriendlyByteBuf buf) { return new S2C_OpenGtiSpecialOperations(); }
    public static void handle(S2C_OpenGtiSpecialOperations msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> GtiSpecialOperationsScreen::open));
        ctx.get().setPacketHandled(true);
    }
}
