package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import com.cope.meteoraddons.util.TimeUtil;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Detail screen for viewing and downloading an addon.
 */
public class AddonDetailScreen extends WindowScreen {
    private final Addon addon;
    private final Screen parent;

    public AddonDetailScreen(GuiTheme theme, Addon addon, Screen parent) {
        super(theme, addon.getName());
        this.addon = addon;
        this.parent = parent;
    }

    @Override
    public void initWidgets() {
        // Header Section (Icon + Details)
        WHorizontalList header = add(theme.horizontalList()).expandX().widget();
        
        // Icon
        Texture iconTexture = IconCache.get(addon);
        header.add(theme.texture(IconSizeConfig.ADDON_ICON_SIZE, IconSizeConfig.ADDON_ICON_SIZE, 0, iconTexture)).widget();

        // Details (Name, Authors, Version, Verified)
        WVerticalList details = header.add(theme.verticalList()).expandX().widget();
        
        WHorizontalList titleRow = details.add(theme.horizontalList()).widget();
        titleRow.add(theme.label(addon.getName(), true)); // Title
        
        if (addon.isInstalled()) {
            Texture installedIcon = IconCache.getInstalledIndicator();
            if (installedIcon != null) {
                double size = theme.textHeight(true);
                titleRow.add(theme.texture(size, size, 0, installedIcon)).padLeft(4);
            }
            titleRow.add(theme.label("(Installed)", true).color(theme.textSecondaryColor())).padLeft(4);
        }

        if (addon instanceof OnlineAddon) {
            AddonMetadata metadata = ((OnlineAddon) addon).getMetadata();
            if (metadata.verified) {
                details.add(theme.label("Verified").color(theme.textSecondaryColor()));
            }
        }
        
        String version = addon.getVersion();
        if (version != null && !version.isEmpty()) {
            details.add(theme.label("Version: " + version).color(theme.textSecondaryColor()));
        }

        List<String> authors = addon.getAuthors();
        if (!authors.isEmpty()) {
            details.add(theme.label("By: " + String.join(", ", authors)).color(theme.textSecondaryColor()));
        }

        add(theme.horizontalSeparator()).expandX();

        // Description
        addon.getDescription().ifPresent(desc -> {
            add(theme.label(desc, getWindowWidth() / 2.0));
        });
        
        // Stats (Online Addons only)
        if (addon instanceof OnlineAddon) {
             AddonMetadata metadata = ((OnlineAddon) addon).getMetadata();
             if (metadata.repo != null) {
                 WHorizontalList stats = add(theme.horizontalList()).centerX().widget();
                 stats.add(theme.label("Stars: " + metadata.repo.stars));
                 stats.add(theme.label("Downloads: " + metadata.repo.downloads)).padHorizontal(10);
                 stats.add(theme.label("Updated: " + TimeUtil.getRelativeTime(metadata.repo.last_update)));
             }
             
             // Features Section
             if (metadata.features != null) {
                 boolean hasFeatures = (metadata.features.modules != null && !metadata.features.modules.isEmpty()) ||
                                       (metadata.features.commands != null && !metadata.features.commands.isEmpty()) ||
                                       (metadata.features.hud_elements != null && !metadata.features.hud_elements.isEmpty()) ||
                                       (metadata.features.custom_screens != null && !metadata.features.custom_screens.isEmpty());
                                       
                 if (hasFeatures) {
                     WSection featuresSection = add(theme.section("Features", true)).expandX().widget();
                     
                     boolean first = true;
                     
                     if (metadata.features.modules != null && !metadata.features.modules.isEmpty()) {
                        featuresSection.add(theme.label("Modules (" + metadata.features.modules.size() + "):"));
                        String modulesStr = String.join(", ", metadata.features.modules);
                        featuresSection.add(theme.label(modulesStr, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));
                        first = false;
                     }
                     
                     if (metadata.features.commands != null && !metadata.features.commands.isEmpty()) {
                        if (!first) featuresSection.add(theme.horizontalSeparator()).expandX();
                        featuresSection.add(theme.label("Commands (" + metadata.features.commands.size() + "):"));
                        String cmdStr = String.join(", ", metadata.features.commands);
                        featuresSection.add(theme.label(cmdStr, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));
                        first = false;
                     }
                     
                      if (metadata.features.hud_elements != null && !metadata.features.hud_elements.isEmpty()) {
                        if (!first) featuresSection.add(theme.horizontalSeparator()).expandX();
                        featuresSection.add(theme.label("HUD (" + metadata.features.hud_elements.size() + "):"));
                        String hudStr = String.join(", ", metadata.features.hud_elements);
                        featuresSection.add(theme.label(hudStr, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));
                        first = false;
                     }

                      if (metadata.features.custom_screens != null && !metadata.features.custom_screens.isEmpty()) {
                        if (!first) featuresSection.add(theme.horizontalSeparator()).expandX();
                        featuresSection.add(theme.label("Screens (" + metadata.features.custom_screens.size() + "):"));
                        String screensStr = String.join(", ", metadata.features.custom_screens);
                        featuresSection.add(theme.label(screensStr, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));
                        first = false;
                     }
                 }
             }
        }

        add(theme.horizontalSeparator()).expandX();

        // Actions (Buttons)
        WHorizontalList actions = add(theme.horizontalList()).right().widget();

        // Download/Install Button
        if (!addon.isInstalled() && addon instanceof OnlineAddon) {
            WButton downloadButton = actions.add(theme.button("Download")).widget();
            downloadButton.action = () -> {
                downloadButton.set("Downloading...");
                MeteorExecutor.execute(() -> {
                    boolean success = AddonManager.get().downloadAddon((OnlineAddon) addon);
                    mc.execute(() -> {
                        if (success) {
                            downloadButton.set("Downloaded!");
                        } else {
                            downloadButton.set("Download Failed");
                        }
                    });
                });
            };
        }

        // Link Buttons
        if (addon.getGithubUrl().isPresent()) {
             WButton btn = actions.add(theme.button("GitHub")).widget();
             final String url = addon.getGithubUrl().get();
             btn.action = () -> Util.getOperatingSystem().open(url);
        }
        if (addon.getDiscordUrl().isPresent()) {
             WButton btn = actions.add(theme.button("Discord")).widget();
             final String url = addon.getDiscordUrl().get();
             btn.action = () -> Util.getOperatingSystem().open(url);
        }
        if (addon.getHomepageUrl().isPresent()) {
             WButton btn = actions.add(theme.button("Homepage")).widget();
             final String url = addon.getHomepageUrl().get();
             btn.action = () -> Util.getOperatingSystem().open(url);
        }

        // Back Button
        WButton backButton = actions.add(theme.button("Back")).widget();
        backButton.action = () -> mc.setScreen(parent);
    }
}
