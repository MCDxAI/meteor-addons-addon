package com.cope.meteoraddons.models;

import com.cope.meteoraddons.addons.InstalledAddon;

import java.nio.file.Path;

/**
 * Holds information about an available addon update.
 */
public class UpdateInfo {
    private final InstalledAddon installedAddon;
    private final String addonName;
    private final String currentVersion;
    private final String newVersion;
    private final String changelog;
    private final String downloadUrl;
    private final String remoteHash;
    private final String localHash;
    private final Path localJarPath;

    public UpdateInfo(
        InstalledAddon installedAddon,
        String addonName,
        String currentVersion,
        String newVersion,
        String changelog,
        String downloadUrl,
        String remoteHash,
        String localHash,
        Path localJarPath
    ) {
        this.installedAddon = installedAddon;
        this.addonName = addonName;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.changelog = changelog;
        this.downloadUrl = downloadUrl;
        this.remoteHash = remoteHash;
        this.localHash = localHash;
        this.localJarPath = localJarPath;
    }

    public InstalledAddon getInstalledAddon() {
        return installedAddon;
    }

    public String getAddonName() {
        return addonName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getRemoteHash() {
        return remoteHash;
    }

    public String getLocalHash() {
        return localHash;
    }

    public Path getLocalJarPath() {
        return localJarPath;
    }

    /**
     * Get a display string showing version change.
     */
    public String getVersionChangeDisplay() {
        if (currentVersion != null && newVersion != null && !currentVersion.equals(newVersion)) {
            return currentVersion + " → " + newVersion;
        }
        return "Update available";
    }
}
