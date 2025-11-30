package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

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
        // Icon
        Texture iconTexture = IconCache.get(addon);
        add(theme.texture(96, 96, 0, iconTexture)).centerX();

        // Name
        WTable header = add(theme.table()).expandX().widget();
        header.add(theme.label(addon.getName(), true)).expandCellX();

        // Show verified badge for online addons
        if (addon instanceof OnlineAddon) {
            AddonMetadata metadata = ((OnlineAddon) addon).getMetadata();
            if (metadata.verified) {
                header.add(theme.label("✓ Verified"));
            }
        }

        // Description
        addon.getDescription().ifPresent(desc -> {
            add(theme.label(desc)).expandX();
        });

        // Authors
        List<String> authors = addon.getAuthors();
        if (!authors.isEmpty()) {
            WVerticalList authorsList = add(theme.verticalList()).expandX().widget();
            authorsList.add(theme.label("Authors:"));
            for (String author : authors) {
                authorsList.add(theme.label("  • " + author));
            }
        }

        // Version
        String version = addon.getVersion();
        if (version != null && !version.isEmpty()) {
            add(theme.label("Version: " + version)).expandX();
        }

        add(theme.horizontalSeparator()).expandX();

        // Stats (only for online addons)
        if (addon instanceof OnlineAddon) {
            AddonMetadata metadata = ((OnlineAddon) addon).getMetadata();
            if (metadata.repo != null) {
                WTable stats = add(theme.table()).expandX().widget();
                stats.add(theme.label("Stars: "));
                stats.add(theme.label(String.valueOf(metadata.repo.stars)));
                stats.row();
                stats.add(theme.label("Downloads: "));
                stats.add(theme.label(String.valueOf(metadata.repo.downloads)));
                stats.row();
                stats.add(theme.label("Last Update: "));
                stats.add(theme.label(metadata.repo.last_update));

                add(theme.horizontalSeparator()).expandX();
            }

            // Features
            if (metadata.features != null) {
                WVerticalList featuresList = add(theme.verticalList()).expandX().widget();

                if (metadata.features.modules != null && !metadata.features.modules.isEmpty()) {
                    featuresList.add(theme.label("Modules (" + metadata.features.modules.size() + ")"));
                    int count = 0;
                    for (String module : metadata.features.modules) {
                        if (count++ >= 10) {
                            featuresList.add(theme.label("  ... and " + (metadata.features.modules.size() - 10) + " more"));
                            break;
                        }
                        featuresList.add(theme.label("  • " + module));
                    }
                }

                if (metadata.features.commands != null && !metadata.features.commands.isEmpty()) {
                    featuresList.add(theme.label("Commands (" + metadata.features.commands.size() + ")"));
                    for (String command : metadata.features.commands) {
                        featuresList.add(theme.label("  • " + command));
                    }
                }

                if (metadata.features.hud_elements != null && !metadata.features.hud_elements.isEmpty()) {
                    featuresList.add(theme.label("HUD Elements (" + metadata.features.hud_elements.size() + ")"));
                    for (String hud : metadata.features.hud_elements) {
                        featuresList.add(theme.label("  • " + hud));
                    }
                }

                add(theme.horizontalSeparator()).expandX();
            }
        }

        // Links
        WTable links = add(theme.table()).expandX().widget();
        boolean hasLinks = false;

        if (addon.getGithubUrl().isPresent()) {
            WButton githubButton = links.add(theme.button("GitHub")).expandCellX().widget();
            final String githubUrl = addon.getGithubUrl().get();
            githubButton.action = () -> Util.getOperatingSystem().open(githubUrl);
            hasLinks = true;
        }

        if (addon.getDiscordUrl().isPresent()) {
            WButton discordButton = links.add(theme.button("Discord")).expandCellX().widget();
            final String discordUrl = addon.getDiscordUrl().get();
            discordButton.action = () -> Util.getOperatingSystem().open(discordUrl);
            hasLinks = true;
        }

        if (addon.getHomepageUrl().isPresent()) {
            WButton homepageButton = links.add(theme.button("Homepage")).expandCellX().widget();
            final String homepageUrl = addon.getHomepageUrl().get();
            homepageButton.action = () -> Util.getOperatingSystem().open(homepageUrl);
            hasLinks = true;
        }

        if (hasLinks) {
            add(theme.horizontalSeparator()).expandX();
        }

        // Download button (only for online addons)
        WTable actions = add(theme.table()).expandX().widget();

        if (addon.isInstalled()) {
            actions.add(theme.label("✓ Installed")).expandCellX();
        } else if (addon instanceof OnlineAddon) {
            WButton downloadButton = actions.add(theme.button("Download")).expandCellX().widget();
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

        WButton backButton = actions.add(theme.button("Back")).expandCellX().widget();
        backButton.action = () -> mc.setScreen(parent);
    }
}
