package mccfk.DeltaForce.java.client.dfgame;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.dfgame.DFGameManager;

/**
 * DFGAME 伤害/治疗事件处理器
 * DFGAME 开启时拦截原版伤害/治疗，按 1:1 计入自定义血量系统（0-100）
 */
public class DFGameDamageEventHandler {

    /**
     * 监听玩家受伤事件
     * DFGAME 开启时，将原版伤害转换为自定义系统伤害
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 创造模式例外
        if (player.getAbilities().instabuild) {
            return;
        }

        // 获取当前自定义血量
        String playerName = player.getName().getString();
        double currentHealth = DFGameManager.getHealth(playerName);

        // 伤害直接按 1:1 扣到自定义血量（0-100）
        double customDamage = event.getAmount();
        double newHealth = currentHealth - customDamage;

        if (newHealth <= 0) {
            // 血量归零：放行原版伤害，触发原版死亡流程
            // （物品掉落、死亡界面、重生点重生，不再打开自定义界面/原地复活）
            DFGameManager.setHealth(playerName, 0);
            // 压低原版血量，保证本次伤害（即使经过护甲减免）足以致命
            if (player.getHealth() > 0.01f) {
                player.setHealth(0.01f);
            }
            return; // 不取消事件，让原版伤害流程完成致命一击
        }

        // 未死亡：更新自定义血量并取消原版伤害处理（防止双重扣血）
        DFGameManager.setHealth(playerName, newHealth);
        event.setCanceled(true);

        // 播放受伤音效和动画
        player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
        player.level().broadcastEntityEvent(player, (byte) 2); // 受伤粒子效果
    }

    /**
     * 监听玩家治疗事件
     * DFGAME 开启时，将原版治疗转换为自定义系统治疗
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!DeltaForce.isDFGameEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 创造模式例外
        if (player.getAbilities().instabuild) {
            return;
        }

        // 获取当前自定义血量
        String playerName = player.getName().getString();
        double currentHealth = DFGameManager.getHealth(playerName);

        // 治疗直接按 1:1 加回自定义血量
        double customHeal = event.getAmount();
        double newHealth = currentHealth + customHeal;

        // 更新自定义血量
        DFGameManager.setHealth(playerName, newHealth);

        // 取消原版治疗处理（防止双重加血）
        event.setCanceled(true);
    }
}