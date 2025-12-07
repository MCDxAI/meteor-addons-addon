package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.gui.widgets.WAddonCard;
import com.cope.meteoraddons.gui.widgets.WAddonList;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.AddonSearchUtil;
import com.cope.meteoraddons.util.IconCache;
import com.cope.meteoraddons.util.VersionUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class BrowseAddonsScreen extends WindowScreen {
    private static final int CARDS_PER_ROW = 4;
    private boolean isGridView = false;
    private WContainer contentContainer;
    private WTextBox searchField;
    private String currentSearch = "";

    public BrowseAddonsScreen(GuiTheme theme) {
        super(theme, "Browse Addons");
    }

    @Override
    public void initWidgets() {
        AddonManager manager = AddonManager.get();

        WHorizontalList header = add(theme.horizontalList()).expandX().widget();
        header.add(theme.label("Available Addons")).expandX();
        header.add(theme.label("MC " + VersionUtil.getCurrentMinecraftVersion()).color(theme.textSecondaryColor()));

        add(theme.horizontalSeparator()).expandX();

        if (manager.isLoading()) {
            add(theme.label("Loading addons...")).centerX();
            return;
        }

        if (manager.getLastError() != null) {
            add(theme.label("Error: " + manager.getLastError()).color(theme.textSecondaryColor())).centerX();
            WButton retry = add(theme.button("Retry")).centerX().widget();
            retry.action = () -> {
                manager.fetchAddonMetadata();
                reload();
            };
            return;
        }

        List<Addon> addons = manager.getOnlineAddons();
        if (addons.isEmpty()) {
            add(theme.label("No addons found for this version.")).centerX();
            return;
        }

        // Toolbar: Search + View Toggle
        WHorizontalList toolbar = add(theme.horizontalList()).expandX().widget();
        
        // Search Bar
        searchField = toolbar.add(theme.textBox(currentSearch)).minWidth(200).expandX().widget();
        searchField.setFocused(true);
        searchField.action = () -> {
            currentSearch = searchField.get();
            updateContent(manager.getOnlineAddons());
        };

        // View Toggles
        toolbar.add(theme.horizontalList()).expandX(); // Spacer
        
        WButton listBtn = toolbar.add(theme.button(isGridView ? "List" : "[List]")).widget();
        listBtn.action = () -> { 
            isGridView = false; 
            reload(); 
        };

        WButton gridBtn = toolbar.add(theme.button(isGridView ? "[Grid]" : "Grid")).widget();
        gridBtn.action = () -> { 
            isGridView = true; 
            reload(); 
        };

        add(theme.horizontalSeparator()).expandX();

        // Content Container
        contentContainer = add(theme.verticalList()).expandX().widget();
        updateContent(addons);
    }

    private void updateContent(List<Addon> allAddons) {
        contentContainer.clear();

        List<Addon> filtered = allAddons.stream()
            .filter(addon -> AddonSearchUtil.matches(addon, currentSearch))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            contentContainer.add(theme.label("No addons match your search.")).centerX();
            return;
        }

        if (isGridView) {
            initGridView(contentContainer, filtered);
        } else {
            initListView(contentContainer, filtered);
        }
    }

    private void initGridView(WContainer parent, List<Addon> addons) {
        WTable table = parent.add(theme.table()).expandX().widget();
        int col = 0;
        for (Addon addon : addons) {
            table.add(new WAddonCard(addon, () -> mc.setScreen(new AddonDetailScreen(theme, addon, this))));
            col++;
            if (col >= CARDS_PER_ROW) {
                table.row();
                col = 0;
            }
        }
    }

    private void initListView(WContainer parent, List<Addon> addons) {
        parent.add(new WAddonList(
            addons,
            addon -> () -> mc.setScreen(new AddonDetailScreen(theme, addon, this)),
            addon -> button -> {
                if (addon instanceof OnlineAddon) {
                    button.set("Downloading...");
                    meteordevelopment.meteorclient.utils.network.MeteorExecutor.execute(() -> {
                        boolean success = AddonManager.get().downloadAddon((OnlineAddon) addon);
                        mc.execute(() -> {
                            if (success) {
                                button.set("Downloaded!");
                            } else {
                                button.set("Failed");
                            }
                        });
                    });
                }
            }
        )).expandX();
    }
}