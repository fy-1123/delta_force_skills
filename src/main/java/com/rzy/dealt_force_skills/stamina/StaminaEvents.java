package com.rzy.dealt_force_skills.stamina;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class StaminaEvents {
    private StaminaEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            StaminaMovement.tick(player);
            StaminaManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        StaminaMovement.reset(event.getEntity());
        StaminaManager.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getEntity().getPersistentData().put(StaminaManager.ROOT, StaminaManager.data(event.getOriginal()).copy());
        }
        StaminaMovement.reset(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        StaminaMovement.reset(event.getEntity());
        StaminaManager.sync(event.getEntity());
    }
}
