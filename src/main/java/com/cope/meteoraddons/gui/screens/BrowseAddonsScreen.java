package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.gui.widgets.WAddonCard;
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

/**
 * Screen for browsing and downloading online addons.
 */
public class BrowseAddonsScreen extends WindowScreen {
    private static final int CARDS_PER_ROW = 4;
    private boolean isGridView = false;

    public BrowseAddonsScreen(GuiTheme theme) {
        super(theme, "Browse Addons");
    }

    @Override
    public void initWidgets() {
        AddonManager manager = AddonManager.get();

        // Header
        WHorizontalList header = add(theme.horizontalList()).expandX().widget();
        header.add(theme.label("Available Addons")).expandX();
        header.add(theme.label("MC " + VersionUtil.getCurrentMinecraftVersion()).color(theme.textSecondaryColor()));

        add(theme.horizontalSeparator()).expandX();

        // Loading / Error
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

        // Toolbar
        WHorizontalList toolbar = add(theme.horizontalList()).expandX().widget();
        toolbar.add(theme.label(addons.size() + " addons"));
        
        // Spacer using empty expanding label or just layout properties
        // Meteor doesn't have a spacer widget usually, but expanding a label works or expanding the container.
        // We'll rely on the previous label expanding? No, WLabel auto-sizes. 
        // We can add a dummy widget that expands.
        toolbar.add(theme.horizontalList()).expandX();
        
        WButton listBtn = toolbar.add(theme.button(isGridView ? "List" : "[List]")).widget();
        listBtn.action = () -> { isGridView = false; reload(); };
        
        WButton gridBtn = toolbar.add(theme.button(isGridView ? "[Grid]" : "Grid")).widget();
        gridBtn.action = () -> { isGridView = true; reload(); };

        add(theme.horizontalSeparator()).expandX();

        // Content
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
            WHorizontalList row = list.add(theme.horizontalList()).expandX().widget();
            
            // Icon
            Texture icon = IconCache.get(addon);
            row.add(theme.texture(48, 48, 0, icon)).widget(); 
            
            // Details
            WVerticalList details = row.add(theme.verticalList()).expandX().widget();
            
            // Name & Version
            WHorizontalList infoLine = details.add(theme.horizontalList()).widget();
            infoLine.add(theme.label(addon.getName(), true));
            
            String version = addon.getVersion();
            if (version != null) infoLine.add(theme.label(version).color(theme.textSecondaryColor()));

            if (addon instanceof OnlineAddon) {
                AddonMetadata meta = ((OnlineAddon) addon).getMetadata();
                if (meta.verified) infoLine.add(theme.label("✓").color(theme.textColor()));
            }

            // Description
            addon.getDescription().ifPresent(desc -> {
                 details.add(theme.label(desc, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));
            });
            
            // Authors
            List<String> authors = addon.getAuthors();
            if (!authors.isEmpty()) {
                details.add(theme.label("By: " + String.join(", ", authors)).color(theme.textSecondaryColor()));
            }
            
            // Action Buttons (Right aligned)
            WVerticalList actions = row.add(theme.verticalList()).widget();
            
            if (addon.isInstalled()) {
                WHorizontalList installedContainer = actions.add(theme.horizontalList()).right().widget();
                installedContainer.add(theme.label("Installed"));
                Texture installedIcon = IconCache.getInstalledIndicator();
                if (installedIcon != null) {
                    double size = theme.textHeight();
                    installedContainer.add(theme.texture(size, size, 0, installedIcon)).padLeft(4);
                }
            } else {
                WButton install = actions.add(theme.button("Install")).right().widget();
                install.action = () -> mc.setScreen(new AddonDetailScreen(theme, addon, this));
            }
            
            WButton detailsBtn = actions.add(theme.button("Details")).right().widget();
            detailsBtn.action = () -> mc.setScreen(new AddonDetailScreen(theme, addon, this));

            // Separator
            if (i < addons.size() - 1) {
                list.add(theme.horizontalSeparator()).expandX();
            }
        }
    }
}