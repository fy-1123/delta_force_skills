package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.music.FoxMusicData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record S2C_FoxMusicSnapshot(UUID device, int page, int total, int selected, boolean playing, int mode,
                                   List<ItemStack> discs, String feedback) {
    public S2C_FoxMusicSnapshot { discs = discs.stream().map(ItemStack::copy).toList(); }
    public static void encode(S2C_FoxMusicSnapshot msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.device); buf.writeVarInt(msg.page); buf.writeVarInt(msg.total); buf.writeVarInt(msg.selected);
        buf.writeBoolean(msg.playing); buf.writeVarInt(msg.mode); buf.writeUtf(msg.feedback, 64);
        buf.writeVarInt(msg.discs.size()); for (ItemStack disc : msg.discs) buf.writeItem(disc);
    }
    public static S2C_FoxMusicSnapshot decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID(); int page = buf.readVarInt(), total = buf.readVarInt(), selected = buf.readVarInt();
        boolean playing = buf.readBoolean(); int mode = buf.readVarInt(); String feedback = buf.readUtf(64);
        int count = buf.readVarInt();
        if (count < 0 || count > FoxMusicData.PAGE_SIZE) throw new IllegalArgumentException("Invalid music page");
        List<ItemStack> discs = new ArrayList<>(); for (int i = 0; i < count; i++) discs.add(buf.readItem());
        return new S2C_FoxMusicSnapshot(id, page, total, selected, playing, mode, discs, feedback);
    }
    public static void handle(S2C_FoxMusicSnapshot msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.FoxMusicPlayerScreen", "open", msg));
        ctx.get().setPacketHandled(true);
    }
}
