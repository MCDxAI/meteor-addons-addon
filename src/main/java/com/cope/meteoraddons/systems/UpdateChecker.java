package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.InstalledAddon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.models.UpdateInfo;
import com.cope.meteoraddons.util.GitHubReleaseAPI;
import com.cope.meteoraddons.util.HashUtil;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.NbtCompound;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * System for checking addon updates by comparing SHA256 hashes.
 * Runs after AddonManager finishes fetching addon metadata.
 */
public class UpdateChecker extends System<UpdateChecker> {
    private final List<UpdateInfo> availableUpdates = new ArrayList<>();
    private final AtomicBoolean isChecking = new AtomicBoolean(false);
    private final AtomicBoolean checkComplete = new AtomicBoolean(false);
    private Consumer<List<UpdateInfo>> onUpdatesFound;

    public UpdateChecker() {
        super("update-checker");
    }

    public static UpdateChecker get() {
        return Systems.get(UpdateChecker.class);
    }

    @Override
    public void init() {
        // Don't auto-run, wait to be triggered after AddonManager is ready
    }

    @Override
    public NbtCompound toTag() {
        return new NbtCompound();
    }

    @Override
    public UpdateChecker fromTag(NbtCompound tag) {
        return this;
    }

    /**
     * Set callback for when updates are found.
     */
    public void setOnUpdatesFound(Consumer<List<UpdateInfo>> callback) {
        this.onUpdatesFound = callback;
    }

    /**
     * Start checking for updates in the background.
     * Should be called after AddonManager finishes loading.
     */
    public void checkForUpdates() {
        if (isChecking.getAndSet(true)) {
            MeteorAddonsAddon.LOG.info("Update check already in progress");
            return;
        }

        checkComplete.set(false);
        availableUpdates.clear();

        MeteorExecutor.execute(() -> {
            try {
                doUpdateCheck();
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Update check failed", e);
            } finally {
                isChecking.set(false);
                checkComplete.set(true);

                if (!availableUpdates.isEmpty() && onUpdatesFound != null) {
                    onUpdatesFound.accept(new ArrayList<>(availableUpdates));
                }
            }
        });
    }

    private void doUpdateCheck() {
        AddonManager addonManager = AddonManager.get();
        List<Addon> installedAddons = addonManager.getInstalledAddons();
        List<AddonMetadata> onlineAddons = addonManager.getAvailableAddons();

        MeteorAddonsAddon.LOG.info("Checking {} installed addons for updates...", installedAddons.size());

        // Build map of online addon metadata by name for quick lookup
        Map<String, AddonMetadata> onlineMap = new ConcurrentHashMap<>();
        for (AddonMetadata metadata : onlineAddons) {
            if (metadata.name != null) {
                onlineMap.put(metadata.name.toLowerCase(), metadata);
            }
        }

        for (Addon addon : installedAddons) {
            if (!(addon instanceof InstalledAddon installed)) {
                continue;
            }

            // Skip checking ourselves
            if (installed.getId().equals("meteor-addons")) {
                continue;
            }

            try {
                checkAddonForUpdate(installed, onlineMap);
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.warn("Failed to check update for {}: {}", addon.getName(), e.getMessage());
            }
        }

        MeteorAddonsAddon.LOG.info("Update check complete. Found {} updates.", availableUpdates.size());
    }

    private void checkAddonForUpdate(InstalledAddon installed, Map<String, AddonMetadata> onlineMap) {
        String name = installed.getName();

        // Find matching online addon
        AddonMetadata metadata = onlineMap.get(name.toLowerCase());
        if (metadata == null) {
            // Try matching by ID
            metadata = onlineMap.get(installed.getId().toLowerCase());
        }

        if (metadata == null) {
            MeteorAddonsAddon.LOG.debug("No online metadata found for {}", name);
            return;
        }

        // Get GitHub URL from metadata
        String githubUrl = metadata.links != null ? metadata.links.github : null;
        if (githubUrl == null || githubUrl.isEmpty()) {
            MeteorAddonsAddon.LOG.debug("No GitHub URL for {}", name);
            return;
        }

        // Parse owner/repo from GitHub URL
        Optional<String[]> parsed = GitHubReleaseAPI.parseGitHubUrl(githubUrl);
        if (parsed.isEmpty()) {
            MeteorAddonsAddon.LOG.debug("Failed to parse GitHub URL for {}: {}", name, githubUrl);
            return;
        }

        String[] ownerRepo = parsed.get();

        // Get local JAR path
        Path localJarPath = getJarPath(installed);
        if (localJarPath == null) {
            MeteorAddonsAddon.LOG.debug("Could not determine JAR path for {}", name);
            return;
        }

        // Compute local hash
        String localHash = HashUtil.computeSha256(localJarPath);
        if (localHash == null) {
            MeteorAddonsAddon.LOG.warn("Failed to compute hash for {}", name);
            return;
        }

        // Fetch release info from GitHub
        Optional<GitHubReleaseAPI.ReleaseInfo> releaseOpt = GitHubReleaseAPI.getLatestRelease(ownerRepo[0], ownerRepo[1]);
        if (releaseOpt.isEmpty()) {
            MeteorAddonsAddon.LOG.debug("No release found for {}", name);
            return;
        }

        GitHubReleaseAPI.ReleaseInfo release = releaseOpt.get();

        // Find JAR asset
        Optional<GitHubReleaseAPI.AssetInfo> assetOpt = GitHubReleaseAPI.findJarAsset(release);
        if (assetOpt.isEmpty()) {
            MeteorAddonsAddon.LOG.debug("No JAR asset found in release for {}", name);
            return;
        }

        GitHubReleaseAPI.AssetInfo asset = assetOpt.get();
        String remoteHash = asset.getSha256();

        if (remoteHash == null || remoteHash.isEmpty()) {
            MeteorAddonsAddon.LOG.debug("No SHA256 digest available for {} (GitHub may not have computed it yet)", name);
            return;
        }

        // Compare hashes
        if (!HashUtil.hashesMatch(localHash, remoteHash)) {
            MeteorAddonsAddon.LOG.info("Update available for {}: {} -> {}",
                name, installed.getVersion(), release.getVersion());

            UpdateInfo update = new UpdateInfo(
                installed,
                name,
                installed.getVersion(),
                release.getVersion(),
                release.getChangelog(),
                asset.getDownloadUrl(),
                remoteHash,
                localHash,
                localJarPath
            );

            availableUpdates.add(update);
        } else {
            MeteorAddonsAddon.LOG.debug("{} is up to date", name);
        }
    }

    /**
     * Get the JAR file path for an installed addon.
     */
    private Path getJarPath(InstalledAddon addon) {
        try {
            // Use getRootPaths() and navigate to the actual JAR
            List<Path> rootPaths = addon.getModContainer().getRootPaths();
            if (!rootPaths.isEmpty()) {
                Path rootPath = rootPaths.get(0);
                // getRootPaths returns paths inside the JAR, we need to get the JAR itself
                // The path looks like: jar:file:/path/to/mod.jar!/
                // Convert to the actual JAR path
                String pathStr = rootPath.toUri().toString();

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
            MeteorAddonsAddon.LOG.debug("Failed to get JAR path for {}: {}", addon.getName(), e.getMessage());
        }

        return null;
    }

    /**
     * Get the list of available updates (may be empty if check not complete).
     */
    public List<UpdateInfo> getAvailableUpdates() {
        return new ArrayList<>(availableUpdates);
    }

    /**
     * Check if update check is currently in progress.
     */
    public boolean isChecking() {
        return isChecking.get();
    }

    /**
     * Check if update check has completed.
     */
    public boolean isCheckComplete() {
        return checkComplete.get();
    }

    /**
     * Check if there are any updates available.
     */
    public boolean hasUpdates() {
        return !availableUpdates.isEmpty();
    }
}
