package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.addons.Addon;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Icon cache using Guava's LoadingCache for synchronous blocking texture loading.
 * Based on addon-menu's implementation, adapted for 1.21.10 API changes.
 * Textures are loaded on-demand when widgets call get() (on render thread).
 */
public final class IconCache {
    private static final Texture DEFAULT_TEXTURE = new Texture(128, 128, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);
    private static Texture INSTALLED_INDICATOR = null;

    private static final LoadingCache<Addon, Texture> TEXTURE_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .removalListener((RemovalListener<Addon, Texture>) notification -> {
            Texture texture = notification.getValue();
            if (texture != null && texture != DEFAULT_TEXTURE) {
                texture.close();
            }
        })
        .build(new CacheLoader<>() {
            @Override
            public @NotNull Texture load(@NotNull Addon addon) throws Exception {
                Optional<InputStream> iconStream = addon.getIconStream();

                if (iconStream.isEmpty()) {
                    return DEFAULT_TEXTURE;
                }

                try (InputStream inputStream = iconStream.get()) {
                    NativeImage nativeImage = NativeImage.read(inputStream);

                    Texture texture = new Texture(nativeImage.getWidth(), nativeImage.getHeight(),
                        TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

                    // 1.21.10: Use makePixelArray() instead of direct memory access
                    uploadNativeImageToTexture(nativeImage, texture);

                    nativeImage.close();
                    return texture;
                } catch (IOException e) {
                    MeteorAddonsAddon.LOG.warn("Failed to load icon for {}: {}", addon.getName(), e.getMessage());
                    return DEFAULT_TEXTURE;
                }
            }
        });

    /**
     * Upload NativeImage to Texture (1.21.10 compatible).
     * addon-menu uses MemoryUtil.memCopy with imageId(), but that's not available in 1.21.10.
     */
    @SuppressWarnings("deprecation")
    private static void uploadNativeImageToTexture(NativeImage image, Texture texture) {
        int[] pixels = image.makePixelArray();
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert ABGR to RGBA
        byte[] bytes = new byte[width * height * 4];
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            bytes[i * 4] = (byte) ((color >> 16) & 0xFF);     // R
            bytes[i * 4 + 1] = (byte) ((color >> 8) & 0xFF);  // G
            bytes[i * 4 + 2] = (byte) (color & 0xFF);         // B
            bytes[i * 4 + 3] = (byte) ((color >> 24) & 0xFF); // A
        }

        texture.upload(bytes);
    }

    /**
     * Get icon texture for an addon (128x128 or source resolution).
     * This method BLOCKS until the texture is loaded.
     * MUST be called from render thread (during widget init).
     */
    public static Texture get(Addon addon) {
        try {
            return TEXTURE_CACHE.get(addon);
        } catch (ExecutionException e) {
            MeteorAddonsAddon.LOG.warn("Error loading texture for {}: {}", addon.getName(), e.getMessage());
            return DEFAULT_TEXTURE;
        }
    }

    /**
     * Get the installed indicator texture (32x32 green checkmark).
     * Lazy-loaded on first access.
     */
    public static Texture getInstalledIndicator() {
        if (INSTALLED_INDICATOR == null) {
            try {
                InputStream stream = IconCache.class.getResourceAsStream("/assets/meteor-addons/installed-icon.png");
                if (stream != null) {
                    INSTALLED_INDICATOR = new AddonIconTexture(stream, 32);
                }
            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Failed to load installed indicator icon", e);
            }
        }
        return INSTALLED_INDICATOR;
    }

    /**
     * Clear the texture cache (for testing/debugging).
     */
    public static void clear() {
        TEXTURE_CACHE.invalidateAll();
    }
}
