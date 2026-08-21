package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.shop.GhrothArmoryCatalog;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager;
import com.rzy.dealt_force_skills.shop.UndeadSoulManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2S_OpenOwnShop {
    public static C2S_OpenOwnShop open() {
        return new C2S_OpenOwnShop();
    }

    public static void encode(C2S_OpenOwnShop message, FriendlyByteBuf buffer) {
    }

    public static C2S_OpenOwnShop decode(FriendlyByteBuf buffer) {
        return new C2S_OpenOwnShop();
    }

    public static void handle(C2S_OpenOwnShop message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> openForCurrentCharacter(player));
        context.setPacketHandled(true);
    }

    private static void openForCurrentCharacter(ServerPlayer player) {
        if (player == null || player.isSpectator()) {
            return;
        }
        if (UndeadStateManager.isUndead(player)) {
            NetworkHandler.sendToPlayer(new S2C_OpenUndeadShop(
                    UndeadSoulManager.get(player),
                    UndeadStateManager.profession(player).ordinal(),
                    UndeadUpgradeManager.snapshot(player)), player);
        } else if (LexNinjiaStateManager.isLexNinjia(player)) {
            LexNinjiaStateManager.initializeIfNeeded(player);
            NetworkHandler.sendToPlayer(new S2C_OpenLexNinjiaShop(
                    LexNinjiaCurrencyManager.get(player),
                    LexNinjiaStateManager.shopData(player)), player);
        } else if (SaeedStateManager.isSaeed(player)) {
            SaeedStateManager.initializeIfNeeded(player);
            NetworkHandler.sendToPlayer(new S2C_OpenSaeedRecruitScreen(
                    SaeedStateManager.needsInitialGuardChoice(player)
                            ? SaeedStateManager.initialGuardData(player)
                            : SaeedStateManager.shopData(player)), player);
        } else if (GhrothStateManager.isGhroth(player)) {
            NetworkHandler.sendToPlayer(new S2C_OpenGhrothArmory(
                    HaffCoinManager.get(player), GhrothArmoryCatalog.entries()), player);
        }
    }
}
