package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.util.CacheManager;
import com.cope.meteoraddons.util.HttpClient;
import com.cope.meteoraddons.util.VersionUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.NbtCompound;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages Meteor addon metadata, installation, and updates.
 * Fetches addon data from the meteor-addon-scanner repository on startup.
 */
public class AddonManager extends System<AddonManager> {
    private static final String ADDON_SCANNER_URL =
        "https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/main/addons.json";

    private final Gson gson = new Gson();
    private List<AddonMetadata> availableAddons = new ArrayList<>();
    private List<AddonMetadata> filteredAddons = new ArrayList<>();
    private List<String> installedAddons = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isLoadingIcons = false;
    private String lastError = null;

    public AddonManager() {
        super("addon-manager");
    }

    public static AddonManager get() {
        return Systems.get(AddonManager.class);
    }

    /**
     * Initialize the addon manager.
     * Should be called on addon startup to fetch metadata.
     */
    public void init() {
        CacheManager.init();
        fetchAddonMetadata();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        // TODO: Save installed addons list
        return tag;
    }

    @Override
    public AddonManager fromTag(NbtCompound tag) {
        // TODO: Load installed addons list
        return this;
    }

    /**
     * Fetch addon metadata from the scanner repository.
     * Runs asynchronously on a background thread.
     * After fetching, filters addons by current Minecraft version and downloads icons.
     */
    public void fetchAddonMetadata() {
        if (isLoading) {
            MeteorAddonsAddon.LOG.warn("Already fetching addon metadata");
            return;
        }

        isLoading = true;
        lastError = null;

        MeteorExecutor.execute(() -> {
            try {
                MeteorAddonsAddon.LOG.info("Fetching addon metadata from: {}", ADDON_SCANNER_URL);

                // Download JSON
                String jsonResponse = HttpClient.downloadString(ADDON_SCANNER_URL);

                // Parse JSON into list
                Type listType = new TypeToken<List<AddonMetadata>>(){}.getType();
                availableAddons = gson.fromJson(jsonResponse, listType);

                MeteorAddonsAddon.LOG.info("Fetched {} addons from scanner", availableAddons.size());

                // Filter by current Minecraft version, verified status, and remove duplicates by name
                String currentVersion = VersionUtil.getCurrentMinecraftVersion();
                filteredAddons = availableAddons.stream()
                    .filter(AddonMetadata::supportsCurrentVersion)
                    .filter(addon -> addon.verified) // Only show verified addons
                    .collect(Collectors.toMap(
                        addon -> addon.name, // Key: addon name
                        addon -> addon,      // Value: addon object
                        (existing, replacement) -> existing // Keep first occurrence on duplicate
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

                MeteorAddonsAddon.LOG.info("Filtered to {} addons for Minecraft {}",
                    filteredAddons.size(), currentVersion);

                // Download icons for filtered addons
                downloadIcons();

            } catch (IOException e) {
                lastError = "Network error: " + e.getMessage();
                MeteorAddonsAddon.LOG.error("Failed to fetch addon metadata: {}", e.getMessage());
            } catch (Exception e) {
                lastError = "Parse error: " + e.getMessage();
                MeteorAddonsAddon.LOG.error("Failed to parse addon metadata: {}", e.getMessage(), e);
            } finally {
                isLoading = false;
            }
        });
    }

    /**
     * Download icons for all filtered addons.
     * Runs asynchronously on background threads.
     */
    private void downloadIcons() {
        if (filteredAddons.isEmpty()) {
            return;
        }

        isLoadingIcons = true;
        MeteorExecutor.execute(() -> {
            try {
                CacheManager.downloadIcons(filteredAddons);
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Failed to download icons: {}", e.getMessage());
            } finally {
                isLoadingIcons = false;
            }
        });
    }

    /**
     * Download and install an addon.
     *
     * @param addon addon to install
     * @return true if download succeeded
     */
    public boolean downloadAddon(AddonMetadata addon) {
        String[] downloadUrls = addon.getDownloadUrls();

        if (downloadUrls.length == 0) {
            MeteorAddonsAddon.LOG.error("No compatible download URLs for addon: {}", addon.name);
            return false;
        }

        try {
            // Get mods folder path (parent of meteor-client folder)
            Path meteorFolder = MeteorClient.FOLDER.toPath();
            Path modsFolder = meteorFolder.getParent().resolve("mods");

            // Determine filename from URL
            String url = downloadUrls[0];
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            Path destPath = modsFolder.resolve(fileName);

            MeteorAddonsAddon.LOG.info("Downloading addon {} to {}", addon.name, destPath);

            // Download with fallback URLs
            String successUrl = HttpClient.downloadFileWithFallback(downloadUrls, destPath);

            if (successUrl != null) {
                MeteorAddonsAddon.LOG.info("Successfully downloaded addon: {}", addon.name);
                installedAddons.add(addon.name);
                save();
                return true;
            } else {
                MeteorAddonsAddon.LOG.error("Failed to download addon: {}", addon.name);
                return false;
            }

        } catch (Exception e) {
            MeteorAddonsAddon.LOG.error("Failed to download addon {}: {}",
                addon.name, e.getMessage());
            return false;
        }
    }

    /**
     * Get the list of addons compatible with the current Minecraft version.
     *
     * @return filtered list of addons
     */
    public List<AddonMetadata> getFilteredAddons() {
        return filteredAddons;
    }

    /**
     * Get all available addons (unfiltered).
     *
     * @return all addons
     */
    public List<AddonMetadata> getAvailableAddons() {
        return availableAddons;
    }

    /**
     * Check if addon metadata is currently being loaded.
     *
     * @return true if loading
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Check if icons are currently being downloaded.
     *
     * @return true if loading icons
     */
    public boolean isLoadingIcons() {
        return isLoadingIcons;
    }

    /**
     * Get the last error message.
     *
     * @return error message or null
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Check if an addon is installed.
     *
     * @param addonName addon name to check
     * @return true if installed
     */
    public boolean isInstalled(String addonName) {
        return installedAddons.contains(addonName);
    }
}
