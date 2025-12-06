package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.gui.widgets.WAddonCard;
import com.cope.meteoraddons.gui.widgets.WAddonListItem;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import com.cope.meteoraddons.util.VersionUtil;
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
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class BrowseAddonsScreen extends WindowScreen {
    private static final int CARDS_PER_ROW = 4;
    private boolean isGridView = false;

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

        WHorizontalList toolbar = add(theme.horizontalList()).expandX().widget();
        toolbar.add(theme.label(addons.size() + " addons"));
        toolbar.add(theme.horizontalList()).expandX();

        WButton listBtn = toolbar.add(theme.button(isGridView ? "List" : "[List]")).widget();
        listBtn.action = () -> { isGridView = false; reload(); };

        WButton gridBtn = toolbar.add(theme.button(isGridView ? "[Grid]" : "Grid")).widget();
        gridBtn.action = () -> { isGridView = true; reload(); };

        add(theme.horizontalSeparator()).expandX();

        if (isGridView) {
            initGridView(addons);
        } else {
            initListView(addons);
        }
    }

    private void initGridView(List<Addon> addons) {
        WTable table = add(theme.table()).expandX().widget();
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

    private void initListView(List<Addon> addons) {
        WVerticalList list = add(theme.verticalList()).expandX().widget();
        
        for (int i = 0; i < addons.size(); i++) {
            Addon addon = addons.get(i);
            
            list.add(new WAddonListItem(
                addon,
                () -> mc.setScreen(new AddonDetailScreen(theme, addon, this)),
                (button) -> {
                    if (addon instanceof OnlineAddon) {
                        button.set("Downloading...");
                        meteordevelopment.meteorclient.utils.network.MeteorExecutor.execute(() -> {
                            boolean success = AddonManager.get().downloadAddon((OnlineAddon) addon);
                            mc.execute(() -> {
                                if (success) {
                                    button.set("Downloaded!");
                                    // Optionally refresh logic or disable button could go here
                                } else {
                                    button.set("Failed");
                                }
                            });
                        });
                    }
                }
            )).expandX();

            // Separator
            if (i < addons.size() - 1) {
                list.add(theme.horizontalSeparator()).expandX();
            }
        }
    }
}