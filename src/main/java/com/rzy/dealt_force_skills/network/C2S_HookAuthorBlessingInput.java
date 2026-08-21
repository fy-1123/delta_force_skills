package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.item.HookAuthorBlessingItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_HookAuthorBlessingInput(int button, int phase, boolean sneaking, int heldTicks) {
    public static void encode(C2S_HookAuthorBlessingInput message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.button); buffer.writeByte(message.phase);
        buffer.writeBoolean(message.sneaking); buffer.writeVarInt(message.heldTicks);
    }
    public static C2S_HookAuthorBlessingInput decode(FriendlyByteBuf buffer) {
        return new C2S_HookAuthorBlessingInput(buffer.readByte(), buffer.readByte(), buffer.readBoolean(), buffer.readVarInt());
    }
    public static void handle(C2S_HookAuthorBlessingInput message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> { ServerPlayer player = context.getSender();
            if (player != null) HookAuthorBlessingItem.input(player, message.button, message.phase, message.sneaking, message.heldTicks); });
        context.setPacketHandled(true);
    }
}
