package com.cope.meteoraddons.util;

import com.cope.meteoraddons.addons.Addon;
import com.cope.meteoraddons.addons.OnlineAddon;
import com.cope.meteoraddons.models.AddonMetadata;

import java.util.List;
import java.util.Locale;

/**
 * Utility for searching addons by name, description, authors, features, and tags.
 */
public class AddonSearchUtil {
    /**
     * Check if an addon matches the search query.
     *
     * @param addon The addon to check
     * @param query The search query (case-insensitive)
     * @return true if the addon matches the query
     */
    public static boolean matches(Addon addon, String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT);

        // Name
        if (addon.getName().toLowerCase(Locale.ROOT).contains(q)) return true;

        // Description
        if (addon.getDescription().isPresent() && addon.getDescription().get().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }

        // Authors
        if (addon.getAuthors() != null) {
            for (String author : addon.getAuthors()) {
                if (author.toLowerCase(Locale.ROOT).contains(q)) return true;
            }
        }

        // Metadata Deep Search (Online Addons Only)
        if (addon instanceof OnlineAddon) {
            AddonMetadata meta = ((OnlineAddon) addon).getMetadata();
            if (meta != null) {
                // Features: modules, commands, custom_screens
                if (meta.features != null) {
                    if (containsInList(meta.features.modules, q)) return true;
                    if (containsInList(meta.features.commands, q)) return true;
                    if (containsInList(meta.features.custom_screens, q)) return true;
                }

                // Custom Tags (e.g. "qol", "pvp")
                if (meta.custom != null && containsInList(meta.custom.tags, q)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if any item in a list contains the query (case-insensitive).
     */
    private static boolean containsInList(List<String> items, String query) {
        if (items == null) return false;
        for (String item : items) {
            if (item.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }
}
