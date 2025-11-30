package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.gui.widgets.WAddonCard;
import com.cope.meteoraddons.gui.widgets.WAddonListItem;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import com.cope.meteoraddons.util.VersionUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Screen for browsing and downloading online addons.
 * Supports both list view (default) and grid view.
 */
public class BrowseAddonsScreen extends WindowScreen {
    private static final int CARDS_PER_ROW = 5;
    private boolean isGridView = false;

    public BrowseAddonsScreen(GuiTheme theme) {
        super(theme, "Browse Addons");
    }

    @Override
    public void initWidgets() {
        AddonManager manager = AddonManager.get();

        // Header
        WVerticalList header = add(theme.verticalList()).expandX().widget();
        header.add(theme.label("Meteor Addons Browser")).centerX();
        header.add(theme.label("Minecraft " + VersionUtil.getCurrentMinecraftVersion())).centerX();

        // Loading status
        if (manager.isLoading()) {
            header.add(theme.label("Loading addon metadata...")).centerX();
            add(theme.horizontalSeparator()).expandX();
            return;
        }

        // Error display
        if (manager.getLastError() != null) {
            header.add(theme.label("Error: " + manager.getLastError())).centerX();
            WButton retryButton = header.add(theme.button("Retry")).centerX().widget();
            retryButton.action = () -> {
                manager.fetchAddonMetadata();
                reload();
            };
            add(theme.horizontalSeparator()).expandX();
            return;
        }

        List<Addon> addons = manager.getOnlineAddons();

        // Show addon count
        header.add(theme.label(addons.size() + " addons available")).centerX();

        // View toggle buttons (List View first)
        WHorizontalList viewToggle = header.add(theme.horizontalList()).centerX().widget();
        WButton listButton = viewToggle.add(theme.button(isGridView ? "List View" : "[List View]")).widget();
        WButton gridButton = viewToggle.add(theme.button(isGridView ? "[Grid View]" : "Grid View")).widget();

        gridButton.action = () -> {
            if (!isGridView) {
                isGridView = true;
                reload();
            }
        };

        listButton.action = () -> {
            if (isGridView) {
                isGridView = false;
                reload();
            }
        };

        add(theme.horizontalSeparator()).expandX();

        // No addons available
        if (addons.isEmpty()) {
            add(theme.label("No addons available for this Minecraft version")).expandX().centerX();
            return;
        }

        // Display addons based on view mode
        if (isGridView) {
            displayGridView(addons);
        } else {
            displayListView(addons);
        }
    }

    private void displayGridView(List<Addon> addons) {
        WTable grid = add(theme.table()).expandX().widget();

        int column = 0;
        for (Addon addon : addons) {
            WAddonCard card = grid.add(new WAddonCard(addon, () -> {
                mc.setScreen(new AddonDetailScreen(theme, addon, this));
            })).widget();

            column++;
            if (column >= CARDS_PER_ROW) {
                grid.row();
                column = 0;
            }
        }
    }

    private void displayListView(List<Addon> addons) {
        WTable list = add(theme.table()).expandX().widget();

        for (int i = 0; i < addons.size(); i++) {
            Addon addon = addons.get(i);

            list.add(new WAddonListItem(addon, a -> {
                mc.setScreen(new AddonDetailScreen(theme, a, this));
            }, true, true)).expandX();

            // Add separator between items (except after last)
            if (i < addons.size() - 1) {
                list.row();
                list.add(theme.horizontalSeparator()).expandX();
                list.row();
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
