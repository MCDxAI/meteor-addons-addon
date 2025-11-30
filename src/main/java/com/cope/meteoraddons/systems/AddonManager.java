package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.InstalledAddon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.util.HttpClient;
import com.cope.meteoraddons.util.VersionUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
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
    private List<Addon> onlineAddons = new ArrayList<>();
    private List<Addon> installedAddons = new ArrayList<>();
    private List<String> installedAddonNames = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isInitialized = false;
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
     * Safe to call multiple times - will only initialize once.
     */
    public void init() {
        if (isInitialized) {
            MeteorAddonsAddon.LOG.warn("AddonManager already initialized, skipping duplicate init");
            return;
        }

        isInitialized = true;
        scanInstalledAddons();
        fetchAddonMetadata();
    }

    /**
     * Scan for installed Meteor addons.
     * Looks for all mods with the "meteor" entrypoint.
     */
    private void scanInstalledAddons() {
        installedAddons.clear();
        installedAddonNames.clear();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            // Check if this mod has a meteor entrypoint using FabricLoader
            try {
                // Try to get meteor entrypoints - if it succeeds, this is a meteor addon
                if (!FabricLoader.getInstance().getEntrypointContainers("meteor", Object.class).isEmpty()) {
                    // Check if this specific mod provides the entrypoint
                    boolean isMeteorAddon = FabricLoader.getInstance().getEntrypointContainers("meteor", Object.class)
                        .stream()
                        .anyMatch(container -> container.getProvider().getMetadata().getId().equals(mod.getMetadata().getId()));

                    if (isMeteorAddon) {
                        InstalledAddon addon = new InstalledAddon(mod);
                        installedAddons.add(addon);
                        installedAddonNames.add(addon.getName());

                        MeteorAddonsAddon.LOG.info("Found installed Meteor addon: {} ({})",
                            addon.getName(), addon.getVersion());
                    }
                }
            } catch (Exception e) {
                // Not a meteor addon, skip
            }
        }

        MeteorAddonsAddon.LOG.info("Found {} installed Meteor addons", installedAddons.size());
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
                List<AddonMetadata> filteredMetadata = availableAddons.stream()
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

                // Wrap in OnlineAddon
                onlineAddons = filteredMetadata.stream()
                    .map(OnlineAddon::new)
                    .collect(Collectors.toList());

                MeteorAddonsAddon.LOG.info("Filtered to {} addons for Minecraft {}",
                    onlineAddons.size(), currentVersion);

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
     * Download and install an addon.
     *
     * @param addon addon to install
     * @return true if download succeeded
     */
    public boolean downloadAddon(OnlineAddon addon) {
        String[] downloadUrls = addon.getMetadata().getDownloadUrls();

        if (downloadUrls.length == 0) {
            MeteorAddonsAddon.LOG.error("No compatible download URLs for addon: {}", addon.getName());
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

            MeteorAddonsAddon.LOG.info("Downloading addon {} to {}", addon.getName(), destPath);

            // Download with fallback URLs
            String successUrl = HttpClient.downloadFileWithFallback(downloadUrls, destPath);

            if (successUrl != null) {
                MeteorAddonsAddon.LOG.info("Successfully downloaded addon: {}", addon.getName());
                installedAddonNames.add(addon.getName());
                save();
                return true;
            } else {
                MeteorAddonsAddon.LOG.error("Failed to download addon: {}", addon.getName());
                return false;
            }

        } catch (Exception e) {
            MeteorAddonsAddon.LOG.error("Failed to download addon {}: {}",
                addon.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Get the list of online addons compatible with the current Minecraft version.
     *
     * @return list of online addons
     */
    public List<Addon> getOnlineAddons() {
        return onlineAddons;
    }

    /**
     * Get the list of installed Meteor addons.
     *
     * @return list of installed addons
     */
    public List<Addon> getInstalledAddons() {
        return installedAddons;
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
        return installedAddonNames.contains(addonName);
    }
}
