package com.cope.meteoraddons.addons;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents an installed Meteor addon loaded by Fabric.
 * Wraps Fabric's ModContainer to provide addon interface.
 */
public class InstalledAddon implements Addon {
    private final ModContainer modContainer;
    private final ModMetadata metadata;

    public InstalledAddon(ModContainer modContainer) {
        this.modContainer = modContainer;
        this.metadata = modContainer.getMetadata();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(metadata.getDescription()).filter(s -> !s.isEmpty());
    }

    @Override
    public List<String> getAuthors() {
        return metadata.getAuthors().stream()
            .map(Person::getName)
            .collect(Collectors.toList());
    }

    @Override
    public String getVersion() {
        return metadata.getVersion().getFriendlyString();
    }

    @Override
    public Optional<InputStream> getIconStream() throws IOException {
        // Try multiple icon paths in order
        String[] iconPaths = {
            metadata.getIconPath(128).orElse(null),
            metadata.getIconPath(64).orElse(null),
            metadata.getIconPath(32).orElse(null),
            "icon.png",
            "assets/" + metadata.getId() + "/icon.png"
        };

        for (String iconPath : iconPaths) {
            if (iconPath == null || iconPath.isEmpty()) continue;

            Optional<java.nio.file.Path> pathOpt = modContainer.findPath(iconPath);
            if (pathOpt.isPresent()) {
                try {
                    InputStream stream = java.nio.file.Files.newInputStream(pathOpt.get());
                    return Optional.of(stream);
                } catch (IOException e) {
                    // Try next path
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getGithubUrl() {
        // Try to find GitHub URL in contact info
        return metadata.getContact().get("sources")
            .or(() -> metadata.getContact().get("homepage"))
            .filter(url -> url.contains("github.com"));
    }

    @Override
    public Optional<String> getDiscordUrl() {
        // Try to find Discord URL in contact info
        return metadata.getContact().get("discord")
            .or(() -> metadata.getContact().get("issues"))
            .filter(url -> url.contains("discord"));
    }

    @Override
    public Optional<String> getHomepageUrl() {
        return metadata.getContact().get("homepage");
    }

    @Override
    public boolean isInstalled() {
        return true; // Always true for installed addons
    }

    public ModContainer getModContainer() {
        return modContainer;
    }
}
