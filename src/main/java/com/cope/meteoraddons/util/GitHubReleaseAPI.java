package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for GitHub Releases API to fetch asset digests and changelogs.
 * Uses unauthenticated requests (60 req/hour rate limit).
 */
public class GitHubReleaseAPI {
    private static final String API_BASE = "https://api.github.com/repos";
    private static final Gson gson = new Gson();

    // Pattern to extract owner/repo from GitHub URLs
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
        "github\\.com/([^/]+)/([^/]+)"
    );

    /**
     * Parse owner and repo from a GitHub URL.
     * Supports various formats:
     * - https://github.com/owner/repo
     * - https://github.com/owner/repo/releases/...
     */
    public static Optional<String[]> parseGitHubUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String owner = matcher.group(1);
            String repo = matcher.group(2);
            // Clean repo name (remove .git suffix if present)
            if (repo.endsWith(".git")) {
                repo = repo.substring(0, repo.length() - 4);
            }
            return Optional.of(new String[]{owner, repo});
        }

        return Optional.empty();
    }

    /**
     * Fetch the latest release info including asset digests and changelog.
     */
    public static Optional<ReleaseInfo> getLatestRelease(String owner, String repo) {
        String url = String.format("%s/%s/%s/releases/latest", API_BASE, owner, repo);

        try {
            String json = HttpClient.downloadString(url);
            Release release = gson.fromJson(json, Release.class);

            if (release == null) {
                return Optional.empty();
            }

            return Optional.of(new ReleaseInfo(
                release.tagName,
                release.name,
                release.body,
                release.assets
            ));
        } catch (IOException e) {
            MeteorAddonsAddon.LOG.warn("Failed to fetch latest release for {}/{}: {}", owner, repo, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find the JAR asset from a release that matches the current Minecraft version.
     * If multiple JARs exist, selects the one whose filename contains the current version
     * and returns empty if none match. Single-JAR releases are assumed version-agnostic.
     */
    public static Optional<AssetInfo> findJarAsset(ReleaseInfo release) {
        if (release.assets == null || release.assets.isEmpty()) {
            return Optional.empty();
        }

        List<Asset> jars = release.assets.stream()
            .filter(asset -> asset.name != null && asset.name.endsWith(".jar"))
            .toList();

        if (jars.isEmpty()) {
            return Optional.empty();
        }

        String currentVersion = VersionUtil.getCurrentMinecraftVersion();

        Optional<Asset> versionMatch = jars.stream()
            .filter(asset -> VersionUtil.containsVersion(asset.name, currentVersion))
            .findFirst();

        // Multi-JAR release with no version match — no safe fallback
        if (versionMatch.isEmpty() && jars.size() > 1) {
            return Optional.empty();
        }

        Asset match = versionMatch.orElse(jars.get(0));

        return Optional.of(new AssetInfo(
            match.name,
            match.browserDownloadUrl,
            HashUtil.parseGitHubDigest(match.digest),
            match.size
        ));
    }

    /**
     * Convenience method: fetch release and find JAR asset in one call.
     */
    public static Optional<AssetInfo> getLatestJarAsset(String owner, String repo) {
        return getLatestRelease(owner, repo).flatMap(GitHubReleaseAPI::findJarAsset);
    }

    /**
     * Get release info from a GitHub URL.
     */
    public static Optional<ReleaseInfo> getLatestReleaseFromUrl(String githubUrl) {
        return parseGitHubUrl(githubUrl)
            .flatMap(parts -> getLatestRelease(parts[0], parts[1]));
    }

    // JSON model classes for GitHub API response

    private static class Release {
        @SerializedName("tag_name")
        String tagName;

        String name;
        String body;
        List<Asset> assets;
    }

    private static class Asset {
        String name;

        @SerializedName("browser_download_url")
        String browserDownloadUrl;

        String digest;
        long size;
    }

    /**
     * Parsed release information.
     */
    public static class ReleaseInfo {
        private final String tagName;
        private final String name;
        private final String changelog;
        private final List<Asset> assets;

        ReleaseInfo(String tagName, String name, String changelog, List<Asset> assets) {
            this.tagName = tagName;
            this.name = name;
            this.changelog = changelog;
            this.assets = assets;
        }

        public String getTagName() {
            return tagName;
        }

        public String getName() {
            return name;
        }

        public String getChangelog() {
            return changelog != null ? changelog : "";
        }

        /**
         * Get version string (prefers tag name, falls back to release name).
         */
        public String getVersion() {
            if (tagName != null && !tagName.isEmpty()) {
                // Strip leading 'v' if present
                return tagName.startsWith("v") ? tagName.substring(1) : tagName;
            }
            return name;
        }
    }

    /**
     * Parsed asset information with digest.
     */
    public static class AssetInfo {
        private final String fileName;
        private final String downloadUrl;
        private final String sha256;
        private final long size;

        AssetInfo(String fileName, String downloadUrl, String sha256, long size) {
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
            this.size = size;
        }

        public String getFileName() {
            return fileName;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getSha256() {
            return sha256;
        }

        public long getSize() {
            return size;
        }
    }
}
