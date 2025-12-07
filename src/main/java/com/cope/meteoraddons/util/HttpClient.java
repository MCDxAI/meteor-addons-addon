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
 * HTTP client for downloading addon metadata and files using OkHttp.
 */
public class HttpClient {
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    /**
     * Functional interface for handling HTTP response bodies.
     */
    @FunctionalInterface
    private interface ResponseHandler<T> {
        T handle(ResponseBody body) throws IOException;
    }

    /**
     * Execute HTTP request and handle response with common validation logic.
     */
    private static <T> T executeRequest(Request request, ResponseHandler<T> handler) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            return handler.handle(body);
        }
    }

    public static String downloadString(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();

        return executeRequest(request, ResponseBody::string);
    }

    public static byte[] downloadBytes(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();

        return executeRequest(request, ResponseBody::bytes);
    }

    public static void downloadFile(String url, Path destPath) throws IOException {
        MeteorAddonsAddon.LOG.info("Downloading file from: {}", url);

        Request request = new Request.Builder()
            .url(url)
            .build();

        executeRequest(request, body -> {
            Files.createDirectories(destPath.getParent());

            Path tempPath = destPath.resolveSibling(destPath.getFileName() + ".tmp");

            try (InputStream inputStream = body.byteStream()) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempPath, destPath, StandardCopyOption.REPLACE_EXISTING);

            MeteorAddonsAddon.LOG.info("Successfully downloaded to: {}", destPath);
            return null; // Void operation
        });
    }

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
