package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2C_OpenGtiTradingHall(long coins, List<GtiTradingHallManager.Entry> entries) {
    private static final int MAX_ENTRIES = 4096;
    public S2C_OpenGtiTradingHall {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many GTI entries: " + entries.size());
        }
    }
    public static void encode(S2C_OpenGtiTradingHall msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.coins); buf.writeVarInt(msg.entries.size());
        for (GtiTradingHallManager.Entry entry : msg.entries) {
            buf.writeItem(entry.stack()); buf.writeLong(entry.price()); buf.writeUtf(entry.category().id(), 64);
        }
    }
    public static S2C_OpenGtiTradingHall decode(FriendlyByteBuf buf) {
        long coins = buf.readLong(); int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new DecoderException("Invalid GTI entry count: " + size);
        List<GtiTradingHallManager.Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItem();
            long price = buf.readLong();
            GtiTradingHallManager.Category category = GtiTradingHallManager.Category.byId(buf.readUtf(64))
                    .orElse(GtiTradingHallManager.Category.MISC);
            entries.add(new GtiTradingHallManager.Entry(stack, price, category));
        }
        return new S2C_OpenGtiTradingHall(coins, entries);
    }
    public static void handle(S2C_OpenGtiTradingHall msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openGtiTradingHall(msg.coins, msg.entries)));
        ctx.get().setPacketHandled(true);
    }
}
