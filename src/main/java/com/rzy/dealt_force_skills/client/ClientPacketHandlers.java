package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.network.S2C_SpecialOperationsBaseSnapshot;
import com.rzy.dealt_force_skills.network.S2C_StartPageSnapshot;
import com.rzy.dealt_force_skills.client.screen.CommanderScreen;
import com.rzy.dealt_force_skills.client.screen.GtiTradingHallScreen;
import com.rzy.dealt_force_skills.client.screen.SpecialOperationsBaseScreen;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import mccfk.DeltaForce.java.client.screen.StartGameScreen;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import mccfk.DeltaForce.java.network.S2C_DfWarehouseSnapshot;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Client-only targets for network callbacks. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void characterHitFeedback(int hurtEntityId, float amount) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(hurtEntityId) != null) {
            CharacterHitFeedbackOverlay.markHit(amount);
        }
    }

    public static void openCommanderScreen(boolean support) {
        Minecraft.getInstance().setScreen(new CommanderScreen(support));
    }

    public static void openGtiTradingHall(long coins, List<GtiTradingHallManager.Entry> entries) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DFGUI_MAIN main) {
            main.showGtiMarket(coins, entries);
        } else {
            GtiTradingHallScreen.open(coins, entries);
        }
    }

    public static void openSpecialOperationsBase(S2C_SpecialOperationsBaseSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DFGUI_MAIN main) {
            main.showGtiBase(snapshot);
        } else {
            SpecialOperationsBaseScreen.accept(snapshot);
        }
    }

    public static void acceptStartPageSnapshot(S2C_StartPageSnapshot snapshot) {
        StartGameScreen.acceptSnapshot(snapshot);
    }

    public static void openWarehouse(S2C_DfWarehouseSnapshot snapshot) {
        if (Minecraft.getInstance().screen instanceof DFGUI_MAIN main) {
            main.showWarehouse(snapshot.page(), snapshot.container(), snapshot.pageSlots(), snapshot.warehouse());
        }
    }
}
