package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.client.screen.CommanderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2C_OpenCommanderScreen(boolean support) {
    public static void encode(S2C_OpenCommanderScreen msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.support);
    }

    public static S2C_OpenCommanderScreen decode(FriendlyByteBuf buf) {
        return new S2C_OpenCommanderScreen(buf.readBoolean());
    }

    public static void handle(S2C_OpenCommanderScreen msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new CommanderScreen(msg.support))));
        context.get().setPacketHandled(true);
    }
}
