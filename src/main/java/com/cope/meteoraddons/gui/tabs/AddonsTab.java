package com.cope.meteoraddons.gui.tabs;

import com.cope.meteoraddons.gui.screens.BrowseAddonsScreen;
import com.cope.meteoraddons.gui.screens.InstalledAddonsScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * GUI tab for browsing and managing Meteor addons.
 * Provides navigation between Installed and Browse screens.
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
        public AddonsTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            // Title
            add(theme.label("Meteor Addons")).expandX().centerX();

            add(theme.horizontalSeparator()).expandX();

            // Horizontal navigation buttons
            WHorizontalList nav = add(theme.horizontalList()).expandX().centerX().widget();

            WButton installedButton = nav.add(theme.button("Installed Addons")).minWidth(200).widget();
            installedButton.action = () -> mc.setScreen(new InstalledAddonsScreen(theme));

            WButton browseButton = nav.add(theme.button("Browse Addons")).minWidth(200).widget();
            browseButton.action = () -> mc.setScreen(new BrowseAddonsScreen(theme));

            add(theme.horizontalSeparator()).expandX();

            // Info text
            add(theme.label("Manage your Meteor Client addons")).expandX().centerX();
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
