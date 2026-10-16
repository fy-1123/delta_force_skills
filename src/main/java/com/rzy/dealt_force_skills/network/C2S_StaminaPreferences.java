package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_StaminaPreferences(int mask) {
    public static void encode(C2S_StaminaPreferences msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.mask()); }
    public static C2S_StaminaPreferences decode(FriendlyByteBuf buf) { return new C2S_StaminaPreferences(buf.readVarInt()); }
    public static void handle(C2S_StaminaPreferences msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.rzy.dealt_force_skills.stamina.StaminaFeature.apply(ctx.get().getSender(), msg.mask()));
        ctx.get().setPacketHandled(true);
    }
}
