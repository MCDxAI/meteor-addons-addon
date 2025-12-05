package com.cope.meteoraddons.addons;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Unified interface for addon types (online, installed).
 */
public interface Addon {
    String getName();
    String getId();
    Optional<String> getDescription();
    List<String> getAuthors();
    String getVersion();
    Optional<InputStream> getIconStream() throws IOException;
    Optional<String> getGithubUrl();
    Optional<String> getDiscordUrl();
    Optional<String> getHomepageUrl();
    boolean isInstalled();
}
