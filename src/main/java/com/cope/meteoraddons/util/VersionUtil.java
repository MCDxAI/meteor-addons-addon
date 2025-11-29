package com.cope.meteoraddons.util;

import net.minecraft.SharedConstants;

/**
 * Utility for detecting the current Minecraft version.
 */
public class VersionUtil {
    private static String cachedVersion = null;

    /**
     * Get the current Minecraft version.
     *
     * @return version string (e.g., "1.21.10")
     */
    public static String getCurrentMinecraftVersion() {
        if (cachedVersion == null) {
            // Use Minecraft's SharedConstants to get the game version
            // GameVersion returns the version name directly
            cachedVersion = SharedConstants.getGameVersion().id();
        }
        return cachedVersion;
    }

    /**
     * Check if a version string matches the current Minecraft version.
     * Performs exact string matching.
     *
     * @param version version to check
     * @return true if the version matches
     */
    public static boolean matchesCurrentVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return getCurrentMinecraftVersion().equals(version);
    }
}
