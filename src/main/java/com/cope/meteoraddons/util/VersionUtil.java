package com.cope.meteoraddons.util;

import net.minecraft.SharedConstants;

public class VersionUtil {
    private static String cachedVersion = null;

    public static String getCurrentMinecraftVersion() {
        if (cachedVersion == null) {
            cachedVersion = SharedConstants.getGameVersion().id();
        }
        return cachedVersion;
    }
}
