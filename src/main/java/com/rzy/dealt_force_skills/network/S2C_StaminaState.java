package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2C_StaminaState(double value, double maximum) {
    public static void encode(S2C_StaminaState msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.value);
        buf.writeDouble(msg.maximum);
    }

    public static S2C_StaminaState decode(FriendlyByteBuf buf) {
        return new S2C_StaminaState(buf.readDouble(), buf.readDouble());
    }

    public static void handle(S2C_StaminaState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.StaminaHud", "update", msg.value, msg.maximum));
        ctx.get().setPacketHandled(true);
    }
}
