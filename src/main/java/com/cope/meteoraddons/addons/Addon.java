package com.cope.meteoraddons.addons;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Unified interface for all addon types (online, installed, etc).
 * Provides common methods for displaying and managing addons.
 */
public interface Addon {
    /**
     * Get the display name of the addon.
     */
    String getName();

    /**
     * Get the unique identifier of the addon.
     */
    String getId();

    /**
     * Get the addon description.
     */
    Optional<String> getDescription();

    /**
     * Get the list of addon authors.
     */
    List<String> getAuthors();

    /**
     * Get the addon version.
     */
    String getVersion();

    /**
     * Get an input stream for the addon icon.
     * May download remotely or read from local file.
     */
    Optional<InputStream> getIconStream() throws IOException;

    /**
     * Get the GitHub repository URL.
     */
    Optional<String> getGithubUrl();

    /**
     * Get the Discord server URL.
     */
    Optional<String> getDiscordUrl();

    /**
     * Get the homepage URL.
     */
    Optional<String> getHomepageUrl();

    /**
     * Check if this addon is currently installed.
     */
    boolean isInstalled();
}
