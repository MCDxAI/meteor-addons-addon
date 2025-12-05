package com.cope.meteoraddons.util;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.systems.IconPreloadSystem;
import meteordevelopment.meteorclient.renderer.Texture;

import java.io.InputStream;
import java.util.Optional;

/**
 * Simplified icon cache - delegates to IconPreloadSystem for instant lookups.
 *
 * LEGACY COMPATIBILITY: Maintains same public API as before, but now blazing fast.
 * No more LoadingCache, no more blocking, no more lag.
 */
public final class IconCache {
    /**
     * Get icon texture for an addon (instant, non-blocking).
     * MUST be called from render thread (during widget init).
     *
     * @param addon addon to get icon for
     * @return texture (never null, returns default if not found)
     */
    public static Texture get(Addon addon) {
        String addonId = addon.getId();
        Texture texture = IconPreloadSystem.get().getTexture(addonId);

        // If texture not found in preloaded registry, try loading from addon's icon stream
        // This handles installed addons that have icons embedded in their JARs
        if (texture == null) {
            try {
                Optional<InputStream> iconStream = addon.getIconStream();
                if (iconStream.isPresent()) {
                    texture = IconPreloadSystem.get().loadTextureFromStream(addonId, iconStream.get());
                }
            } catch (Exception e) {
                // Fall through to return default texture
            }
        }

        // Return default if still null
        if (texture == null) {
            texture = IconPreloadSystem.get().getDefaultTexture();
        }

        return texture;
    }

    /**
     * Get the installed indicator texture (32x32 green checkmark).
     *
     * @return installed indicator texture
     */
    public static Texture getInstalledIndicator() {
        return IconPreloadSystem.get().getInstalledIndicator();
    }

    /**
     * Clear the texture cache (for testing/debugging).
     */
    public static void clear() {
        IconPreloadSystem.get().clearCache();
    }
}
