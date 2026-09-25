package com.rzy.dealt_force_skills.compat.sophisticated;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControlBase.MatchButton;

public final class AutoSellUpgradeTab extends UpgradeSettingsTab<AutoSellUpgradeContainer> {
    private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filter;

    public AutoSellUpgradeTab(AutoSellUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("upgrade.dealt_force_skills.auto_sell.title"),
                Component.translatable("upgrade.dealt_force_skills.auto_sell.description"));
        filter = addHideableChild(new FilterLogicControl<>(screen, new Position(position.x() + 3, position.y() + 24),
                container.filter(), 9, MatchButton.PRIMARY_MATCH, MatchButton.DURABILITY, MatchButton.NBT));
    }

    @Override protected void moveSlotsToTab() { filter.moveSlotsToView(); }

    public static void register() { UpgradeGuiManager.registerTab(AutoSellUpgradeContainer.TYPE, AutoSellUpgradeTab::new); }
}
