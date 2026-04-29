package com.cope.meteoraddons.util;

import net.minecraft.SharedConstants;

public class VersionUtil {
    private static String cachedVersion = null;

    public static String getCurrentMinecraftVersion() {
        if (cachedVersion == null) {
            cachedVersion = SharedConstants.getCurrentVersion().name();
        }
        return cachedVersion;
    }

    /** Allows tests to inject a version without Minecraft runtime. Do not call in production code. */
    public static void setCurrentVersionForTest(String version) {
        cachedVersion = version;
    }

    /**
     * Performs a boundary-aware version match within a string.
     * Returns true only if the version occurrence is not immediately followed by another digit,
     * ensuring exact version segments are matched (e.g. "1.21.1" won't match inside "1.21.10").
     */
    public static boolean containsVersion(String text, String version) {
        int idx = text.indexOf(version);
        while (idx != -1) {
            int end = idx + version.length();
            if (end >= text.length() || !Character.isDigit(text.charAt(end))) {
                return true;
            }
            idx = text.indexOf(version, idx + 1);
        }
        return false;
    }
}
