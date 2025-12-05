package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;

/**
 * Addon card widget for grid view.
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
        width = theme.scale(FIXED_WIDTH);
    }

    @Override
    public void init() {
        Texture iconTexture = IconCache.get(addon);
        add(theme.texture(64, 64, 0, iconTexture)).centerX();

        WHorizontalList titleRow = add(theme.horizontalList()).centerX().widget();
        titleRow.add(theme.label(addon.getName()));

        if (addon.isInstalled()) {
            Texture installedIcon = IconCache.getInstalledIndicator();
            if (installedIcon != null) {
                double size = theme.textHeight();
                titleRow.add(theme.texture(size, size, 0, installedIcon)).padLeft(4);
            }
        }

        WButton viewButton = add(theme.button("View Details")).expandX().widget();
        viewButton.action = onOpenDetails;
    }
}
