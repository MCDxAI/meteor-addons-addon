package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.config.IconSizeConfig;
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
 * Manages addon icon preloading during resource reload.
 * HTTP downloads fill iconDataCache (background), reload() converts to GPU
 * textures (render thread).
 * Thread-safe: iconDataCache uses ConcurrentHashMap, textureRegistry is
 * render-thread only.
 */
public class IconPreloadSystem extends System<IconPreloadSystem> implements SynchronousResourceReloader {
    private final Map<String, byte[]> iconDataCache = new ConcurrentHashMap<>();
    private final Map<String, Texture> textureRegistry = new ConcurrentHashMap<>();
    private Texture defaultTexture;
    private Texture installedIndicator;

    public IconPreloadSystem() {
        super("icon-preload");
    }

    public static IconPreloadSystem get() {
        return Systems.get(IconPreloadSystem.class);
    }

    /**
     * Cache icon data from HTTP download (background thread safe).
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
     * Get texture for addon (instant lookup, render thread only).
     */
    public Texture getTexture(String addonId) {
        return textureRegistry.get(addonId);
    }

    public Texture getDefaultTexture() {
        if (defaultTexture == null) {
            defaultTexture = createDefaultTexture(IconSizeConfig.ADDON_ICON_SIZE);
        }
        return defaultTexture;
    }

    /**
     * Load texture from InputStream for installed addons with embedded JAR icons.
     */
    public Texture loadTextureFromStream(String addonId, InputStream iconStream) {
        Texture existing = textureRegistry.get(addonId);
        if (existing != null) {
            return existing;
        }

        try {
            byte[] iconData = iconStream.readAllBytes();
            NativeImage image = NativeImage.read(new ByteArrayInputStream(iconData));
            Texture texture = createTextureFromNativeImage(image, IconSizeConfig.ADDON_ICON_SIZE);
            textureRegistry.put(addonId, texture);
            image.close();
            MeteorAddonsAddon.LOG.debug("Loaded texture from stream for {}", addonId);
            return texture;
        } catch (Exception e) {
            MeteorAddonsAddon.LOG.warn("Failed to load texture from stream for {}: {}", addonId, e.getMessage());
            if (defaultTexture == null) {
                defaultTexture = createDefaultTexture(IconSizeConfig.ADDON_ICON_SIZE);
            }
            return defaultTexture;
        }
    }

    public Texture getInstalledIndicator() {
        if (installedIndicator == null) {
            try {
                InputStream stream = IconPreloadSystem.class.getResourceAsStream(
                        "/assets/meteor-addons/installed-icon.png");
                if (stream != null) {
                    byte[] data = stream.readAllBytes();
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
                    installedIndicator = createTextureFromNativeImage(image, IconSizeConfig.INSTALLED_INDICATOR_SIZE);
                    image.close();
                } else {
                    MeteorAddonsAddon.LOG.warn("Installed indicator asset not found, using default");
                    installedIndicator = createDefaultTexture(IconSizeConfig.INSTALLED_INDICATOR_SIZE);
                }
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Failed to load installed indicator", e);
                installedIndicator = createDefaultTexture(IconSizeConfig.INSTALLED_INDICATOR_SIZE);
            }
        }
        return installedIndicator;
    }

    /**
     * SynchronousResourceReloader: called on render thread during resource load.
     */
    @Override
    public void reload(ResourceManager manager) {
        MeteorAddonsAddon.LOG.info("Starting icon preload: {} cached icons", iconDataCache.size());

        for (Texture oldTexture : textureRegistry.values()) {
            if (oldTexture != null && oldTexture != defaultTexture) {
                oldTexture.close();
            }
        }
        textureRegistry.clear();

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, byte[]> entry : iconDataCache.entrySet()) {
            String addonId = entry.getKey();
            byte[] pngData = entry.getValue();

            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(pngData));
                Texture texture = createTextureFromNativeImage(image, IconSizeConfig.ADDON_ICON_SIZE);
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
     * Create Texture from NativeImage with resizing and ABGR→RGBA conversion.
     */
    @SuppressWarnings("deprecation")
    private Texture createTextureFromNativeImage(NativeImage sourceImage, int targetSize) {
        NativeImage image;

        if (sourceImage.getWidth() != targetSize || sourceImage.getHeight() != targetSize) {
            NativeImage resized = new NativeImage(targetSize, targetSize, false);
            sourceImage.resizeSubRectTo(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), resized);
            image = resized;
        } else {
            image = sourceImage;
        }

        Texture texture = new Texture(targetSize, targetSize, TextureFormat.RGBA8, FilterMode.LINEAR,
                FilterMode.LINEAR);

        int[] pixels = image.makePixelArray();
        byte[] bytes = new byte[targetSize * targetSize * 4];

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            bytes[i * 4] = (byte) ((color >> 16) & 0xFF);
            bytes[i * 4 + 1] = (byte) ((color >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) (color & 0xFF);
            bytes[i * 4 + 3] = (byte) ((color >> 24) & 0xFF);
        }

        texture.upload(bytes);

        if (image != sourceImage) {
            image.close();
        }

        return texture;
    }

    private Texture createDefaultTexture(int size) {
        Texture texture = new Texture(size, size, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        byte[] pixels = new byte[size * size * 4];
        for (int i = 0; i < pixels.length; i += 4) {
            pixels[i] = (byte) 128; // R
            pixels[i + 1] = (byte) 128; // G
            pixels[i + 2] = (byte) 128; // B
            pixels[i + 3] = (byte) 255; // A
        }

        texture.upload(pixels);
        return texture;
    }

    public void clearCache() {
        iconDataCache.clear();
        MeteorAddonsAddon.LOG.info("Icon cache cleared");
    }

    @Override
    public NbtCompound toTag() {
        return new NbtCompound();
    }

    @Override
    public IconPreloadSystem fromTag(NbtCompound tag) {
        return this;
    }
}
