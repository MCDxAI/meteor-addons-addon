package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.gui.widgets.WAddonList;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

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
        WHorizontalList header = add(theme.horizontalList()).expandX().widget();
        header.add(theme.label(addons.size() + " installed addon" + (addons.size() != 1 ? "s" : ""))).expandX();

        add(theme.horizontalSeparator()).expandX();

        if (addons.isEmpty()) {
            add(theme.label("No Meteor addons installed")).expandX().centerX();
        } else {
            // List of installed addons (no install button needed)
            add(new WAddonList(
                    addons,
                    addon -> () -> mc.setScreen(new AddonDetailScreen(theme, addon, this)),
                    null)).expandX();
        }
    }
}
