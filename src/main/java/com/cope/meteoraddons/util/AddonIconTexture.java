package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.client.texture.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Texture for addon icons loaded from cached PNG files.
 * Extends Meteor's Texture class for proper rendering in GUI.
 */
public class AddonIconTexture extends Texture {
    /**
     * Create an addon icon texture from a cached file.
     * Resizes the image to 48x48 if needed.
     *
     * @param iconPath path to the cached icon PNG file
     */
    public AddonIconTexture(Path iconPath) {
        super(48, 48, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        if (!Files.exists(iconPath)) {
            MeteorAddonsAddon.LOG.warn("Icon file does not exist: {}", iconPath);
            createDefaultIcon();
            return;
        }

        try (InputStream inputStream = Files.newInputStream(iconPath)) {
            // Load image using NativeImage (handles any size)
            NativeImage sourceImage = NativeImage.read(inputStream);

            // If image is already 48x48, use it directly
            if (sourceImage.getWidth() == 48 && sourceImage.getHeight() == 48) {
                uploadNativeImage(sourceImage);
                sourceImage.close();
            } else {
                // Resize to 48x48
                NativeImage resizedImage = new NativeImage(48, 48, false);
                sourceImage.resizeSubRectTo(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), resizedImage);

                uploadNativeImage(resizedImage);

                // Clean up
                sourceImage.close();
                resizedImage.close();
            }

        } catch (IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to load icon from {}: {}", iconPath, e.getMessage());
            createDefaultIcon();
        }
    }

    /**
     * Create a default/placeholder icon texture.
     * Uses a simple colored square.
     */
    public AddonIconTexture() {
        super(48, 48, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);
        createDefaultIcon();
    }

    /**
     * Helper method to upload NativeImage data to GPU texture.
     *
     * @param image the NativeImage to upload (must be 48x48)
     */
    @SuppressWarnings("deprecation")
    private void uploadNativeImage(NativeImage image) {
        // Get pixel array from NativeImage
        int[] pixels = image.makePixelArray();

        // Convert ABGR int array to RGBA byte array
        byte[] bytes = new byte[48 * 48 * 4];
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];

            // NativeImage pixels are in ABGR format
            bytes[i * 4] = (byte) ((color >> 16) & 0xFF);     // R
            bytes[i * 4 + 1] = (byte) ((color >> 8) & 0xFF);  // G
            bytes[i * 4 + 2] = (byte) (color & 0xFF);         // B
            bytes[i * 4 + 3] = (byte) ((color >> 24) & 0xFF); // A
        }

        upload(bytes);
    }

    /**
     * Helper method to create default gray icon.
     */
    private void createDefaultIcon() {
        // Create a simple gray placeholder (48x48 pixels, RGBA)
        int size = 48 * 48 * 4;
        byte[] pixels = new byte[size];

        // Fill with gray color (R=128, G=128, B=128, A=255)
        for (int i = 0; i < size; i += 4) {
            pixels[i] = (byte) 128;     // R
            pixels[i + 1] = (byte) 128; // G
            pixels[i + 2] = (byte) 128; // B
            pixels[i + 3] = (byte) 255; // A
        }

        upload(pixels);
    }
}
