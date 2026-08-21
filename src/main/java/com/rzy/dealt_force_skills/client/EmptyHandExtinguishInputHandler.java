package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.C2S_EmptyHandExtinguish;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class EmptyHandExtinguishInputHandler {
    private static final int HEARTBEAT_INTERVAL_TICKS = 5;
    private static boolean holdingSent;
    private static int heartbeatTicks;

    private EmptyHandExtinguishInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick(Minecraft.getInstance());
    }

    private static void tick(Minecraft minecraft) {
        boolean holding = minecraft.player != null
                && minecraft.screen == null
                && minecraft.player.getMainHandItem().isEmpty()
                && minecraft.player.getOffhandItem().isEmpty()
                && minecraft.options.keyUse.isDown();
        if (holding) {
            if (!holdingSent || heartbeatTicks-- <= 0) {
                NetworkHandler.CHANNEL.sendToServer(new C2S_EmptyHandExtinguish(true));
                holdingSent = true;
                heartbeatTicks = HEARTBEAT_INTERVAL_TICKS;
            }
        } else if (holdingSent) {
            NetworkHandler.CHANNEL.sendToServer(new C2S_EmptyHandExtinguish(false));
            holdingSent = false;
            heartbeatTicks = 0;
        }
    }
}
