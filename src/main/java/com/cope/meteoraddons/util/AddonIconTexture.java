package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.client.texture.NativeImage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Texture for addon icons loaded from input streams.
 * Extends Meteor's Texture class for proper rendering in GUI.
 * Default size is 128x128 (for main icons), with support for custom sizes (e.g., 32x32 for badges).
 */
public class AddonIconTexture extends Texture {
    /**
     * Create an addon icon texture from an input stream (128x128).
     *
     * @param inputStream stream containing icon image data
     */
    public AddonIconTexture(InputStream inputStream) {
        this(inputStream, 128);
    }

    /**
     * Create an addon icon texture from an input stream with custom size.
     *
     * @param inputStream stream containing icon image data
     * @param size target size (width and height)
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
