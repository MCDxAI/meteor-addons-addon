package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing SHA256 hashes of files.
 */
public class HashUtil {
    private static final int BUFFER_SIZE = 8192;

    /**
     * Compute SHA256 hash of a file.
     *
     * @param path Path to the file
     * @return SHA256 hash as hex string, or null on failure
     */
    public static String computeSha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            MeteorAddonsAddon.LOG.error("Failed to compute SHA256 for {}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Parse SHA256 hash from GitHub's digest format.
     * GitHub returns: "sha256:abc123..."
     * We want just: "abc123..."
     */
    public static String parseGitHubDigest(String digest) {
        if (digest == null || digest.isEmpty()) {
            return null;
        }

        if (digest.startsWith("sha256:")) {
            return digest.substring(7);
        }

        return digest;
    }

    /**
     * Compare two hashes (case-insensitive).
     */
    public static boolean hashesMatch(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return false;
        }
        return hash1.equalsIgnoreCase(hash2);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
