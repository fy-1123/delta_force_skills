package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class S2C_SyncModGameRules {
    private final boolean armorSpecialsEnabled;
    private final boolean rebuyEnabled;
    private final boolean havocValueVisible;
    private final long[] havocPrices;
    private final List<DealtItemsHavocConfig.VariantPrice> havocVariantPrices;

    public S2C_SyncModGameRules(boolean armorSpecialsEnabled, boolean rebuyEnabled, boolean havocValueVisible,
                                long[] havocPrices,
                                List<DealtItemsHavocConfig.VariantPrice> havocVariantPrices) {
        this.armorSpecialsEnabled = armorSpecialsEnabled;
        this.rebuyEnabled = rebuyEnabled;
        this.havocValueVisible = havocValueVisible;
        this.havocPrices = havocPrices.clone();
        this.havocVariantPrices = List.copyOf(havocVariantPrices);
    }

    public static void encode(S2C_SyncModGameRules msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.armorSpecialsEnabled);
        buf.writeBoolean(msg.rebuyEnabled);
        buf.writeBoolean(msg.havocValueVisible);
        buf.writeVarInt(msg.havocPrices.length);
        for (long price : msg.havocPrices) buf.writeVarLong(price);
        buf.writeVarInt(msg.havocVariantPrices.size());
        for (DealtItemsHavocConfig.VariantPrice variant : msg.havocVariantPrices) {
            buf.writeItem(variant.stack());
            buf.writeVarLong(variant.price());
        }
    }

    public static S2C_SyncModGameRules decode(FriendlyByteBuf buf) {
        boolean armorSpecialsEnabled = buf.readBoolean();
        boolean rebuyEnabled = buf.readBoolean();
        boolean havocValueVisible = buf.readBoolean();
        int size = buf.readVarInt();
        if (size < 0 || size > 1_000_000) throw new IllegalArgumentException("Invalid Havoc price count: " + size);
        long[] havocPrices = new long[size];
        for (int i = 0; i < size; i++) havocPrices[i] = buf.readVarLong();
        int variantCount = buf.readVarInt();
        if (variantCount < 0 || variantCount > 10_000) {
            throw new IllegalArgumentException("Invalid Havoc variant price count: " + variantCount);
        }
        List<DealtItemsHavocConfig.VariantPrice> variants = new ArrayList<>(variantCount);
        for (int i = 0; i < variantCount; i++) {
            ItemStack stack = buf.readItem();
            variants.add(new DealtItemsHavocConfig.VariantPrice(stack, buf.readVarLong()));
        }
        return new S2C_SyncModGameRules(armorSpecialsEnabled, rebuyEnabled, havocValueVisible, havocPrices, variants);
    }

    public static void handle(S2C_SyncModGameRules msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ModGameRules.acceptClientSync(msg.armorSpecialsEnabled, msg.rebuyEnabled, msg.havocValueVisible);
            DealtItemsHavocConfig.acceptClientPrices(msg.havocPrices, msg.havocVariantPrices);
        });
        ctx.get().setPacketHandled(true);
    }
}
