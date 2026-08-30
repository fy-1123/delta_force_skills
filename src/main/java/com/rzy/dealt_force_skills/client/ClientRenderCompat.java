package com.rzy.dealt_force_skills.client;

public final class ClientRenderCompat {
    private ClientRenderCompat() {
    }

    public static float partialTick(float partialTick) {
        return partialTick;
    }

    public static float partialTick(double partialTick) {
        return (float) partialTick;
    }

    public static float frameTime(net.minecraft.client.Minecraft minecraft) {
        return minecraft == null ? 0.0F : minecraft.getFrameTime();
    }
}