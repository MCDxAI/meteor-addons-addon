package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;

/**
 * Widget for displaying an addon card in the grid.
 * Simplified design: Icon + Title + View Details button.
 * Shows installed indicator (green checkmark) overlaid on icon for installed addons.
 */
public class WAddonCard extends WVerticalList {
    private static final double FIXED_WIDTH = 220;

    private final Addon addon;
    private final Runnable onOpenDetails;

    public WAddonCard(Addon addon, Runnable onOpenDetails) {
        this.addon = addon;
        this.onOpenDetails = onOpenDetails;
    }

    @Override
    protected void onCalculateSize() {
        super.onCalculateSize();
        // Force fixed width
        width = theme.scale(FIXED_WIDTH);
    }

    @Override
    public void init() {
        // Icon row - main icon with optional installed indicator
        WHorizontalList iconRow = add(theme.horizontalList()).centerX().widget();

        // Main icon (64x64)
        Texture iconTexture = IconCache.get64(addon);
        iconRow.add(theme.texture(64, 64, 0, iconTexture));

        // Add installed indicator badge if installed
        if (addon.isInstalled()) {
            Texture installedIcon = IconCache.getInstalledIndicator();
            if (installedIcon != null) {
                // Add indicator next to icon (32x32 badge)
                iconRow.add(theme.texture(32, 32, 0, installedIcon)).padLeft(4);
            }
        }

        // Title (with wrapping support)
        add(theme.label(addon.getName())).centerX().expandX();

        // View details button
        WButton viewButton = add(theme.button("View Details")).expandX().widget();
        viewButton.action = onOpenDetails;
    }
}
