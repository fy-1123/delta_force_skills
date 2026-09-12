package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_StaminaVault() {
    public static void encode(C2S_StaminaVault msg, FriendlyByteBuf buf) { }
    public static C2S_StaminaVault decode(FriendlyByteBuf buf) { return new C2S_StaminaVault(); }
    public static void handle(C2S_StaminaVault msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> StaminaMovement.tryVault(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
