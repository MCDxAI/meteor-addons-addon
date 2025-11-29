package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.CacheManager;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;

/**
 * Widget for displaying an addon card in the grid.
 */
public class WAddonCard extends WVerticalList {
    private static final double FIXED_WIDTH = 180;

    private final AddonMetadata addon;
    private final Runnable onOpenDetails;

    public WAddonCard(AddonMetadata addon, Runnable onOpenDetails) {
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
        // Icon (48x48)
        Texture iconTexture = CacheManager.getIconTexture(addon);
        add(theme.texture(48, 48, 0, iconTexture)).centerX();

        // Title (with wrapping support)
        add(theme.label(addon.name)).centerX().expandX();

        // View details button
        WButton viewButton = add(theme.button("View Details")).expandX().widget();
        viewButton.action = onOpenDetails;
    }
}
