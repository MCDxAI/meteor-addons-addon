package com.cope.meteoraddons.models;

import com.cope.meteoraddons.util.VersionUtil;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AddonMetadata")
class AddonMetadataTest {

    @AfterEach
    void resetVersion() {
        VersionUtil.setCurrentVersionForTest(null);
    }

    // -- Helpers to build test fixtures --

    private static AddonMetadata.Links linksWithDownloads(String latestRelease, String... downloads) {
        AddonMetadata.Links links = new AddonMetadata.Links();
        links.latest_release = latestRelease;
        links.downloads = Arrays.asList(downloads);
        return links;
    }

    private static AddonMetadata metadataWithLinks(String latestRelease, String... downloads) {
        AddonMetadata m = new AddonMetadata();
        m.links = linksWithDownloads(latestRelease, downloads);
        return m;
    }

    @Nested
    @DisplayName("getDownloadUrls()")
    class GetDownloadUrls {

        @Test
        @DisplayName("returns matching latest_release as priority 1")
        void latestReleaseMatchesMcVersion() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            AddonMetadata m = metadataWithLinks(
                "https://example.com/releases/download/build-7/addon-1.21.11-build-7.jar",
                "https://example.com/releases/download/build-7/addon-1.21.10-build-7.jar",
                "https://example.com/releases/download/build-7/addon-1.21.11-build-7.jar"
            );

            String[] urls = m.getDownloadUrls();

            assertEquals(1, urls.length, "latest_release + duplicate from downloads should be deduped");
            assertTrue(urls[0].contains("1.21.11"));
        }

        @Test
        @DisplayName("skips latest_release when it doesn't match current MC version")
        void latestReleaseSkippedOnVersionMismatch() {
            VersionUtil.setCurrentVersionForTest("1.21.10");

            AddonMetadata m = metadataWithLinks(
                "https://example.com/addon-1.21.11-build-7.jar",
                "https://example.com/addon-1.21.10-build-7.jar",
                "https://example.com/addon-1.21.11-build-7.jar"
            );

            String[] urls = m.getDownloadUrls();

            assertEquals(1, urls.length);
            assertTrue(urls[0].contains("1.21.10"));
        }

        @Test
        @DisplayName("prevents 1.21.1 from matching 1.21.10 or 1.21.11 URLs")
        void substringVersionSafety() {
            VersionUtil.setCurrentVersionForTest("1.21.1");

            AddonMetadata m = metadataWithLinks(
                "https://example.com/addon-1.21.11-build-7.jar",
                "https://example.com/addon-1.21.10-build-7.jar",
                "https://example.com/addon-1.21.11-build-7.jar"
            );

            String[] urls = m.getDownloadUrls();

            assertEquals(0, urls.length, "No URLs should match 1.21.1 when only 1.21.10/1.21.11 exist");
        }

        @Test
        @DisplayName("deduplicates latest_release appearing in downloads list")
        void deduplicatesLatestRelease() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            String sharedUrl = "https://example.com/addon-1.21.11-build-7.jar";
            AddonMetadata m = metadataWithLinks(sharedUrl, sharedUrl);

            String[] urls = m.getDownloadUrls();

            assertEquals(1, urls.length, "Same URL should not appear twice");
        }

        @Test
        @DisplayName("returns multiple compatible downloads when available")
        void multipleCompatibleDownloads() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            AddonMetadata m = metadataWithLinks(
                null,
                "https://example.com/addon-1.21.11-build-6.jar",
                "https://example.com/addon-1.21.11-build-7.jar"
            );

            String[] urls = m.getDownloadUrls();

            assertEquals(2, urls.length);
        }

        @Test
        @DisplayName("returns empty array when links is null")
        void nullLinks() {
            AddonMetadata m = new AddonMetadata();
            m.links = null;
            assertEquals(0, m.getDownloadUrls().length);
        }

        @Test
        @DisplayName("returns empty array when no URLs match current version")
        void noMatchingUrls() {
            VersionUtil.setCurrentVersionForTest("1.20.4");

            AddonMetadata m = metadataWithLinks(
                "https://example.com/addon-1.21.11.jar",
                "https://example.com/addon-1.21.10.jar"
            );

            assertEquals(0, m.getDownloadUrls().length);
        }
    }

    @Nested
    @DisplayName("supportsCurrentVersion()")
    class SupportsCurrentVersion {

        @Test
        @DisplayName("matches via custom.supported_versions")
        void customSupportedVersions() {
            VersionUtil.setCurrentVersionForTest("1.21.11");

            AddonMetadata m = new AddonMetadata();
            m.mc_version = "1.21.10";
            m.custom = new AddonMetadata.CustomMetadata();
            m.custom.supported_versions = List.of("1.21.10", "1.21.11");

            assertTrue(m.supportsCurrentVersion());
        }

        @Test
        @DisplayName("rejects when version not in custom.supported_versions")
        void customSupportedVersionsMismatch() {
            VersionUtil.setCurrentVersionForTest("1.21.9");

            AddonMetadata m = new AddonMetadata();
            m.mc_version = "1.21.10";
            m.custom = new AddonMetadata.CustomMetadata();
            m.custom.supported_versions = List.of("1.21.10", "1.21.11");

            assertFalse(m.supportsCurrentVersion());
        }

        @Test
        @DisplayName("falls back to mc_version when custom is null")
        void fallbackToMcVersion() {
            VersionUtil.setCurrentVersionForTest("1.21.10");

            AddonMetadata m = new AddonMetadata();
            m.mc_version = "1.21.10";
            m.custom = null;

            assertTrue(m.supportsCurrentVersion());
        }

        @Test
        @DisplayName("exact match only — no substring matching on mc_version")
        void exactMatchOnly() {
            VersionUtil.setCurrentVersionForTest("1.21.1");

            AddonMetadata m = new AddonMetadata();
            m.mc_version = "1.21.10";
            m.custom = null;

            assertFalse(m.supportsCurrentVersion());
        }
    }
}
