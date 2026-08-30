package mccfk.DeltaForce.java.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record S2C_DfWarehouseSnapshot(int page, String container, int pageSlots, List<ItemStack> warehouse) {
    private static final int MAX_WAREHOUSE_SLOTS = 4096;

    public S2C_DfWarehouseSnapshot(int page, String container, int pageSlots, Collection<ItemStack> warehouse) {
        this(page, container, pageSlots, warehouse == null ? List.of() : List.copyOf(warehouse));
    }

    public S2C_DfWarehouseSnapshot {
        container = container == null ? "" : container;
        warehouse = warehouse == null ? List.of() : List.copyOf(warehouse);
        if (page < 0 || pageSlots < 0 || pageSlots > MAX_WAREHOUSE_SLOTS || warehouse.size() > pageSlots) {
            throw new IllegalArgumentException("Invalid warehouse snapshot sizes");
        }
    }

    public static void encode(S2C_DfWarehouseSnapshot message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.page);
        buffer.writeUtf(message.container, 64);
        buffer.writeVarInt(message.pageSlots);
        buffer.writeVarInt(message.warehouse.size());
        message.warehouse.forEach(buffer::writeItem);
    }

    public static S2C_DfWarehouseSnapshot decode(FriendlyByteBuf buffer) {
        int page = buffer.readVarInt();
        String container = buffer.readUtf(64);
        int pageSlots = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (page < 0 || pageSlots < 0 || pageSlots > MAX_WAREHOUSE_SLOTS || size < 0 || size > pageSlots) {
            throw new DecoderException("Invalid warehouse snapshot sizes: " + pageSlots + "/" + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int index = 0; index < size; index++) stacks.add(buffer.readItem());
        return new S2C_DfWarehouseSnapshot(page, container, pageSlots, stacks);
    }

    public static void handle(S2C_DfWarehouseSnapshot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketBridge.openWarehouse(message));
        context.setPacketHandled(true);
    }
}
