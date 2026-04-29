package com.cope.meteoraddons.systems;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AddonManager")
class AddonManagerTest {

    @Nested
    @DisplayName("extractFileName()")
    class ExtractFileName {

        @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
        @CsvSource({
            "https://github.com/owner/repo/releases/download/v1.0/addon-1.0.jar,  addon-1.0.jar",
            "https://example.com/path/to/file.jar?token=abc123,                    file.jar",
            "https://example.com/path/to/file.jar#section,                         file.jar",
            "https://example.com/path/to/file.jar?token=abc&sig=xyz#frag,          file.jar",
            "https://example.com/simple.jar,                                       simple.jar",
        })
        void extractsFilename(String url, String expected) {
            assertEquals(expected, AddonManager.extractFileName(url));
        }

        @Test
        @DisplayName("handles URL with only query string, no fragment")
        void queryOnly() {
            assertEquals("download.jar",
                AddonManager.extractFileName("https://cdn.example.com/download.jar?expires=12345"));
        }

        @Test
        @DisplayName("handles URL with only fragment, no query")
        void fragmentOnly() {
            assertEquals("addon.jar",
                AddonManager.extractFileName("https://example.com/addon.jar#sha256=abc"));
        }
    }
}
