package com.cope.meteoraddons.config;

/**
 * Centralized configuration for icon sizes across the addon.
 * Change these values to adjust icon sizes globally.
 */
public final class IconSizeConfig {
    /**
     * Standard addon icon size used in all GUI views.
     * Default: 64x64 pixels
     */
    public static final int ADDON_ICON_SIZE = 64;

    /**
     * Installed indicator badge icon size.
     * Default: 32x32 pixels
     */
    public static final int INSTALLED_INDICATOR_SIZE = 32;

    private IconSizeConfig() {
        // Prevent instantiation
    }
}
