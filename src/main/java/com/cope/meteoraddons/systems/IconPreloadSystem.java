package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages preloading of addon icons during resource reload phase.
 *
 * ARCHITECTURE:
 * - HTTP downloads fill iconDataCache (background thread)
 * - reload() converts to GPU textures (render thread, during loading screen)
 * - Widgets access via instant lookup (render thread, no blocking)
 *
 * THREAD SAFETY:
 * - iconDataCache: ConcurrentHashMap (thread-safe for HTTP writes)
 * - textureRegistry: Only accessed on render thread (no sync needed)
 */
public class IconPreloadSystem extends System<IconPreloadSystem> implements SynchronousResourceReloader {
    // Raw PNG data from HTTP downloads (filled asynchronously)
    private final Map<String, byte[]> iconDataCache = new ConcurrentHashMap<>();

    // GPU textures created during reload() (render thread only)
    private final Map<String, Texture> textureRegistry = new ConcurrentHashMap<>();

    // Default fallback texture (created lazily, shared instance)
    private Texture defaultTexture;

    // Installed indicator badge (created lazily)
    private Texture installedIndicator;

    public IconPreloadSystem() {
        super("icon-preload");
    }

    public static IconPreloadSystem get() {
        return Systems.get(IconPreloadSystem.class);
    }

    /**
     * Cache icon data from HTTP download (called from background thread).
     * Safe to call before reload() - data will be converted to textures on next reload.
     *
     * @param addonId unique addon identifier
     * @param pngData raw PNG bytes
     */
    public void cacheIconData(String addonId, byte[] pngData) {
        if (pngData == null || pngData.length == 0) {
            MeteorAddonsAddon.LOG.warn("Ignoring empty icon data for {}", addonId);
            return;
        }

        iconDataCache.put(addonId, pngData);
        MeteorAddonsAddon.LOG.debug("Cached icon data for {} ({} bytes)", addonId, pngData.length);
    }

    /**
     * Get texture for addon (instant lookup, no blocking).
     * MUST be called from render thread.
     *
     * @param addonId addon identifier
     * @return texture or null if not found (caller should use default)
     */
    public Texture getTexture(String addonId) {
        return textureRegistry.get(addonId);
    }

    /**
     * Get the default fallback texture.
     *
     * @return default gray texture
     */
    public Texture getDefaultTexture() {
        if (defaultTexture == null) {
            defaultTexture = createDefaultTexture(128);
        }
        return defaultTexture;
    }

    /**
     * Load and cache texture from an InputStream (for installed addons with JAR icons).
     * This allows installed addons to use their embedded icons.
     *
     * @param addonId addon identifier
     * @param iconStream input stream of icon PNG data
     * @return texture or default fallback
     */
    public Texture loadTextureFromStream(String addonId, InputStream iconStream) {
        // Check if already loaded
        Texture existing = textureRegistry.get(addonId);
        if (existing != null) {
            return existing;
        }

        try {
            byte[] iconData = iconStream.readAllBytes();
            NativeImage image = NativeImage.read(new ByteArrayInputStream(iconData));
            Texture texture = createTextureFromNativeImage(image, 128);
            textureRegistry.put(addonId, texture);
            image.close();
            MeteorAddonsAddon.LOG.debug("Loaded texture from stream for {}", addonId);
            return texture;
        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to load texture from stream for {}: {}", addonId, e.getMessage());
            if (defaultTexture == null) {
                defaultTexture = createDefaultTexture(128);
            }
            return defaultTexture;
        }
    }

    /**
     * Get installed indicator badge texture.
     *
     * @return 32x32 green checkmark texture
     */
    public Texture getInstalledIndicator() {
        if (installedIndicator == null) {
            try {
                InputStream stream = IconPreloadSystem.class.getResourceAsStream(
                    "/assets/meteor-addons/installed-icon.png"
                );
                if (stream != null) {
                    byte[] data = stream.readAllBytes();
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
                    installedIndicator = createTextureFromNativeImage(image, 32);
                    image.close();
                } else {
                    MeteorAddonsAddon.LOG.warn("Installed indicator asset not found, using default");
                    installedIndicator = createDefaultTexture(32);
                }
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Failed to load installed indicator", e);
                installedIndicator = createDefaultTexture(32);
            }
        }
        return installedIndicator;
    }

    /**
     * SynchronousResourceReloader implementation.
     * Called by Minecraft on render thread during resource load phase.
     *
     * WHEN CALLED:
     * - Game startup (after mods load)
     * - F3+T resource reload
     * - Resource pack change
     *
     * THREAD: Always render thread (guaranteed by Minecraft)
     */
    @Override
    public void reload(ResourceManager manager) {
        MeteorAddonsAddon.LOG.info("Starting icon preload: {} cached icons", iconDataCache.size());

        // Clear old textures to prevent GPU memory leak
        for (Texture oldTexture : textureRegistry.values()) {
            if (oldTexture != null && oldTexture != defaultTexture) {
                oldTexture.close();
            }
        }
        textureRegistry.clear();

        // Convert cached PNG data to GPU textures
        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, byte[]> entry : iconDataCache.entrySet()) {
            String addonId = entry.getKey();
            byte[] pngData = entry.getValue();

            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(pngData));
                Texture texture = createTextureFromNativeImage(image, 128);
                textureRegistry.put(addonId, texture);
                image.close();
                successCount++;
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.warn("Failed to create texture for {}: {}",
                    addonId, e.getMessage());
                failureCount++;
            }
        }

        MeteorAddonsAddon.LOG.info("Icon preload complete: {} success, {} failed",
            successCount, failureCount);
    }

    /**
     * Create Meteor Texture from NativeImage.
     * Handles resizing and ABGR→RGBA color conversion.
     */
    @SuppressWarnings("deprecation")
    private Texture createTextureFromNativeImage(NativeImage sourceImage, int targetSize) {
        NativeImage image;

        // Resize if needed
        if (sourceImage.getWidth() != targetSize || sourceImage.getHeight() != targetSize) {
            NativeImage resized = new NativeImage(targetSize, targetSize, false);
            sourceImage.resizeSubRectTo(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), resized);
            image = resized;
        } else {
            image = sourceImage;
        }

        // Create texture
        Texture texture = new Texture(targetSize, targetSize, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        // Convert ABGR pixels to RGBA bytes
        int[] pixels = image.makePixelArray();
        byte[] bytes = new byte[targetSize * targetSize * 4];

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            bytes[i * 4]     = (byte) ((color >> 16) & 0xFF); // R
            bytes[i * 4 + 1] = (byte) ((color >> 8) & 0xFF);  // G
            bytes[i * 4 + 2] = (byte) (color & 0xFF);         // B
            bytes[i * 4 + 3] = (byte) ((color >> 24) & 0xFF); // A
        }

        texture.upload(bytes);

        // Clean up resized image if we created one
        if (image != sourceImage) {
            image.close();
        }

        return texture;
    }

    /**
     * Create simple gray fallback texture.
     */
    private Texture createDefaultTexture(int size) {
        Texture texture = new Texture(size, size, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        byte[] pixels = new byte[size * size * 4];
        for (int i = 0; i < pixels.length; i += 4) {
            pixels[i]     = (byte) 128; // R
            pixels[i + 1] = (byte) 128; // G
            pixels[i + 2] = (byte) 128; // B
            pixels[i + 3] = (byte) 255; // A
        }

        texture.upload(pixels);
        return texture;
    }

    /**
     * Clear all cached data (for testing/debugging).
     */
    public void clearCache() {
        iconDataCache.clear();

        // Textures will be cleaned on next reload()
        MeteorAddonsAddon.LOG.info("Icon cache cleared");
    }

    @Override
    public NbtCompound toTag() {
        // No persistent state needed
        return new NbtCompound();
    }

    @Override
    public IconPreloadSystem fromTag(NbtCompound tag) {
        return this;
    }
}
