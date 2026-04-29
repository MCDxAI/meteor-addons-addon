package com.cope.meteoraddons.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VersionUtil")
class VersionUtilTest {

    @Nested
    @DisplayName("containsVersion()")
    class ContainsVersion {

        @ParameterizedTest(name = "\"{0}\" contains version \"{1}\" → true")
        @CsvSource({
            // Exact match at end of string
            "nora-tweaks-1.21.11.jar,           1.21.11",
            // Version followed by non-digit separator
            "nora-tweaks-1.21.11-build-7.jar,   1.21.11",
            // Version followed by dot (non-digit)
            "addon-1.21.1.jar,                  1.21.1",
            // Version at very end (no extension)
            "addon-1.21.10,                     1.21.10",
            // Version in the middle of URL path
            "https://example.com/releases/download/1.21.11-build-7/addon.jar, 1.21.11",
            // Single-digit version
            "addon-1.jar,                       1",
        })
        void shouldMatchExactVersion(String text, String version) {
            assertTrue(VersionUtil.containsVersion(text, version));
        }

        @ParameterizedTest(name = "\"{0}\" contains version \"{1}\" → false")
        @CsvSource({
            // 1.21.1 must NOT match inside 1.21.10
            "nora-tweaks-1.21.10-build-7.jar,   1.21.1",
            // 1.21.1 must NOT match inside 1.21.11
            "nora-tweaks-1.21.11-build-7.jar,   1.21.1",
            // 1.21.10 must NOT match inside 1.21.100
            "addon-1.21.100.jar,                1.21.10",
            // Version not present at all
            "some-random-addon.jar,             1.21.11",
            // Partial prefix match (digit follows)
            "addon-1.21.112.jar,                1.21.11",
        })
        void shouldRejectSubstringMatch(String text, String version) {
            assertFalse(VersionUtil.containsVersion(text, version));
        }

        @Test
        @DisplayName("handles empty and null-like edge cases")
        void edgeCases() {
            assertFalse(VersionUtil.containsVersion("", "1.21.1"));
            assertTrue(VersionUtil.containsVersion("1.21.1", "1.21.1"));
        }

        @Test
        @DisplayName("finds version on second occurrence when first is a prefix")
        void secondOccurrenceMatch() {
            // First "1.21.1" is followed by "0" (reject), but second is exact
            assertTrue(VersionUtil.containsVersion("1.21.10-and-1.21.1.jar", "1.21.1"));
        }
    }
}
