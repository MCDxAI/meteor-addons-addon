package com.cope.meteoraddons.util;

import com.cope.meteoraddons.MeteorAddonsAddon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client utility for downloading addon metadata and files.
 * Uses OkHttp for reliable, efficient HTTP operations.
 */
public class HttpClient {
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    /**
     * Download a URL's content as a string.
     *
     * @param url URL to download from
     * @return response body as string
     * @throws IOException if the request fails
     */
    public static String downloadString(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            return body.string();
        }
    }

    /**
     * Download a file from a URL to a local path.
     * Creates parent directories if they don't exist.
     *
     * @param url      URL to download from
     * @param destPath destination file path
     * @throws IOException if the download or file write fails
     */
    public static void downloadFile(String url, Path destPath) throws IOException {
        MeteorAddonsAddon.LOG.info("Downloading file from: {}", url);

        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            // Create parent directories if needed
            Files.createDirectories(destPath.getParent());

            // Download to temp file first, then atomic move
            Path tempPath = destPath.resolveSibling(destPath.getFileName() + ".tmp");

            try (InputStream inputStream = body.byteStream()) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Atomic move to final destination
            Files.move(tempPath, destPath, StandardCopyOption.REPLACE_EXISTING);

            MeteorAddonsAddon.LOG.info("Successfully downloaded to: {}", destPath);
        }
    }

    /**
     * Download a file with fallback URLs.
     * Tries each URL in order until one succeeds.
     *
     * @param urls     list of URLs to try (in order)
     * @param destPath destination file path
     * @return the URL that succeeded, or null if all failed
     */
    public static String downloadFileWithFallback(String[] urls, Path destPath) {
        if (urls == null || urls.length == 0) {
            MeteorAddonsAddon.LOG.warn("No URLs provided for download");
            return null;
        }

        for (String url : urls) {
            if (url == null || url.isEmpty()) continue;

            try {
                downloadFile(url, destPath);
                return url;
            } catch (IOException e) {
                MeteorAddonsAddon.LOG.warn("Failed to download from {}: {}", url, e.getMessage());
            }
        }

        MeteorAddonsAddon.LOG.error("All download URLs failed for: {}", destPath);
        return null;
    }
}
