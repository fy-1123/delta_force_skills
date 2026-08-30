package mccfk.DeltaForce.java.dfgame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DFGAME 游戏管理器
 * 管理玩家的血量、体力等状态（DFGAME 开启时生效）
 */
public class DFGameManager {

    // 玩家血量映射（玩家UUID -> 血量，0-100，精确到小数点后2位）
    private static final Map<String, Double> playerHealth = new ConcurrentHashMap<>();

    // 玩家体力映射（玩家UUID -> 体力，0-100，暂不实现功能）
    private static final Map<String, Double> playerStamina = new ConcurrentHashMap<>();

    // 最大血量
    public static final double MAX_HEALTH = 100.0;

    /**
     * 获取玩家血量
     * @param playerName 玩家名
     * @return 血量（0-100），未设置返回100.0
     */
    public static double getHealth(String playerName) {
        return playerHealth.getOrDefault(playerName, MAX_HEALTH);
    }

    /**
     * 设置玩家血量
     * @param playerName 玩家名
     * @param health 血量（0-100）
     */
    public static void setHealth(String playerName, double health) {
        if (health < 0) health = 0;
        if (health > MAX_HEALTH) health = MAX_HEALTH;
        // 保留小数点后2位
        health = Math.round(health * 100.0) / 100.0;
        playerHealth.put(playerName, health);
    }

    /**
     * 获取玩家体力
     * @param playerName 玩家名
     * @return 体力（0-100），未设置返回100.0
     */
    public static double getStamina(String playerName) {
        return playerStamina.getOrDefault(playerName, MAX_HEALTH);
    }

    /**
     * 设置玩家体力
     * @param playerName 玩家名
     * @param stamina 体力（0-100）
     */
    public static void setStamina(String playerName, double stamina) {
        if (stamina < 0) stamina = 0;
        if (stamina > MAX_HEALTH) stamina = MAX_HEALTH;
        stamina = Math.round(stamina * 100.0) / 100.0;
        playerStamina.put(playerName, stamina);
    }

    /**
     * 清空玩家数据（玩家退出时调用）
     */
    public static void clearPlayerData(String playerName) {
        playerHealth.remove(playerName);
        playerStamina.remove(playerName);
    }
}