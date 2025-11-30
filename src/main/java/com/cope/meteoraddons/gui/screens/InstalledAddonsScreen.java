package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.gui.widgets.WAddonListItem;
import com.cope.meteoraddons.systems.AddonManager;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Screen showing installed Meteor addons in list view.
 */
public class InstalledAddonsScreen extends WindowScreen {
    public InstalledAddonsScreen(GuiTheme theme) {
        super(theme, "Installed Addons");
    }

    @Override
    public void initWidgets() {
        AddonManager manager = AddonManager.get();
        List<Addon> addons = manager.getInstalledAddons();

        // Header
        add(theme.label(addons.size() + " installed addon" + (addons.size() != 1 ? "s" : "")))
            .expandX().centerX();

        add(theme.horizontalSeparator()).expandX();

        if (addons.isEmpty()) {
            add(theme.label("No Meteor addons installed")).expandX().centerX();
        } else {
            // List of installed addons
            WTable addonList = add(theme.table()).expandX().widget();

            for (int i = 0; i < addons.size(); i++) {
                Addon addon = addons.get(i);

                // Add list item (no install button for installed addons)
                addonList.add(new WAddonListItem(addon, a -> {
                    // Open detail screen on click
                    mc.setScreen(new AddonDetailScreen(theme, addon, this));
                }, false)).expandX();

                // Add separator between items (except after last)
                if (i < addons.size() - 1) {
                    addonList.row();
                    addonList.add(theme.horizontalSeparator()).expandX();
                    addonList.row();
                }
            }
        }
    }
}
