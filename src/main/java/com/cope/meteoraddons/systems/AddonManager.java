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

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Manages addon metadata, installation, and updates.
 */
public class AddonManager extends System<AddonManager> {
    private static final String ADDON_SCANNER_URL =
        "https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/addons/addons.json";

    private final Gson gson = new Gson();
    private List<AddonMetadata> availableAddons = new ArrayList<>();
    private List<Addon> onlineAddons = new ArrayList<>();
    private List<Addon> installedAddons = new ArrayList<>();
    private List<String> installedAddonNames = new ArrayList<>();
    private boolean isLoading = false;
    private String lastError = null;

    public AddonManager() {
        super("addon-manager");
    }

    public static AddonManager get() {
        return Systems.get(AddonManager.class);
    }

    @Override
    public void init() {
        scanInstalledAddons();
        fetchAddonMetadata();
    }

    private void scanInstalledAddons() {
        installedAddons.clear();
        installedAddonNames.clear();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                if (!FabricLoader.getInstance().getEntrypointContainers("meteor", Object.class).isEmpty()) {
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
     * Fetch addon metadata asynchronously from scanner repository.
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

                String jsonResponse = HttpClient.downloadString(ADDON_SCANNER_URL);

                Type listType = new TypeToken<List<AddonMetadata>>(){}.getType();
                availableAddons = gson.fromJson(jsonResponse, listType);

                MeteorAddonsAddon.LOG.info("Fetched {} addons from scanner", availableAddons.size());

                String currentVersion = VersionUtil.getCurrentMinecraftVersion();
                List<AddonMetadata> filteredMetadata = availableAddons.stream()
                    .filter(AddonMetadata::supportsCurrentVersion)
                    .filter(addon -> addon.verified)
                    .filter(addon -> {
                        // Hardcoded ignore for template repo
                        if (addon.repo == null || addon.repo.id == null) return true;
                        return !addon.repo.id.equalsIgnoreCase("meteordevelopment/meteor-addon-template");
                    })
                    .collect(Collectors.toMap(
                        addon -> addon.name,
                        addon -> addon,
                        (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

                onlineAddons = filteredMetadata.stream()
                    .map(OnlineAddon::new)
                    .collect(Collectors.toList());

                MeteorAddonsAddon.LOG.info("Filtered to {} addons for Minecraft {}",
                    onlineAddons.size(), currentVersion);

                preloadIconsAsync(filteredMetadata);

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

    private void preloadIconsAsync(List<AddonMetadata> addons) {
        MeteorAddonsAddon.LOG.info("Starting async icon download for {} addons", addons.size());

        int successCount = 0;
        int failureCount = 0;

        for (AddonMetadata metadata : addons) {
            String iconUrl = metadata.getIconUrl();
            if (iconUrl == null || iconUrl.isEmpty()) {
                failureCount++;
                continue;
            }

            try {
                byte[] iconData = HttpClient.downloadBytes(iconUrl);
                String addonId = metadata.name.toLowerCase().replace(" ", "-");
                IconPreloadSystem.get().cacheIconData(addonId, iconData);
                successCount++;
            } catch (IOException e) {
                MeteorAddonsAddon.LOG.debug("Failed to download icon for {}: {}",
                    metadata.name, e.getMessage());
                failureCount++;
            }
        }

        MeteorAddonsAddon.LOG.info("Icon download complete: {} success, {} failed",
            successCount, failureCount);

        mc.execute(() -> {
            MeteorAddonsAddon.LOG.info("Converting downloaded icons to GPU textures");
            IconPreloadSystem.get().reload(mc.getResourceManager());
        });
    }

    public boolean downloadAddon(OnlineAddon addon) {
        String[] downloadUrls = addon.getMetadata().getDownloadUrls();

        if (downloadUrls.length == 0) {
            MeteorAddonsAddon.LOG.error("No compatible download URLs for addon: {}", addon.getName());
            return false;
        }

        try {
            Path meteorFolder = MeteorClient.FOLDER.toPath();
            Path modsFolder = meteorFolder.getParent().resolve("mods");

            String url = downloadUrls[0];
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            Path destPath = modsFolder.resolve(fileName);

            MeteorAddonsAddon.LOG.info("Downloading addon {} to {}", addon.getName(), destPath);

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

    public List<Addon> getOnlineAddons() {
        return onlineAddons;
    }

    public List<Addon> getInstalledAddons() {
        return installedAddons;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isInstalled(String addonName) {
        return installedAddonNames.contains(addonName);
    }
}
