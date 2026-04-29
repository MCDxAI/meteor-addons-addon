package com.cope.meteoraddons.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UpdateInfo")
class UpdateInfoTest {

    private static UpdateInfo withNewVersion(String newVersion) {
        return new UpdateInfo(null, "TestAddon", "1.0.0", newVersion, "", "", "", "", null);
    }

    @Test
    @DisplayName("displays new version when available")
    void showsNewVersion() {
        assertEquals("build-7", withNewVersion("build-7").getVersionChangeDisplay());
    }

    @Test
    @DisplayName("displays 'Update available' when new version is null")
    void fallbackOnNull() {
        assertEquals("Update available", withNewVersion(null).getVersionChangeDisplay());
    }

    @Test
    @DisplayName("displays 'Update available' when new version is empty")
    void fallbackOnEmpty() {
        assertEquals("Update available", withNewVersion("").getVersionChangeDisplay());
    }
}
