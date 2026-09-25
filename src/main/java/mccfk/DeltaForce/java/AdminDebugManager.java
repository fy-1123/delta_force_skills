package mccfk.DeltaForce.java;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 管理员调试验证状态管理器
 * 控制无敌、无限体力、技能无冷却三个调试开关
 * 受 DeltaForce.isAllowAdminDebug() 总开关控制，默认关闭
 * MC_CaoFangKuai 及 OP>=1 的玩家均适用
 */
public final class AdminDebugManager {

    private static boolean invincible = false;
    private static boolean noStaminaCost = false;
    private static boolean noCooldown = false;

    public AdminDebugManager() {
    }

    /**
     * 检查管理员调试总开关与具体功能开关是否同时开启
     */
    public static boolean isInvincible() {
        return DeltaForce.isAllowAdminDebug() && invincible;
    }

    public static void setInvincible(boolean value) {
        invincible = value;
    }

    public static boolean isNoStaminaCost() {
        return DeltaForce.isAllowAdminDebug() && noStaminaCost;
    }

    public static void setNoStaminaCost(boolean value) {
        noStaminaCost = value;
    }

    public static boolean isNoCooldown() {
        return DeltaForce.isAllowAdminDebug() && noCooldown;
    }

    public static void setNoCooldown(boolean value) {
        noCooldown = value;
    }

    /**
     * 检查玩家是否为管理员或OP>=1（调试功能适用对象）
     */
    private static boolean isAdminOrOp(Player player) {
        if (player == null) return false;
        if ("MC_CaoFangKuai".equals(player.getName().getString())) return true;
        if (player instanceof ServerPlayer sp && sp.getServer() != null
                && sp.getServer().getPlayerList().isOp(player.getGameProfile())) return true;
        return false;
    }

    /**
     * 检查指定玩家是否启用了管理员无冷却调试
     */
    public static boolean isNoCooldown(Player player) {
        return DeltaForce.isAllowAdminDebug() && noCooldown && isAdminOrOp(player);
    }

    /**
     * 管理员无敌：阻断管理员/OP受到的所有伤害
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!isInvincible()) return;
        if (event.getEntity() instanceof Player player && isAdminOrOp(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * 管理员无敌：阻止管理员/OP死亡
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!isInvincible()) return;
        if (event.getEntity() instanceof Player player && isAdminOrOp(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * 管理员无限体力：每tick回满管理员/OP的体力
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!isNoStaminaCost()) return;
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isAdminOrOp(player)) {
                com.rzy.dealt_force_skills.stamina.StaminaManager.fill(player);
            }
        }
    }
}