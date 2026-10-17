package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla kicks players that look like they float for more than 80 ticks ({@code multiplayer.disconnect.flying}).
 * A rider hanging in mid-air on a long zipline matches that heuristic - no blocks below them, the client
 * reports no downward motion - so the flag is cleared while a traversal is active.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HvkZiplineConnectionMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void dfs$skipFloatingKickWhileRiding(CallbackInfo ci) {
        HvkZiplineConnectionAccess access = (HvkZiplineConnectionAccess) (Object) this;
        ServerPlayer player = access.dfs$player();
        if (player != null && HvkZiplineManager.isRiding(player)) {
            access.dfs$setClientIsFloating(false);
        }
    }
}
