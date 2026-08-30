package com.rzy.dealt_force_skills.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2C_OpenHvkEnchantInfusion(BlockPos pos, ItemStack stack, List<Entry> entries,
                                          boolean apotheosisAvailable, List<AffixEntry> affixes) {
    public static final int MAX_ENTRIES = 8192;
    public record Entry(String id, int currentLevel, int maxLevel) {}
    public record AffixEntry(String id, String name, int currentValue, int maxValue) {}

    public static void encode(S2C_OpenHvkEnchantInfusion msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeItem(msg.stack);
        buf.writeVarInt(Math.min(MAX_ENTRIES, msg.entries.size()));
        for (int i = 0; i < Math.min(MAX_ENTRIES, msg.entries.size()); i++) {
            Entry entry = msg.entries.get(i);
            buf.writeUtf(entry.id, 256);
            buf.writeVarInt(entry.currentLevel);
            buf.writeVarInt(entry.maxLevel);
        }
        buf.writeBoolean(msg.apotheosisAvailable);
        buf.writeVarInt(Math.min(MAX_ENTRIES, msg.affixes.size()));
        for (int i = 0; i < Math.min(MAX_ENTRIES, msg.affixes.size()); i++) {
            AffixEntry entry = msg.affixes.get(i);
            buf.writeUtf(entry.id, 256);
            buf.writeUtf(entry.name, 512);
            buf.writeVarInt(entry.currentValue);
            buf.writeVarInt(entry.maxValue);
        }
    }

    public static S2C_OpenHvkEnchantInfusion decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStack stack = buf.readItem();
        int count = Math.min(MAX_ENTRIES, Math.max(0, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(256), buf.readVarInt(), buf.readVarInt()));
        }
        boolean apotheosisAvailable = buf.readBoolean();
        int affixCount = Math.min(MAX_ENTRIES, Math.max(0, buf.readVarInt()));
        List<AffixEntry> affixes = new ArrayList<>(affixCount);
        for (int i = 0; i < affixCount; i++) {
            affixes.add(new AffixEntry(buf.readUtf(256), buf.readUtf(512), buf.readVarInt(), buf.readVarInt()));
        }
        return new S2C_OpenHvkEnchantInfusion(pos, stack, List.copyOf(entries),
                apotheosisAvailable, List.copyOf(affixes));
    }

    public static void handle(S2C_OpenHvkEnchantInfusion msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HvkEnchantInfusionScreen.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
