package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import com.cope.meteoraddons.gui.widgets.WAddonListItem;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

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
            // List of installed addons
            WVerticalList list = add(theme.verticalList()).expandX().widget();

            for (int i = 0; i < addons.size(); i++) {
                Addon addon = addons.get(i);
                
                // Pass null for onInstall since we are already in the installed list, 
                // but the widget will still show the "Installed" badge which provides consistency.
                list.add(new WAddonListItem(
                    addon,
                    () -> mc.setScreen(new AddonDetailScreen(theme, addon, this)),
                    null
                )).expandX();

                // Separator
                if (i < addons.size() - 1) {
                    list.add(theme.horizontalSeparator()).expandX();
                }
            }
        }
    }
}
