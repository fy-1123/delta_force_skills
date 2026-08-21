package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkEnchantInfusionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_HvkEnchantInfusionAction(BlockPos pos, Action action, int selectedSlot,
                                            String enchantmentId, int level) {
    public enum Action { INSERT_SLOT, RETRIEVE, APPLY, APPLY_AFFIX }

    public static void encode(C2S_HvkEnchantInfusionAction msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.selectedSlot);
        buf.writeUtf(msg.enchantmentId, 256);
        buf.writeVarInt(msg.level);
    }

    public static C2S_HvkEnchantInfusionAction decode(FriendlyByteBuf buf) {
        return new C2S_HvkEnchantInfusionAction(buf.readBlockPos(), buf.readEnum(Action.class),
                buf.readVarInt(), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(C2S_HvkEnchantInfusionAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level().getBlockEntity(msg.pos) instanceof HvkEnchantInfusionBlockEntity infusion)) return;
            switch (msg.action) {
                case INSERT_SLOT -> infusion.insertFromSlot(player, msg.selectedSlot);
                case RETRIEVE -> infusion.retrieve(player);
                case APPLY -> infusion.apply(player, msg.enchantmentId, msg.level);
                case APPLY_AFFIX -> infusion.applyAffix(player, msg.enchantmentId, msg.level);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
