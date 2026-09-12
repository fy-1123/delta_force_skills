package com.rzy.dealt_force_skills.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** A ten-point layer spans the same 81 GUI pixels as the vanilla hunger row. */
public final class StaminaBarRenderer {
    public static final int WIDTH = 81;
    public static final int HEIGHT = 7;
    private static final int INNER_WIDTH = WIDTH - 2;
    private static final int RED = 0xFFFF4545;
    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFFFFF3B0, 0xFFFFE34A, 0xFFFFBF32,
            0xFF55E5E5, 0xFF5599FF, 0xFFFF963F, RED
    };

    private StaminaBarRenderer() { }

    public static void render(GuiGraphics graphics, double value, boolean burningHeart) {
        graphics.fill(RenderType.guiOverlay(), 0, 0, WIDTH, HEIGHT, 0xE0101010);
        graphics.fill(RenderType.guiOverlay(), 1, 1, WIDTH - 1, HEIGHT - 1, 0xFF303030);
        if (!Double.isFinite(value) || value <= 0.0D) return;

        // Exact multiples stay full in their current tier instead of showing an empty next tier.
        double layer = Math.max(0.0D, Math.ceil(value / 10.0D) - 1.0D);
        int colorIndex = (int) (layer % COLORS.length);
        int color = burningHeart ? RED : COLORS[colorIndex];
        if (layer > 0.0D) {
            int previousColor = burningHeart ? RED : COLORS[(colorIndex + COLORS.length - 1) % COLORS.length];
            graphics.fill(RenderType.guiOverlay(), 1, 1, WIDTH - 1, HEIGHT - 1, previousColor);
        }
        double fraction = Math.max(0.0D, Math.min(1.0D, (value - layer * 10.0D) / 10.0D));
        int filled = Math.min(INNER_WIDTH, (int) Math.ceil(fraction * INNER_WIDTH));
        if (filled > 0) graphics.fill(RenderType.guiOverlay(), 1, 1, 1 + filled, HEIGHT - 1, color);
    }
}
