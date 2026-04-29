package com.cope.meteoraddons.util;

import com.cope.meteoraddons.util.GitHubReleaseAPI.AssetInfo;
import com.cope.meteoraddons.util.GitHubReleaseAPI.ReleaseInfo;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubReleaseAPI")
class GitHubReleaseAPITest {

    @AfterEach
    void resetVersion() {
        VersionUtil.setCurrentVersionForTest(null);
    }

    private static GitHubReleaseAPI.Asset makeAsset(String name, String downloadUrl) {
        GitHubReleaseAPI.Asset asset = new GitHubReleaseAPI.Asset();
        asset.name = name;
        asset.browserDownloadUrl = downloadUrl;
        asset.digest = null;
        asset.size = 1000;
        return asset;
    }

    @Nested
    @DisplayName("findJarAsset()")
    class FindJarAsset {

        @Test
        @DisplayName("selects version-matching JAR from multi-JAR release")
        void selectsCorrectVersionFromMultiJar() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            ReleaseInfo release = new ReleaseInfo("build-7", "Build 7", "changelog", List.of(
                makeAsset("nora-tweaks-1.21.10-build-7.jar", "https://example.com/nora-tweaks-1.21.10-build-7.jar"),
                makeAsset("nora-tweaks-1.21.11-build-7.jar", "https://example.com/nora-tweaks-1.21.11-build-7.jar")
            ));

            Optional<AssetInfo> result = GitHubReleaseAPI.findJarAsset(release);

            assertTrue(result.isPresent());
            assertEquals("nora-tweaks-1.21.11-build-7.jar", result.get().getFileName());
        }

        @Test
        @DisplayName("returns empty for multi-JAR release with no version match")
        void emptyWhenNoVersionMatchInMultiJar() {
            VersionUtil.setCurrentVersionForTest("1.21.4");

            ReleaseInfo release = new ReleaseInfo("build-7", "Build 7", "changelog", List.of(
                makeAsset("nora-tweaks-1.21.10-build-7.jar", "https://example.com/a.jar"),
                makeAsset("nora-tweaks-1.21.11-build-7.jar", "https://example.com/b.jar")
            ));

            Optional<AssetInfo> result = GitHubReleaseAPI.findJarAsset(release);

            assertTrue(result.isEmpty(), "Should not fall back to wrong-version JAR in multi-JAR release");
        }

        @Test
        @DisplayName("falls back to single JAR in version-agnostic release")
        void fallbackForSingleJarRelease() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            ReleaseInfo release = new ReleaseInfo("v1.0.0", "Release 1.0.0", "changelog", List.of(
                makeAsset("meteor-mcp-1.0.0.jar", "https://example.com/meteor-mcp-1.0.0.jar")
            ));

            Optional<AssetInfo> result = GitHubReleaseAPI.findJarAsset(release);

            assertTrue(result.isPresent(), "Single-JAR releases should be treated as version-agnostic");
            assertEquals("meteor-mcp-1.0.0.jar", result.get().getFileName());
        }

        @Test
        @DisplayName("returns empty when release has no assets")
        void emptyWhenNoAssets() {
            ReleaseInfo release = new ReleaseInfo("v1.0", "1.0", "", List.of());
            assertTrue(GitHubReleaseAPI.findJarAsset(release).isEmpty());
        }

        @Test
        @DisplayName("returns empty when release has no JAR assets")
        void emptyWhenNoJarAssets() {
            ReleaseInfo release = new ReleaseInfo("v1.0", "1.0", "", List.of(
                makeAsset("source.zip", "https://example.com/source.zip"),
                makeAsset("checksums.txt", "https://example.com/checksums.txt")
            ));
            assertTrue(GitHubReleaseAPI.findJarAsset(release).isEmpty());
        }

        @Test
        @DisplayName("does not confuse 1.21.1 with 1.21.10 in multi-JAR release")
        void versionBoundaryInMultiJar() {
            VersionUtil.setCurrentVersionForTest("1.21.1");

            ReleaseInfo release = new ReleaseInfo("build-5", "Build 5", "", List.of(
                makeAsset("addon-1.21.10-build-5.jar", "https://example.com/a.jar"),
                makeAsset("addon-1.21.11-build-5.jar", "https://example.com/b.jar"),
                makeAsset("addon-1.21.1-build-5.jar", "https://example.com/c.jar")
            ));

            Optional<AssetInfo> result = GitHubReleaseAPI.findJarAsset(release);

            assertTrue(result.isPresent());
            assertEquals("addon-1.21.1-build-5.jar", result.get().getFileName());
        }

        @Test
        @DisplayName("returns null assets gracefully")
        void handlesNullAssets() {
            ReleaseInfo release = new ReleaseInfo("v1.0", "1.0", "", null);
            assertTrue(GitHubReleaseAPI.findJarAsset(release).isEmpty());
        }
    }

    @Nested
    @DisplayName("parseGitHubUrl()")
    class ParseGitHubUrl {

        @Test
        @DisplayName("parses standard GitHub URL")
        void standardUrl() {
            Optional<String[]> result = GitHubReleaseAPI.parseGitHubUrl("https://github.com/noramibu/Nora-Tweaks");
            assertTrue(result.isPresent());
            assertEquals("noramibu", result.get()[0]);
            assertEquals("Nora-Tweaks", result.get()[1]);
        }

        @Test
        @DisplayName("parses URL with .git suffix")
        void gitSuffix() {
            Optional<String[]> result = GitHubReleaseAPI.parseGitHubUrl("https://github.com/owner/repo.git");
            assertTrue(result.isPresent());
            assertEquals("repo", result.get()[1]);
        }

        @Test
        @DisplayName("parses URL with release path")
        void releasePath() {
            Optional<String[]> result = GitHubReleaseAPI.parseGitHubUrl(
                "https://github.com/owner/repo/releases/tag/v1.0");
            assertTrue(result.isPresent());
            assertEquals("owner", result.get()[0]);
            assertEquals("repo", result.get()[1]);
        }

        @Test
        @DisplayName("returns empty for null/empty input")
        void nullAndEmpty() {
            assertTrue(GitHubReleaseAPI.parseGitHubUrl(null).isEmpty());
            assertTrue(GitHubReleaseAPI.parseGitHubUrl("").isEmpty());
        }

        @Test
        @DisplayName("returns empty for non-GitHub URLs")
        void nonGitHub() {
            assertTrue(GitHubReleaseAPI.parseGitHubUrl("https://gitlab.com/owner/repo").isEmpty());
        }
    }

    @Nested
    @DisplayName("ReleaseInfo.getVersion()")
    class GetVersion {

        @Test
        @DisplayName("strips leading 'v' from tag name")
        void stripsLeadingV() {
            ReleaseInfo release = new ReleaseInfo("v1.2.3", "Release 1.2.3", "", List.of());
            assertEquals("1.2.3", release.getVersion());
        }

        @Test
        @DisplayName("returns tag name as-is when no leading 'v'")
        void noLeadingV() {
            ReleaseInfo release = new ReleaseInfo("build-7", "Build 7", "", List.of());
            assertEquals("build-7", release.getVersion());
        }

        @Test
        @DisplayName("falls back to release name when tag is null")
        void fallsBackToName() {
            ReleaseInfo release = new ReleaseInfo(null, "Release Name", "", List.of());
            assertEquals("Release Name", release.getVersion());
        }
    }
}
