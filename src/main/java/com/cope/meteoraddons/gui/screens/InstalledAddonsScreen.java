package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.util.Util;

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

                // Column 0: Icon (128x128)
                Texture iconTexture = IconCache.get(addon);
                addonList.add(theme.texture(128, 128, 0, iconTexture)).pad(8);

                // Column 1: Details (expanding)
                WVerticalList details = addonList.add(theme.verticalList()).expandCellX().widget();

                // Title line with authors
                WHorizontalList titleLine = details.add(theme.horizontalList()).expandX().widget();
                titleLine.add(theme.label(addon.getName(), true));

                List<String> authors = addon.getAuthors();
                if (!authors.isEmpty()) {
                    titleLine.add(theme.label(" by ")).widget().color = theme.textSecondaryColor();
                    for (int j = 0; j < authors.size(); j++) {
                        if (j > 0) {
                            titleLine.add(theme.label(j == authors.size() - 1 ? " & " : ", ")).widget().color = theme.textSecondaryColor();
                        }
                        titleLine.add(theme.label(authors.get(j)));
                    }
                }

                // Description
                addon.getDescription().ifPresent(desc -> {
                    details.add(theme.label(desc)).expandX();
                });

                // Version
                String version = addon.getVersion();
                if (version != null && !version.isEmpty()) {
                    details.add(theme.label("Version: " + version)).expandX().widget().color = theme.textSecondaryColor();
                }

                // Column 2: Vertical separator
                addonList.add(theme.verticalSeparator()).expandWidgetY();

                // Column 3: Buttons
                WVerticalList buttons = addonList.add(theme.verticalList()).widget();

                addon.getHomepageUrl().ifPresent(url -> {
                    WButton btn = buttons.add(theme.button("Homepage")).expandX().widget();
                    btn.action = () -> Util.getOperatingSystem().open(url);
                });

                addon.getGithubUrl().ifPresent(url -> {
                    WButton btn = buttons.add(theme.button("GitHub")).expandX().widget();
                    btn.action = () -> Util.getOperatingSystem().open(url);
                });

                WButton viewDetails = buttons.add(theme.button("View Details")).expandX().widget();
                viewDetails.action = () -> mc.setScreen(new AddonDetailScreen(theme, addon, this));

                // End of this addon's row
                addonList.row();
            }
        }
    }
}
