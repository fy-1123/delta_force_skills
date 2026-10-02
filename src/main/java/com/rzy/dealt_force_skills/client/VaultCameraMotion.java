package com.rzy.dealt_force_skills.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Visual-only interpolation: collision and actual player position remain server-authoritative. */
public final class VaultCameraMotion {
    private static Vec3 from = Vec3.ZERO;
    private static Vec3 to = Vec3.ZERO;
    private static long started;
    private static ClientLevel level;

    private VaultCameraMotion() { }

    public static void update(double x, double y, double z, double endX, double endY, double endZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 next = new Vec3(endX, endY, endZ);
        Vec3 initial = new Vec3(x, y, z);
        if (!Double.isFinite(next.lengthSqr()) || initial.distanceToSqr(next) > 16.0) return;
        long now = Util.getMillis();
        from = level == mc.level && now - started < 150L ? sample(now) : initial;
        to = next;
        level = mc.level;
        started = now;
    }

    private static Vec3 sample(long now) {
        return from.lerp(to, Math.max(0.0, Math.min(1.0, (now - started) / 50.0)));
    }

    public static Vec3 offset(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        long now = Util.getMillis();
        if (mc.player == null || mc.level != level || now - started >= 50L || mc.isPaused()) return Vec3.ZERO;
        Vec3 offset = sample(now).subtract(mc.player.getPosition(partialTick));
        return offset.lengthSqr() <= 16.0 ? offset : Vec3.ZERO;
    }
}
