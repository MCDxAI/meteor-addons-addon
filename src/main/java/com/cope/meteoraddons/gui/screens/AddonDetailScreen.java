package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.InstalledAddon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.models.UpdateInfo;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.GitHubReleaseAPI;
import com.cope.meteoraddons.util.HashUtil;
import com.cope.meteoraddons.util.IconCache;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        WHorizontalList header = add(theme.horizontalList()).centerX().widget();
        
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



        List<String> authors = addon.getAuthors();
        if (!authors.isEmpty()) {
            details.add(theme.label("By: " + String.join(", ", authors)).color(theme.textSecondaryColor()));
        }

        add(theme.horizontalSeparator()).expandX();

        // Description
        addon.getDescription().ifPresent(desc -> {
            add(theme.label(desc, getWindowWidth() / 2.0)).centerX();
        });

        // Tags
        if (addon instanceof OnlineAddon) {
            AddonMetadata metadata = ((OnlineAddon) addon).getMetadata();
            if (metadata.custom != null && metadata.custom.tags != null && !metadata.custom.tags.isEmpty()) {
                WHorizontalList tagsList = add(theme.horizontalList()).centerX().padTop(3).widget();
                tagsList.add(theme.label("Tags: ").color(theme.textSecondaryColor()));

                for (String tag : metadata.custom.tags) {
                    tagsList.add(theme.label(tag)).padRight(6);
                }
            }
        }
        
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
             if (metadata.features != null && hasAnyFeatures(metadata.features)) {
                 WSection featuresSection = add(theme.section("Features", true)).expandX().widget();

                 boolean needsSeparator = false;
                 needsSeparator = addFeatureList(featuresSection, "Modules", metadata.features.modules, needsSeparator) || needsSeparator;
                 needsSeparator = addFeatureList(featuresSection, "Commands", metadata.features.commands, needsSeparator) || needsSeparator;
                 needsSeparator = addFeatureList(featuresSection, "HUD", metadata.features.hud_elements, needsSeparator) || needsSeparator;
                 addFeatureList(featuresSection, "Screens", metadata.features.custom_screens, needsSeparator);
             }
        }

        add(theme.horizontalSeparator()).expandX();

        // Actions (Buttons)
        WHorizontalList actions = add(theme.horizontalList()).right().widget();

        // Download/Install Button (for non-installed addons)
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

        // Check for Updates Button (for installed addons)
        if (addon.isInstalled() && addon instanceof InstalledAddon installedAddon) {
            WButton checkUpdateBtn = actions.add(theme.button("Check for Updates")).widget();
            checkUpdateBtn.action = () -> {
                checkUpdateBtn.set("Checking...");
                MeteorExecutor.execute(() -> {
                    Optional<UpdateInfo> update = checkForUpdate(installedAddon);
                    mc.execute(() -> {
                        if (update.isPresent()) {
                            checkUpdateBtn.set("Update Found!");
                            // Show updates screen with this single update
                            mc.setScreen(new UpdatesAvailableScreen(GuiThemes.get(), Collections.singletonList(update.get())));
                        } else {
                            checkUpdateBtn.set("Up to date");
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

    /**
     * Check if metadata has any non-empty feature lists.
     */
    private boolean hasAnyFeatures(AddonMetadata.Features features) {
        return (features.modules != null && !features.modules.isEmpty()) ||
               (features.commands != null && !features.commands.isEmpty()) ||
               (features.hud_elements != null && !features.hud_elements.isEmpty()) ||
               (features.custom_screens != null && !features.custom_screens.isEmpty());
    }

    /**
     * Add a feature list to the section if items are present.
     *
     * @param section The section to add to
     * @param label The feature type label (e.g., "Modules", "Commands")
     * @param items The list of feature names
     * @param addSeparator Whether to add a separator before this feature group
     * @return true if items were added, false otherwise
     */
    private boolean addFeatureList(WSection section, String label, List<String> items, boolean addSeparator) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        if (addSeparator) {
            section.add(theme.horizontalSeparator()).expandX();
        }

        section.add(theme.label(label + " (" + items.size() + "):"));
        String itemsStr = String.join(", ", items);
        section.add(theme.label(itemsStr, getWindowWidth() / 2.0).color(theme.textSecondaryColor()));

        return true;
    }

    /**
     * Check for updates for a specific installed addon.
     */
    private Optional<UpdateInfo> checkForUpdate(InstalledAddon installed) {
        MeteorAddonsAddon.LOG.info("Checking for updates for: {}", installed.getName());

        // Get GitHub URL from the installed addon
        Optional<String> githubUrlOpt = installed.getGithubUrl();
        if (githubUrlOpt.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("No GitHub URL for {}", installed.getName());
            return Optional.empty();
        }

        String githubUrl = githubUrlOpt.get();
        MeteorAddonsAddon.LOG.info("GitHub URL: {}", githubUrl);

        // Parse owner/repo
        Optional<String[]> parsed = GitHubReleaseAPI.parseGitHubUrl(githubUrl);
        if (parsed.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("Failed to parse GitHub URL: {}", githubUrl);
            return Optional.empty();
        }

        String[] ownerRepo = parsed.get();
        MeteorAddonsAddon.LOG.info("Parsed: owner={}, repo={}", ownerRepo[0], ownerRepo[1]);

        // Get local JAR path
        Path localJarPath = getJarPath(installed);
        if (localJarPath == null) {
            MeteorAddonsAddon.LOG.warn("Could not determine JAR path for {}", installed.getName());
            return Optional.empty();
        }
        MeteorAddonsAddon.LOG.info("Local JAR path: {}", localJarPath);

        // Compute local hash
        String localHash = HashUtil.computeSha256(localJarPath);
        if (localHash == null) {
            MeteorAddonsAddon.LOG.warn("Failed to compute local hash for {}", installed.getName());
            return Optional.empty();
        }
        MeteorAddonsAddon.LOG.info("Local SHA256: {}", localHash);

        // Fetch release info from GitHub
        Optional<GitHubReleaseAPI.ReleaseInfo> releaseOpt = GitHubReleaseAPI.getLatestRelease(ownerRepo[0], ownerRepo[1]);
        if (releaseOpt.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("No release found for {}/{}", ownerRepo[0], ownerRepo[1]);
            return Optional.empty();
        }

        GitHubReleaseAPI.ReleaseInfo release = releaseOpt.get();
        MeteorAddonsAddon.LOG.info("Latest release: {} ({})", release.getName(), release.getTagName());

        // Find JAR asset
        Optional<GitHubReleaseAPI.AssetInfo> assetOpt = GitHubReleaseAPI.findJarAsset(release);
        if (assetOpt.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("No JAR asset found in release for {}", installed.getName());
            return Optional.empty();
        }

        GitHubReleaseAPI.AssetInfo asset = assetOpt.get();
        String remoteHash = asset.getSha256();
        MeteorAddonsAddon.LOG.info("Remote JAR: {}", asset.getFileName());
        MeteorAddonsAddon.LOG.info("Remote SHA256: {}", remoteHash);

        if (remoteHash == null || remoteHash.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("No SHA256 digest available for {} (GitHub may not have computed it yet)", installed.getName());
            return Optional.empty();
        }

        // Compare hashes
        if (!HashUtil.hashesMatch(localHash, remoteHash)) {
            MeteorAddonsAddon.LOG.info("UPDATE AVAILABLE for {}: hashes differ", installed.getName());

            return Optional.of(new UpdateInfo(
                installed,
                installed.getName(),
                installed.getVersion(),
                release.getVersion(),
                release.getChangelog(),
                asset.getDownloadUrl(),
                remoteHash,
                localHash,
                localJarPath
            ));
        } else {
            MeteorAddonsAddon.LOG.info("{} is up to date (hashes match)", installed.getName());
            return Optional.empty();
        }
    }

    /**
     * Get the JAR file path for an installed addon.
     */
    private Path getJarPath(InstalledAddon addon) {
        try {
            List<Path> rootPaths = addon.getModContainer().getRootPaths();
            if (!rootPaths.isEmpty()) {
                Path rootPath = rootPaths.get(0);
                String pathStr = rootPath.toUri().toString();
                MeteorAddonsAddon.LOG.debug("Root path URI: {}", pathStr);

                if (pathStr.startsWith("jar:file:")) {
                    // Extract JAR path from jar:file:/path/to/mod.jar!/
                    int exclamation = pathStr.indexOf('!');
                    if (exclamation > 0) {
                        String jarUriStr = pathStr.substring(4, exclamation); // Get "file:/path/to/mod.jar"
                        java.net.URI jarUri = new java.net.URI(jarUriStr);
                        return Path.of(jarUri);
                    }
                } else if (pathStr.endsWith(".jar")) {
                    return rootPath;
                }
            }
        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to get JAR path for {}: {}", addon.getName(), e.getMessage());
        }
        return null;
    }
}
