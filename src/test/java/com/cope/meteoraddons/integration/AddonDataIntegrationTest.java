package com.cope.meteoraddons.integration;

import com.cope.meteoraddons.models.AddonMetadata;
import com.cope.meteoraddons.util.VersionUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that fetch real addon data from cqb13's addons.json
 * and verify version filtering logic against multiple Minecraft versions.
 *
 * Uses raw.githubusercontent.com (no rate limit, no auth required).
 * Tagged "network" so they can be excluded from offline builds.
 */
@Tag("network")
@DisplayName("Addon Data Integration")
class AddonDataIntegrationTest {

    private static final String ADDONS_JSON_URL =
        "https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/addons/addons.json";

    /** Versions to simulate — covers the boundary-matching edge cases. */
    private static final String[] TEST_VERSIONS = {
        "1.21.1", "1.21.10", "1.21.11"
    };

    private static List<AddonMetadata> allAddons;
    private static boolean fetchFailed = false;

    @BeforeAll
    static void fetchAddonsJson() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ADDONS_JSON_URL))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "Failed to fetch addons.json");

            Type listType = new TypeToken<List<AddonMetadata>>(){}.getType();
            allAddons = new Gson().fromJson(response.body(), listType);

            assertNotNull(allAddons);
            assertFalse(allAddons.isEmpty(), "addons.json should not be empty");

            System.out.printf("Fetched %d addons from addons.json%n", allAddons.size());
        } catch (IOException | InterruptedException e) {
            fetchFailed = true;
            fail("Could not fetch addons.json: " + e.getMessage());
        }
    }

    @AfterEach
    void resetVersion() {
        VersionUtil.setCurrentVersionForTest(null);
    }

    @Test
    @DisplayName("addons.json parses without errors and contains expected fields")
    void parsesCorrectly() {
        for (AddonMetadata addon : allAddons) {
            assertNotNull(addon.name, "Every addon must have a name");
        }

        long withLinks = allAddons.stream().filter(a -> a.links != null).count();
        assertTrue(withLinks > 0, "At least some addons should have links");

        System.out.printf("  %d/%d addons have links%n", withLinks, allAddons.size());
    }

    @Test
    @DisplayName("no download URL cross-contamination between similar versions")
    void noVersionCrossContamination() {
        List<String> violations = new ArrayList<>();

        for (String version : TEST_VERSIONS) {
            VersionUtil.setCurrentVersionForTest(version);

            for (AddonMetadata addon : allAddons) {
                String[] urls = addon.getDownloadUrls();

                for (String url : urls) {
                    // Check every other test version — the URL must NOT contain them
                    for (String otherVersion : TEST_VERSIONS) {
                        if (otherVersion.equals(version)) continue;

                        // Only flag if the URL contains the OTHER version but NOT the current one
                        // (i.e., a boundary-matching failure)
                        if (VersionUtil.containsVersion(url, otherVersion)
                                && !VersionUtil.containsVersion(url, version)) {
                            violations.add(String.format(
                                "  [MC %s] %s: URL '%s' matches version %s instead",
                                version, addon.name, url, otherVersion
                            ));
                        }
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("Version cross-contamination detected:\n" + String.join("\n", violations));
        }
    }

    @Test
    @DisplayName("shared URLs between versions are multi-version JARs, not false matches")
    void sharedUrlsAreIntentionalMultiVersion() {
        // Some addons publish a single JAR covering multiple MC versions
        // (e.g. "addon-1.21.10-1.21.11.jar"). These legitimately match both versions.
        // This test verifies that any shared URL actually contains BOTH version strings,
        // ruling out false substring matches.
        Map<String, Map<String, Set<String>>> addonVersionUrls = new LinkedHashMap<>();

        for (AddonMetadata addon : allAddons) {
            Map<String, Set<String>> versionUrls = new LinkedHashMap<>();

            for (String version : TEST_VERSIONS) {
                VersionUtil.setCurrentVersionForTest(version);
                Set<String> urls = new LinkedHashSet<>(Arrays.asList(addon.getDownloadUrls()));
                versionUrls.put(version, urls);
            }

            long versionsWithUrls = versionUrls.values().stream().filter(s -> !s.isEmpty()).count();
            if (versionsWithUrls > 1) {
                addonVersionUrls.put(addon.name, versionUrls);
            }
        }

        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : addonVersionUrls.entrySet()) {
            String addonName = entry.getKey();
            Map<String, Set<String>> versionUrls = entry.getValue();

            for (int i = 0; i < TEST_VERSIONS.length; i++) {
                for (int j = i + 1; j < TEST_VERSIONS.length; j++) {
                    Set<String> urlsA = versionUrls.get(TEST_VERSIONS[i]);
                    Set<String> urlsB = versionUrls.get(TEST_VERSIONS[j]);

                    Set<String> intersection = new HashSet<>(urlsA);
                    intersection.retainAll(urlsB);

                    for (String sharedUrl : intersection) {
                        // A shared URL must contain BOTH versions — that's an intentional
                        // multi-version JAR. If it only contains one, it's a matching bug.
                        boolean containsBoth =
                            VersionUtil.containsVersion(sharedUrl, TEST_VERSIONS[i])
                            && VersionUtil.containsVersion(sharedUrl, TEST_VERSIONS[j]);

                        if (!containsBoth) {
                            violations.add(String.format(
                                "  %s: URL shared between %s and %s but doesn't contain both: %s",
                                addonName, TEST_VERSIONS[i], TEST_VERSIONS[j], sharedUrl
                            ));
                        }
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("URL matched multiple versions without containing both:\n"
                + String.join("\n", violations));
        }
    }

    @Test
    @DisplayName("no duplicate URLs in any single addon's download list")
    void noDuplicateUrls() {
        List<String> violations = new ArrayList<>();

        for (String version : TEST_VERSIONS) {
            VersionUtil.setCurrentVersionForTest(version);

            for (AddonMetadata addon : allAddons) {
                String[] urls = addon.getDownloadUrls();
                Set<String> unique = new LinkedHashSet<>(Arrays.asList(urls));

                if (unique.size() != urls.length) {
                    violations.add(String.format(
                        "  [MC %s] %s: %d URLs but only %d unique",
                        version, addon.name, urls.length, unique.size()
                    ));
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("Duplicate download URLs found:\n" + String.join("\n", violations));
        }
    }

    @Test
    @DisplayName("version filtering summary for current test versions")
    void versionFilteringSummary() {
        System.out.println("\n=== Version Filtering Summary ===");

        for (String version : TEST_VERSIONS) {
            VersionUtil.setCurrentVersionForTest(version);

            long supported = allAddons.stream()
                .filter(AddonMetadata::supportsCurrentVersion)
                .count();

            long withDownloads = allAddons.stream()
                .filter(a -> a.getDownloadUrls().length > 0)
                .count();

            System.out.printf("  MC %-8s → %3d supported, %3d with download URLs%n",
                version, supported, withDownloads);
        }
    }
}
