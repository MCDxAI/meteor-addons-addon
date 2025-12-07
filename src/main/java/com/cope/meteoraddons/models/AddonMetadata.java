package com.cope.meteoraddons.models;

import com.cope.meteoraddons.util.VersionUtil;

import java.util.List;
import java.util.ArrayList;

/**
 * Meteor addon metadata from scanner JSON.
 * Source: https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/main/addons.json
 */
public class AddonMetadata {
    /**
     * Get the first non-null, non-empty string from the provided values.
     * Used for priority-based fallback logic (custom fields override regular fields).
     */
    private static String getFirstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
    public String name;
    public String description;
    public String mc_version;
    public List<String> authors;
    public Features features;
    public boolean verified;
    public Repository repo;
    public Links links;
    public CustomMetadata custom;

    public boolean supportsCurrentVersion() {
        String currentVersion = VersionUtil.getCurrentMinecraftVersion();

        if (custom != null && custom.supported_versions != null && !custom.supported_versions.isEmpty()) {
            for (String version : custom.supported_versions) {
                if (currentVersion.equals(version)) {
                    return true;
                }
            }
            return false;
        }

        return currentVersion.equals(mc_version);
    }

    public String getDisplayDescription() {
        String desc = getFirstNonEmpty(
            custom != null ? custom.description : null,
            description
        );
        return desc != null ? desc : "";
    }

    public String getIconUrl() {
        return getFirstNonEmpty(
            custom != null ? custom.icon : null,
            links != null ? links.icon : null
        );
    }

    public String getDiscordUrl() {
        return getFirstNonEmpty(
            custom != null ? custom.discord : null,
            links != null ? links.discord : null
        );
    }

    public String getHomepageUrl() {
        return getFirstNonEmpty(
            custom != null ? custom.homepage : null,
            links != null ? links.homepage : null
        );
    }

    public String[] getDownloadUrls() {
        if (links == null) {
            return new String[0];
        }

        List<String> urls = new ArrayList<>();

        // Priority 1: Latest Release
        if (links.latest_release != null && !links.latest_release.isEmpty()) {
            urls.add(links.latest_release);
        }

        // Priority 2: Compatible downloads from the list
        if (links.downloads != null && !links.downloads.isEmpty()) {
            String currentVersion = VersionUtil.getCurrentMinecraftVersion();
            
            links.downloads.stream()
                .filter(url -> url != null && url.contains(currentVersion))
                .forEach(urls::add);
        }

        return urls.toArray(String[]::new);
    }

    public static class Features {
        public List<String> modules;
        public List<String> commands;
        public List<String> hud_elements;
        public List<String> custom_screens;
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
        public String latest_release;
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
