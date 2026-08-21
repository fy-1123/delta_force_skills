package com.rzy.dealt_force_skills.client;

import net.minecraft.core.BlockPos;

import java.util.List;

public final class ClientHvkAsaraArenaState {
    private static boolean active;
    private static int remaining;
    private static List<BlockPos> positions = List.of();

    private ClientHvkAsaraArenaState() {
    }

    public static void sync(boolean activeSession, int remainingHostiles, List<BlockPos> hostilePositions) {
        active = activeSession;
        remaining = Math.max(0, remainingHostiles);
        positions = active && remaining < 8 ? List.copyOf(hostilePositions) : List.of();
    }

    public static boolean active() {
        return active;
    }

    public static int remaining() {
        return remaining;
    }

    public static List<BlockPos> positions() {
        return positions;
    }
}
