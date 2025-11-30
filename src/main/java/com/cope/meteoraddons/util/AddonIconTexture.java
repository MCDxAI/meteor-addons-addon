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
 * Texture for addon icons loaded from cached PNG files or input streams.
 * Extends Meteor's Texture class for proper rendering in GUI.
 * Supports both 48x48 (grid) and 128x128 (list) sizes.
 */
public class AddonIconTexture extends Texture {
    /**
     * Create an addon icon texture from a cached file.
     *
     * @param iconPath path to the cached icon PNG file
     * @param size target size (48 or 128)
     */
    public AddonIconTexture(Path iconPath, int size) {
        super(size, size, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        if (!Files.exists(iconPath)) {
            MeteorAddonsAddon.LOG.warn("Icon file does not exist: {}", iconPath);
            createDefaultIcon(size);
            return;
        }

        try (InputStream inputStream = Files.newInputStream(iconPath)) {
            loadFromStream(inputStream, size);
        } catch (IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to load icon from {}: {}", iconPath, e.getMessage());
            createDefaultIcon(size);
        }
    }

    /**
     * Create an addon icon texture from an input stream.
     *
     * @param inputStream stream containing icon image data
     * @param size target size (48 or 128)
     */
    public AddonIconTexture(InputStream inputStream, int size) {
        super(size, size, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);

        try {
            loadFromStream(inputStream, size);
        } catch (IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to load icon from stream: {}", e.getMessage());
            createDefaultIcon(size);
        }
    }

    /**
     * Create a default/placeholder icon texture.
     *
     * @param size target size (48 or 128)
     */
    public AddonIconTexture(int size) {
        super(size, size, TextureFormat.RGBA8, FilterMode.LINEAR, FilterMode.LINEAR);
        createDefaultIcon(size);
    }

    /**
     * Load icon from input stream and resize to target size.
     */
    private void loadFromStream(InputStream inputStream, int size) throws IOException {
        // Load image using NativeImage (handles any size)
        NativeImage sourceImage = NativeImage.read(inputStream);

        // If image is already the target size, use it directly
        if (sourceImage.getWidth() == size && sourceImage.getHeight() == size) {
            uploadNativeImage(sourceImage, size);
            sourceImage.close();
        } else {
            // Resize to target size
            NativeImage resizedImage = new NativeImage(size, size, false);
            sourceImage.resizeSubRectTo(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), resizedImage);

            uploadNativeImage(resizedImage, size);

            // Clean up
            sourceImage.close();
            resizedImage.close();
        }
    }

    /**
     * Helper method to upload NativeImage data to GPU texture.
     *
     * @param image the NativeImage to upload
     * @param size expected image size
     */
    @SuppressWarnings("deprecation")
    private void uploadNativeImage(NativeImage image, int size) {
        // Get pixel array from NativeImage
        int[] pixels = image.makePixelArray();

        // Convert ABGR int array to RGBA byte array
        byte[] bytes = new byte[size * size * 4];
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
    private void createDefaultIcon(int size) {
        // Create a simple gray placeholder (size x size pixels, RGBA)
        int totalSize = size * size * 4;
        byte[] pixels = new byte[totalSize];

        // Fill with gray color (R=128, G=128, B=128, A=255)
        for (int i = 0; i < totalSize; i += 4) {
            pixels[i] = (byte) 128;     // R
            pixels[i + 1] = (byte) 128; // G
            pixels[i + 2] = (byte) 128; // B
            pixels[i + 3] = (byte) 255; // A
        }

        upload(pixels);
    }
}
