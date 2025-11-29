package com.cope.meteoraddons.gui.tabs;

import com.cope.meteoraddons.gui.screens.AddonDetailScreen;
import com.cope.meteoraddons.gui.widgets.WAddonCard;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.VersionUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * GUI tab for browsing and managing Meteor addons.
 * Displays addons in a grid layout with cards.
 */
public class AddonsTab extends Tab {
    public AddonsTab() {
        super("Addons");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new AddonsTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof AddonsTabScreen;
    }

    private static class AddonsTabScreen extends WindowTabScreen {
        private static final int CARDS_PER_ROW = 5;
        private static final int CARD_WIDTH = 180;

        public AddonsTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            AddonManager manager = AddonManager.get();

            // Header info
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

            List<AddonMetadata> addons = manager.getFilteredAddons();

            // Show addon count
            header.add(theme.label(addons.size() + " addons available")).centerX();

            // Icon loading status
            if (manager.isLoadingIcons()) {
                header.add(theme.label("Downloading icons...")).centerX();
            }

            add(theme.horizontalSeparator()).expandX();

            // No addons available
            if (addons.isEmpty()) {
                add(theme.label("No addons available for this Minecraft version")).expandX().centerX();
                return;
            }

            // Addon grid
            WTable grid = add(theme.table()).expandX().widget();

            int column = 0;
            for (AddonMetadata addon : addons) {
                // Create addon card with fixed width for uniformity
                WAddonCard card = grid.add(new WAddonCard(addon, () -> {
                    // Open detail screen
                    mc.setScreen(new AddonDetailScreen(theme, addon, this));
                })).minWidth(CARD_WIDTH).widget();

                column++;
                if (column >= CARDS_PER_ROW) {
                    grid.row();
                    column = 0;
                }
            }
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
