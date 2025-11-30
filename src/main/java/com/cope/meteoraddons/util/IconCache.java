package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.addons.Addon;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Simple icon cache for addon textures.
 * Caches loaded textures by addon ID to avoid reloading.
 * Supports background preloading to prevent render thread lag.
 */
public class IconCache {
    private static final Map<String, Texture> cache64 = new HashMap<>();
    private static final Map<String, Texture> cache128 = new HashMap<>();
    private static Texture DEFAULT_64 = null;
    private static Texture DEFAULT_128 = null;
    private static Texture INSTALLED_INDICATOR = null;

    /**
     * Initialize default textures.
     */
    public static void init() {
        DEFAULT_64 = new AddonIconTexture(64);
        DEFAULT_128 = new AddonIconTexture(128);

        // Load installed indicator icon (32x32 for top-right corner of cards)
        try {
            InputStream stream = IconCache.class.getResourceAsStream("/assets/meteor-addons/installed-icon.png");
            if (stream != null) {
                INSTALLED_INDICATOR = new AddonIconTexture(stream, 32);
            }
        } catch (Exception e) {
            MeteorAddonsAddon.LOG.error("Failed to load installed indicator icon", e);
        }
    }

    /**
     * Get the installed indicator texture (32x32 green checkmark).
     */
    public static Texture getInstalledIndicator() {
        if (INSTALLED_INDICATOR == null) {
            init();
        }
        return INSTALLED_INDICATOR;
    }

    /**
     * Get 64x64 icon texture for an addon (for grid view).
     */
    public static Texture get64(Addon addon) {
        return getTexture(addon, 64, cache64, DEFAULT_64);
    }

    /**
     * Get 128x128 icon texture for an addon (for list view).
     */
    public static Texture get128(Addon addon) {
        return getTexture(addon, 128, cache128, DEFAULT_128);
    }

    /**
     * Get texture from cache or load it.
     */
    private static Texture getTexture(Addon addon, int size, Map<String, Texture> cache, Texture defaultTexture) {
        // Ensure defaults are initialized
        if (defaultTexture == null) {
            init();
            defaultTexture = (size == 64) ? DEFAULT_64 : DEFAULT_128;
        }

        String cacheKey = addon.getId() + "_" + size;

        // Return cached texture if available
        if (cache.containsKey(cacheKey)) {
            Texture cached = cache.get(cacheKey);
            return cached != null ? cached : defaultTexture;
        }

        // Try to load icon from addon
        try {
            InputStream iconStream = addon.getIconStream().orElse(null);
            if (iconStream != null) {
                Texture texture = new AddonIconTexture(iconStream, size);
                if (texture != null) {
                    cache.put(cacheKey, texture);
                    return texture;
                }
            }
        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to load icon for {}: {}", addon.getName(), e.getMessage());
        }

        // Cache the default texture for this addon so we don't retry every frame
        cache.put(cacheKey, defaultTexture);

        // Return default texture as fallback
        return defaultTexture;
    }

    /**
     * Preload icons for a list of addons in the background.
     * This prevents lag when first opening the GUI.
     */
    public static void preloadIcons(List<Addon> addons, int size) {
        MeteorExecutor.execute(() -> {
            for (Addon addon : addons) {
                try {
                    String cacheKey = addon.getId() + "_" + size;

                    // Skip if already cached
                    if (cache64.containsKey(cacheKey) || cache128.containsKey(cacheKey)) {
                        continue;
                    }

                    // Load icon stream on background thread
                    InputStream iconStream = addon.getIconStream().orElse(null);
                    if (iconStream != null) {
                        // Create texture on render thread
                        Texture texture = new AddonIconTexture(iconStream, size);
                        if (texture != null) {
                            // Cache it
                            mc.execute(() -> {
                                Map<String, Texture> cache = (size == 64) ? cache64 : cache128;
                                cache.put(cacheKey, texture);
                            });
                        }
                    }
                } catch (Exception e) {
                    MeteorAddonsAddon.LOG.warn("Failed to preload icon for {}: {}", addon.getName(), e.getMessage());
                }
            }
        });
    }

    /**
     * Clear the cache.
     */
    public static void clear() {
        cache64.clear();
        cache128.clear();
    }
}
