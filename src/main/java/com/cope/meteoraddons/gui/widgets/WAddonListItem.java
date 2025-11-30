package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Consumer;

/**
 * List-view widget for displaying addon in full-width layout.
 * Matches reference implementation with 128x128 icon and full details.
 */
public class WAddonListItem extends WTable {
    private static final double BUTTON_COLUMN_WIDTH = 130;

    private final Addon addon;
    private final Consumer<Addon> onAction;
    private final boolean showInstallButton;

    public WAddonListItem(Addon addon, Consumer<Addon> onAction, boolean showInstallButton) {
        this.addon = addon;
        this.onAction = onAction;
        this.showInstallButton = showInstallButton;
    }

    /**
     * Fixed-width vertical list for buttons.
     */
    private static class FixedWidthButtonColumn extends WVerticalList {
        @Override
        protected void onCalculateSize() {
            super.onCalculateSize();
            width = theme.scale(BUTTON_COLUMN_WIDTH);
        }
    }

    @Override
    public void init() {
        // Left side: Icon with optional installed indicator
        WHorizontalList iconRow = add(theme.horizontalList()).widget();

        // Main icon (128x128)
        Texture iconTexture = IconCache.get128(addon);
        iconRow.add(theme.texture(128, 128, 0, iconTexture)).pad(8);

        // Add installed indicator badge if installed
        if (addon.isInstalled()) {
            Texture installedIcon = IconCache.getInstalledIndicator();
            if (installedIcon != null) {
                // Add indicator next to icon (32x32 badge)
                iconRow.add(theme.texture(32, 32, 0, installedIcon)).padRight(8);
            }
        }

        // Right side: Details
        WVerticalList details = add(theme.verticalList()).expandX().widget();

        // Title line with authors
        WHorizontalList titleLine = details.add(theme.horizontalList()).expandX().widget();
        titleLine.add(theme.label(addon.getName(), true)).expandWidgetX();

        List<String> authors = addon.getAuthors();
        if (!authors.isEmpty()) {
            titleLine.add(theme.label(" by ")).expandWidgetX().widget().color(theme.textSecondaryColor());

            for (int i = 0; i < authors.size(); i++) {
                if (i > 0) {
                    titleLine.add(theme.label(i == authors.size() - 1 ? " & " : ", "))
                        .expandWidgetX().widget().color(theme.textSecondaryColor());
                }
                titleLine.add(theme.label(authors.get(i))).expandWidgetX();
            }
        }

        // Description
        addon.getDescription().ifPresent(desc -> {
            details.add(theme.label(desc)).expandX();
        });

        // Version info
        String version = addon.getVersion();
        if (version != null && !version.isEmpty()) {
            details.add(theme.label("Version: " + version)).expandX().widget()
                .color(theme.textSecondaryColor());
        }

        // Vertical separator
        add(theme.verticalSeparator()).expandWidgetY();

        // Right side: Fixed-width button column
        FixedWidthButtonColumn buttons = add(new FixedWidthButtonColumn()).widget();

        // Link buttons
        addon.getHomepageUrl().ifPresent(url -> {
            WButton homeButton = buttons.add(theme.button("Homepage")).expandX().widget();
            homeButton.action = () -> Util.getOperatingSystem().open(url);
        });

        addon.getGithubUrl().ifPresent(url -> {
            WButton githubButton = buttons.add(theme.button("GitHub")).expandX().widget();
            githubButton.action = () -> Util.getOperatingSystem().open(url);
        });

        addon.getDiscordUrl().ifPresent(url -> {
            WButton discordButton = buttons.add(theme.button("Discord")).expandX().widget();
            discordButton.action = () -> Util.getOperatingSystem().open(url);
        });

        // Action button (Install/View Details/etc)
        if (showInstallButton) {
            WButton actionButton = buttons.add(theme.button(addon.isInstalled() ? "Installed" : "Install"))
                .expandX().widget();
            if (!addon.isInstalled()) {
                actionButton.action = () -> onAction.accept(addon);
            }
        } else {
            WButton viewButton = buttons.add(theme.button("View Details"))
                .expandX().widget();
            viewButton.action = () -> onAction.accept(addon);
        }
    }
}
