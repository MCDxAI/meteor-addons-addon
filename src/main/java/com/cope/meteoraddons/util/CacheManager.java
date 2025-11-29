package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.models.AddonMetadata;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Texture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages caching of addon icons and other resources.
 * Stores cached files in the meteor-client/meteor-addons directory.
 */
public class CacheManager {
    private static final Path CACHE_DIR = MeteorClient.FOLDER.toPath().resolve("meteor-addons");
    private static final Path ICONS_DIR = CACHE_DIR.resolve("icons");
    private static final int WORKER_THREADS = 4;

    private static final Map<String, Texture> iconTextures = new HashMap<>();
    private static Texture DEFAULT_ICON = null;

    /**
     * Initialize cache directories and default icon.
     */
    public static void init() {
        try {
            Files.createDirectories(ICONS_DIR);
            MeteorAddonsAddon.LOG.info("Cache directories initialized at: {}", CACHE_DIR);

            // Create default icon texture
            DEFAULT_ICON = new AddonIconTexture();

        } catch (IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to create cache directories: {}", e.getMessage());
        }
    }

    /**
     * Get the cache directory path.
     *
     * @return cache directory path
     */
    public static Path getCacheDir() {
        return CACHE_DIR;
    }

    /**
     * Download icons for all addons in parallel using a thread pool.
     * Uses 4 worker threads for efficient downloading.
     *
     * @param addons list of addons to download icons for
     */
    public static void downloadIcons(List<AddonMetadata> addons) {
        if (addons == null || addons.isEmpty()) {
            return;
        }

        MeteorAddonsAddon.LOG.info("Starting icon download for {} addons with {} workers",
            addons.size(), WORKER_THREADS);

        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
        CountDownLatch latch = new CountDownLatch(addons.size());

        for (AddonMetadata addon : addons) {
            executor.submit(() -> {
                try {
                    downloadIcon(addon);
                } catch (Exception e) {
                    MeteorAddonsAddon.LOG.warn("Failed to download icon for {}: {}",
                        addon.name, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all downloads to complete
        try {
            latch.await(60, TimeUnit.SECONDS);
            MeteorAddonsAddon.LOG.info("Icon download complete");
        } catch (InterruptedException e) {
            MeteorAddonsAddon.LOG.error("Icon download interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Download a single addon's icon.
     *
     * @param addon addon to download icon for
     */
    private static void downloadIcon(AddonMetadata addon) {
        String iconUrl = addon.getIconUrl();
        if (iconUrl == null || iconUrl.isEmpty()) {
            return;
        }

        try {
            String hash = hashUrl(iconUrl);
            Path iconPath = ICONS_DIR.resolve(hash + ".png");

            // Skip if already cached
            if (Files.exists(iconPath)) {
                return;
            }

            // Download icon
            HttpClient.downloadFile(iconUrl, iconPath);

        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to download icon for {}: {}",
                addon.name, e.getMessage());
        }
    }

    /**
     * Get the texture for an addon's icon.
     * Returns the default icon if the addon icon is not available.
     *
     * @param addon addon to get icon for
     * @return Texture object
     */
    public static Texture getIconTexture(AddonMetadata addon) {
        String iconUrl = addon.getIconUrl();
        if (iconUrl == null || iconUrl.isEmpty()) {
            return DEFAULT_ICON;
        }

        // Check if already loaded
        if (iconTextures.containsKey(iconUrl)) {
            return iconTextures.get(iconUrl);
        }

        try {
            String hash = hashUrl(iconUrl);
            Path iconPath = ICONS_DIR.resolve(hash + ".png");

            if (!Files.exists(iconPath)) {
                return DEFAULT_ICON;
            }

            // Create texture from cached file
            AddonIconTexture texture = new AddonIconTexture(iconPath);
            iconTextures.put(iconUrl, texture);
            return texture;

        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to load icon texture for {}: {}",
                addon.name, e.getMessage());
            return DEFAULT_ICON;
        }
    }

    /**
     * Hash a URL to create a consistent filename.
     *
     * @param url URL to hash
     * @return hex string hash
     */
    private static String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to simple hash
            return String.valueOf(url.hashCode());
        }
    }

    /**
     * Clear the icon cache.
     */
    public static void clearIconCache() {
        try {
            Files.walk(ICONS_DIR)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        MeteorAddonsAddon.LOG.warn("Failed to delete cached icon: {}", path);
                    }
                });
            iconTextures.clear();
            MeteorAddonsAddon.LOG.info("Icon cache cleared");
        } catch (IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to clear icon cache: {}", e.getMessage());
        }
    }
}
