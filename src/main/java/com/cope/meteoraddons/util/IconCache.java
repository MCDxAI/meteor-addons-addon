package com.cope.meteoraddons.util;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.systems.IconPreloadSystem;
import meteordevelopment.meteorclient.renderer.Texture;

import java.io.InputStream;
import java.util.Optional;

/**
 * Icon cache - delegates to IconPreloadSystem for instant lookups.
 */
public final class IconCache {
    public static Texture get(Addon addon) {
        String addonId = addon.getId();
        Texture texture = IconPreloadSystem.get().getTexture(addonId);

        if (texture == null) {
            try {
                Optional<InputStream> iconStream = addon.getIconStream();
                if (iconStream.isPresent()) {
                    texture = IconPreloadSystem.get().loadTextureFromStream(addonId, iconStream.get());
                }
            } catch (Exception e) {
            }
        }

        if (texture == null) {
            texture = IconPreloadSystem.get().getDefaultTexture();
        }

        return texture;
    }
    public static Texture getInstalledIndicator() {
        return IconPreloadSystem.get().getInstalledIndicator();
    }

    public static void clear() {
        IconPreloadSystem.get().clearCache();
    }
}
