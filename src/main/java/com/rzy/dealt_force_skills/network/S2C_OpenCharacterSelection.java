package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_OpenCharacterSelection {
    private final boolean allowReselection;
    private final boolean freeReselection;

    public S2C_OpenCharacterSelection() {
        this(false);
    }

    public S2C_OpenCharacterSelection(boolean allowReselection) {
        this(allowReselection, false);
    }

    public S2C_OpenCharacterSelection(boolean allowReselection, boolean freeReselection) {
        this.allowReselection = allowReselection;
        this.freeReselection = freeReselection;
    }

    public static void encode(S2C_OpenCharacterSelection msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.allowReselection);
        buf.writeBoolean(msg.freeReselection);
    }

    public static S2C_OpenCharacterSelection decode(FriendlyByteBuf buf) {
        return new S2C_OpenCharacterSelection(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(S2C_OpenCharacterSelection msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    if (msg.allowReselection) {
                        ClientCharacterSelectionState.openReselectionScreen(msg.freeReselection);
                    } else {
                        ClientCharacterSelectionState.openSelectionScreen();
                    }
                }
        ));
        ctx.get().setPacketHandled(true);
    }
}
