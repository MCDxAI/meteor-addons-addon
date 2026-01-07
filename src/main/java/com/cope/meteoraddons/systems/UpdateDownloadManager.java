package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import com.cope.meteoraddons.models.UpdateInfo;
import com.cope.meteoraddons.util.HashUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Manages downloading addon updates with progress tracking.
 * Downloads to temp directory, then stages for installation on restart.
 */
public class UpdateDownloadManager {
    private static final int BUFFER_SIZE = 8192;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final List<StagedUpdate> stagedUpdates = new ArrayList<>();
    private volatile boolean isDownloading = false;
    private volatile UpdateInfo currentDownload = null;
    private volatile long currentBytesDownloaded = 0;
    private volatile long currentTotalBytes = 0;

    /**
     * Progress callback: (bytesDownloaded, totalBytes)
     */
    public interface ProgressCallback {
        void onProgress(long bytesDownloaded, long totalBytes);
    }

    /**
     * Completion callback: (success, errorMessage)
     */
    public interface CompletionCallback {
        void onComplete(boolean success, String errorMessage);
    }

    /**
     * Download a single update with progress tracking.
     */
    public void downloadUpdate(UpdateInfo update, ProgressCallback onProgress, CompletionCallback onComplete) {
        if (isDownloading) {
            onComplete.onComplete(false, "Another download is in progress");
            return;
        }

        isDownloading = true;
        currentDownload = update;
        currentBytesDownloaded = 0;
        currentTotalBytes = 0;

        new Thread(() -> {
            try {
                Path tempFile = downloadWithProgress(update, onProgress);

                // Verify hash
                String downloadedHash = HashUtil.computeSha256(tempFile);
                if (downloadedHash == null || !HashUtil.hashesMatch(downloadedHash, update.getRemoteHash())) {
                    Files.deleteIfExists(tempFile);
                    onComplete.onComplete(false, "Hash verification failed");
                    return;
                }

                // Stage the update
                StagedUpdate staged = new StagedUpdate(update, tempFile);
                stagedUpdates.add(staged);

                MeteorAddonsAddon.LOG.info("Successfully downloaded and staged update for {}", update.getAddonName());
                onComplete.onComplete(true, null);

            } catch (Exception e) {
                MeteorAddonsAddon.LOG.error("Failed to download update for {}", update.getAddonName(), e);
                onComplete.onComplete(false, e.getMessage());
            } finally {
                isDownloading = false;
                currentDownload = null;
            }
        }, "UpdateDownload-" + update.getAddonName()).start();
    }

    /**
     * Download multiple updates sequentially.
     */
    public void downloadUpdates(
            List<UpdateInfo> updates,
            BiConsumer<UpdateInfo, Double> onIndividualProgress,
            BiConsumer<Integer, Integer> onOverallProgress,
            CompletionCallback onComplete) {
        new Thread(() -> {
            int completed = 0;
            List<String> errors = new ArrayList<>();

            for (UpdateInfo update : updates) {
                final int currentIndex = completed;
                onOverallProgress.accept(currentIndex, updates.size());

                boolean[] success = { false };
                String[] error = { null };

                downloadUpdateBlocking(update, (bytes, total) -> {
                    double progress = total > 0 ? (double) bytes / total : 0;
                    onIndividualProgress.accept(update, progress);
                }, (ok, err) -> {
                    success[0] = ok;
                    error[0] = err;
                });

                if (!success[0]) {
                    errors.add(update.getAddonName() + ": " + error[0]);
                }

                completed++;
            }

            onOverallProgress.accept(completed, updates.size());

            if (errors.isEmpty()) {
                onComplete.onComplete(true, null);
            } else {
                onComplete.onComplete(false, String.join("\n", errors));
            }
        }, "UpdateDownloadBatch").start();
    }

    /**
     * Blocking download with progress.
     */
    private void downloadUpdateBlocking(UpdateInfo update, ProgressCallback onProgress, CompletionCallback onComplete) {
        Object lock = new Object();
        boolean[] done = { false };

        downloadUpdate(update, onProgress, (success, error) -> {
            synchronized (lock) {
                done[0] = true;
                onComplete.onComplete(success, error);
                lock.notifyAll();
            }
        });

        synchronized (lock) {
            while (!done[0]) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private Path downloadWithProgress(UpdateInfo update, ProgressCallback onProgress) throws IOException {
        String url = update.getDownloadUrl();
        MeteorAddonsAddon.LOG.info("Downloading {} from {}", update.getAddonName(), url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            currentTotalBytes = body.contentLength();

            // Create temp file
            Path tempDir = Files.createTempDirectory("meteor-addons-update");
            String fileName = extractFileName(url, update.getAddonName());
            Path tempFile = tempDir.resolve(fileName);

            try (InputStream in = body.byteStream();
                    OutputStream out = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                currentBytesDownloaded = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    currentBytesDownloaded += bytesRead;

                    if (onProgress != null) {
                        onProgress.onProgress(currentBytesDownloaded, currentTotalBytes);
                    }
                }
            }

            return tempFile;
        }
    }

    private String extractFileName(String url, String fallbackName) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            String name = url.substring(lastSlash + 1);
            // Remove query params if present
            int query = name.indexOf('?');
            if (query > 0) {
                name = name.substring(0, query);
            }
            if (name.endsWith(".jar")) {
                return name;
            }
        }
        return fallbackName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".jar";
    }

    /**
     * Get list of staged updates ready for installation.
     */
    public List<StagedUpdate> getStagedUpdates() {
        return new ArrayList<>(stagedUpdates);
    }

    /**
     * Check if any updates are staged.
     */
    public boolean hasStagedUpdates() {
        return !stagedUpdates.isEmpty();
    }

    /**
     * Get current download progress info.
     */
    public long getCurrentBytesDownloaded() {
        return currentBytesDownloaded;
    }

    public long getCurrentTotalBytes() {
        return currentTotalBytes;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public UpdateInfo getCurrentDownload() {
        return currentDownload;
    }

    /**
     * Clear all staged updates (cancels pending installation).
     */
    public void clearStagedUpdates() {
        for (StagedUpdate staged : stagedUpdates) {
            try {
                Files.deleteIfExists(staged.tempFilePath);
                Files.deleteIfExists(staged.tempFilePath.getParent());
            } catch (IOException e) {
                MeteorAddonsAddon.LOG.warn("Failed to clean up temp file: {}", staged.tempFilePath);
            }
        }
        stagedUpdates.clear();
    }

    /**
     * Represents a downloaded update ready for installation.
     */
    public static class StagedUpdate {
        public final UpdateInfo updateInfo;
        public final Path tempFilePath;

        StagedUpdate(UpdateInfo updateInfo, Path tempFilePath) {
            this.updateInfo = updateInfo;
            this.tempFilePath = tempFilePath;
        }
    }
}
