package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;

import java.util.List;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class WAddonListItem extends WHorizontalList {
    private final Addon addon;
    private final Runnable onViewDetails;
    private final Consumer<WButton> onInstall;

    public WAddonListItem(Addon addon, Runnable onViewDetails, Consumer<WButton> onInstall) {
        this.addon = addon;
        this.onViewDetails = onViewDetails;
        this.onInstall = onInstall;
    }

    @Override
    public void init() {
        // Icon
        Texture icon = IconCache.get(addon);
        add(theme.texture(IconSizeConfig.ADDON_ICON_SIZE, IconSizeConfig.ADDON_ICON_SIZE, 0, icon)).widget();

        // Details Column
        WVerticalList details = add(theme.verticalList()).expandX().widget();

        // Name & Version Line
        WHorizontalList infoLine = details.add(theme.horizontalList()).widget();
        infoLine.add(theme.label(addon.getName(), true));

        String version = addon.getVersion();
        if (version != null && !version.isEmpty() && !version.equals("Unknown")) {
            infoLine.add(theme.label(version).color(theme.textSecondaryColor()));
        }

        if (addon instanceof OnlineAddon) {
            AddonMetadata meta = ((OnlineAddon) addon).getMetadata();
            if (meta.verified) {
                infoLine.add(theme.label("✓").color(theme.textColor()));
            }
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

        // Actions Column (Right aligned)
        WVerticalList actions = add(theme.verticalList()).widget();

        // Install / Installed Status
        if (addon.isInstalled()) {
            WHorizontalList installedContainer = actions.add(theme.horizontalList()).right().widget();
            installedContainer.add(theme.label("Installed"));
            Texture installedIcon = IconCache.getInstalledIndicator();
            if (installedIcon != null) {
                double size = theme.textHeight();
                installedContainer.add(theme.texture(size, size, 0, installedIcon)).padLeft(4);
            }
        } else if (onInstall != null) {
            WButton install = actions.add(theme.button("Install")).right().widget();
            install.action = () -> onInstall.accept(install);
        }

        // Details Button
        WButton detailsBtn = actions.add(theme.button("Details")).right().widget();
        detailsBtn.action = onViewDetails;
    }
}
