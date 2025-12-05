package com.cope.meteoraddons.addons;

import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.util.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Online addon wrapper for scanner metadata.
 */
public class OnlineAddon implements Addon {
    private final AddonMetadata metadata;

    public OnlineAddon(AddonMetadata metadata) {
        this.metadata = metadata;
    }

    public AddonMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String getName() {
        return metadata.name;
    }

    @Override
    public String getId() {
        return metadata.name.toLowerCase().replace(" ", "-");
    }

    @Override
    public Optional<String> getDescription() {
        String desc = metadata.getDisplayDescription();
        return desc != null && !desc.isEmpty() ? Optional.of(desc) : Optional.empty();
    }

    @Override
    public List<String> getAuthors() {
        return metadata.authors != null ? metadata.authors : List.of();
    }

    @Override
    public String getVersion() {
        if (metadata.mc_version != null) {
            return metadata.mc_version;
        }
        return "Unknown";
    }

    @Override
    public Optional<InputStream> getIconStream() throws IOException {
        String iconUrl = metadata.getIconUrl();
        if (iconUrl == null || iconUrl.isEmpty()) {
            return Optional.empty();
        }

        try {
            byte[] iconData = HttpClient.downloadBytes(iconUrl);
            return Optional.of(new ByteArrayInputStream(iconData));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getGithubUrl() {
        if (metadata.links != null && metadata.links.github != null && !metadata.links.github.isEmpty()) {
            return Optional.of(metadata.links.github);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getDiscordUrl() {
        String discord = metadata.getDiscordUrl();
        return discord != null && !discord.isEmpty() ? Optional.of(discord) : Optional.empty();
    }

    @Override
    public Optional<String> getHomepageUrl() {
        String homepage = metadata.getHomepageUrl();
        return homepage != null && !homepage.isEmpty() ? Optional.of(homepage) : Optional.empty();
    }

    @Override
    public boolean isInstalled() {
        return AddonManager.get().isInstalled(metadata.name);
    }
}
