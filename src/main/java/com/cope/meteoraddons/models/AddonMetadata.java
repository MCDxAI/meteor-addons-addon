package com.cope.meteoraddons.models;

import com.cope.meteoraddons.util.VersionUtil;

import java.util.List;

/**
 * Represents metadata for a Meteor addon from the addon scanner.
 * Maps to the JSON structure from:
 * https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/main/addons.json
 */
public class AddonMetadata {
    public String name;
    public String description;
    public String mc_version;
    public List<String> authors;
    public Features features;
    public boolean verified;
    public Repository repo;
    public Links links;
    public CustomMetadata custom;

    /**
     * Check if this addon supports the current Minecraft version.
     * Prioritizes custom.supported_versions, falls back to mc_version.
     *
     * @return true if the addon supports the current version
     */
    public boolean supportsCurrentVersion() {
        String currentVersion = VersionUtil.getCurrentMinecraftVersion();

        // Check custom.supported_versions first (array of versions)
        if (custom != null && custom.supported_versions != null && !custom.supported_versions.isEmpty()) {
            for (String version : custom.supported_versions) {
                if (currentVersion.equals(version)) {
                    return true;
                }
            }
            return false;
        }

        // Fall back to mc_version (single version string)
        return currentVersion.equals(mc_version);
    }

    /**
     * Get the display description, prioritizing custom description.
     *
     * @return description string
     */
    public String getDisplayDescription() {
        if (custom != null && custom.description != null && !custom.description.isEmpty()) {
            return custom.description;
        }
        return description != null ? description : "";
    }

    /**
     * Get the icon URL, prioritizing custom icon.
     *
     * @return icon URL or null if not available
     */
    public String getIconUrl() {
        if (custom != null && custom.icon != null && !custom.icon.isEmpty()) {
            return custom.icon;
        }
        if (links != null && links.icon != null && !links.icon.isEmpty()) {
            return links.icon;
        }
        return null;
    }

    /**
     * Get the discord URL, prioritizing custom discord.
     *
     * @return discord URL or null if not available
     */
    public String getDiscordUrl() {
        if (custom != null && custom.discord != null && !custom.discord.isEmpty()) {
            return custom.discord;
        }
        if (links != null && links.discord != null && !links.discord.isEmpty()) {
            return links.discord;
        }
        return null;
    }

    /**
     * Get the homepage URL, prioritizing custom homepage.
     *
     * @return homepage URL or null if not available
     */
    public String getHomepageUrl() {
        if (custom != null && custom.homepage != null && !custom.homepage.isEmpty()) {
            return custom.homepage;
        }
        if (links != null && links.homepage != null && !links.homepage.isEmpty()) {
            return links.homepage;
        }
        return null;
    }

    /**
     * Get download URLs for this addon.
     * Filters to only return downloads matching the current Minecraft version.
     *
     * @return array of download URLs
     */
    public String[] getDownloadUrls() {
        if (links == null || links.downloads == null || links.downloads.isEmpty()) {
            return new String[0];
        }

        String currentVersion = VersionUtil.getCurrentMinecraftVersion();

        // Filter downloads that contain the current version in the filename
        return links.downloads.stream()
            .filter(url -> url != null && url.contains(currentVersion))
            .toArray(String[]::new);
    }

    public static class Features {
        public List<String> modules;
        public List<String> commands;
        public List<String> hud_elements;
        public int feature_count;
    }

    public static class Repository {
        public String id;
        public String owner;
        public String name;
        public boolean archived;
        public boolean fork;
        public int stars;
        public int downloads;
        public String last_update;
        public String creation_date;
    }

    public static class Links {
        public String github;
        public List<String> downloads;
        public String discord;
        public String homepage;
        public String icon;
    }

    public static class CustomMetadata {
        public String description;
        public List<String> tags;
        public List<String> supported_versions;
        public String icon;
        public String discord;
        public String homepage;
    }
}
