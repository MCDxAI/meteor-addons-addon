package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.CacheManager;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Detail screen for viewing and downloading an addon.
 */
public class AddonDetailScreen extends WindowScreen {
    private final AddonMetadata addon;
    private final Screen parent;

    public AddonDetailScreen(GuiTheme theme, AddonMetadata addon, Screen parent) {
        super(theme, addon.name);
        this.addon = addon;
        this.parent = parent;
    }

    @Override
    public void initWidgets() {
        // Icon
        Texture iconTexture = CacheManager.getIconTexture(addon);
        add(theme.texture(96, 96, 0, iconTexture)).centerX();

        // Name and verified badge
        WTable header = add(theme.table()).expandX().widget();
        header.add(theme.label(addon.name, true)).expandCellX();
        if (addon.verified) {
            header.add(theme.label("✓ Verified"));
        }

        // Description
        String description = addon.getDisplayDescription();
        if (!description.isEmpty()) {
            add(theme.label(description)).expandX();
        }

        // Authors
        if (addon.authors != null && !addon.authors.isEmpty()) {
            WVerticalList authorsList = add(theme.verticalList()).expandX().widget();
            authorsList.add(theme.label("Authors:"));
            for (String author : addon.authors) {
                authorsList.add(theme.label("  • " + author));
            }
        }

        add(theme.horizontalSeparator()).expandX();

        // Stats
        if (addon.repo != null) {
            WTable stats = add(theme.table()).expandX().widget();
            stats.add(theme.label("Stars: "));
            stats.add(theme.label(String.valueOf(addon.repo.stars)));
            stats.row();
            stats.add(theme.label("Downloads: "));
            stats.add(theme.label(String.valueOf(addon.repo.downloads)));
            stats.row();
            stats.add(theme.label("Last Update: "));
            stats.add(theme.label(addon.repo.last_update));
        }

        add(theme.horizontalSeparator()).expandX();

        // Features
        if (addon.features != null) {
            WVerticalList featuresList = add(theme.verticalList()).expandX().widget();

            if (addon.features.modules != null && !addon.features.modules.isEmpty()) {
                featuresList.add(theme.label("Modules (" + addon.features.modules.size() + ")"));
                int count = 0;
                for (String module : addon.features.modules) {
                    if (count++ >= 10) {
                        featuresList.add(theme.label("  ... and " + (addon.features.modules.size() - 10) + " more"));
                        break;
                    }
                    featuresList.add(theme.label("  • " + module));
                }
            }

            if (addon.features.commands != null && !addon.features.commands.isEmpty()) {
                featuresList.add(theme.label("Commands (" + addon.features.commands.size() + ")"));
                for (String command : addon.features.commands) {
                    featuresList.add(theme.label("  • " + command));
                }
            }

            if (addon.features.hud_elements != null && !addon.features.hud_elements.isEmpty()) {
                featuresList.add(theme.label("HUD Elements (" + addon.features.hud_elements.size() + ")"));
                for (String hud : addon.features.hud_elements) {
                    featuresList.add(theme.label("  • " + hud));
                }
            }
        }

        add(theme.horizontalSeparator()).expandX();

        // Links
        WTable links = add(theme.table()).expandX().widget();

        if (addon.links != null && addon.links.github != null) {
            WButton githubButton = links.add(theme.button("GitHub")).expandCellX().widget();
            final String githubUrl = addon.links.github;
            githubButton.action = () -> Util.getOperatingSystem().open(githubUrl);
        }

        String discord = addon.getDiscordUrl();
        if (discord != null) {
            WButton discordButton = links.add(theme.button("Discord")).expandCellX().widget();
            final String discordUrl = discord;
            discordButton.action = () -> Util.getOperatingSystem().open(discordUrl);
        }

        String homepage = addon.getHomepageUrl();
        if (homepage != null) {
            WButton homepageButton = links.add(theme.button("Homepage")).expandCellX().widget();
            final String homepageUrl = homepage;
            homepageButton.action = () -> Util.getOperatingSystem().open(homepageUrl);
        }

        add(theme.horizontalSeparator()).expandX();

        // Download button
        WTable actions = add(theme.table()).expandX().widget();

        boolean isInstalled = AddonManager.get().isInstalled(addon.name);
        if (isInstalled) {
            actions.add(theme.label("✓ Installed")).expandCellX();
        } else {
            WButton downloadButton = actions.add(theme.button("Download")).expandCellX().widget();
            downloadButton.action = () -> {
                downloadButton.set("Downloading...");
                MeteorExecutor.execute(() -> {
                    boolean success = AddonManager.get().downloadAddon(addon);
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

        WButton backButton = actions.add(theme.button("Back")).expandCellX().widget();
        backButton.action = () -> mc.setScreen(parent);
    }
}
