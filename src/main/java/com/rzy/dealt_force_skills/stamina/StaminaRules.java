package com.rzy.dealt_force_skills.stamina;

public final class StaminaRules {
    public static final double BASE_CAPACITY = 10.0D;
    public static final double SPRINT_COST_PER_TICK = 0.1D / 20.0D;
    public static final double SWIM_COST_PER_TICK = 0.2D / 20.0D;
    public static final double VAULT_COST = 1.0D;
    public static final double DWOLF_SLIDE_COST = 1.0D;
    public static final double GHROTH_TACTICAL_COST = 0.5D;
    public static final double SINEVA_BASH_COST = 2.0D;
    public static final double SINEVA_CHARGE_COST = 3.5D;
    public static final int RECOVERY_DELAY = 20;
    public static final int BOOST_GRACE_TICKS = 10;

    private StaminaRules() { }

    public static double capacity(double base, int training, int capacity, int boost, double otherBonus) {
        return Math.max(1.0D, base * (1.0D + training * 0.07D + capacity * 0.07D
                + boost * 0.15D + otherBonus));
    }

    public static boolean canSpend(double value, double cost, double floor, boolean free) {
        return Double.isFinite(cost) && cost >= 0.0D
                && (free || value + 1.0E-9D >= cost + floor);
    }

    public static double skillCost(double baseCost, boolean commander) {
        return Math.max(0.0D, baseCost) * (commander ? 0.5D : 1.0D);
    }

    public static double recoveryPerTick(int boost, int training) {
        return 1.5D * (1.0D + boost * 0.10D) * (1.0D + training * 0.05D) / 20.0D;
    }

    public static double jumpRise(double velocity, double gravity) {
        double rise = 0.0D;
        // Vanilla ascent: move, subtract gravity, then apply vertical drag.
        for (int tick = 0; tick < 100 && velocity > 0.0D && rise < 3.0D; tick++) {
            rise += velocity;
            velocity = (velocity - gravity) * 0.98D;
        }
        return rise;
    }

    public static boolean canRecover(long now, long lastSpend) {
        return now - lastSpend >= RECOVERY_DELAY;
    }
}
