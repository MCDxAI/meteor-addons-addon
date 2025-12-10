package com.cope.meteoraddons.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Simple progress bar widget for showing download progress.
 */
public class WProgressBar extends WWidget {
    private static final Color BACKGROUND_COLOR = new Color(50, 50, 50, 200);
    private static final Color FILL_COLOR = new Color(106, 106, 219, 255);
    private static final Color TEXT_COLOR = new Color(255, 255, 255, 255);

    private double progress = 0.0; // 0.0 to 1.0
    private String label = "";
    private final double barHeight;
    private final double barWidth;

    public WProgressBar(double width, double height) {
        this.barWidth = width;
        this.barHeight = height;
    }

    public WProgressBar() {
        this(200, 16);
    }

    @Override
    protected void onCalculateSize() {
        width = barWidth;
        height = barHeight;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        // Background
        renderer.quad(x, y, width, height, BACKGROUND_COLOR);

        // Fill (progress)
        if (progress > 0) {
            double fillWidth = width * Math.min(progress, 1.0);
            renderer.quad(x, y, fillWidth, height, FILL_COLOR);
        }

        // Label text (centered)
        if (!label.isEmpty() && theme != null) {
            double textWidth = theme.textWidth(label);
            double textHeight = theme.textHeight();
            double textX = x + (width - textWidth) / 2;
            double textY = y + (height - textHeight) / 2;
            renderer.text(label, textX, textY, TEXT_COLOR, false);
        }
    }

    /**
     * Set progress value (0.0 to 1.0).
     */
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Get current progress value.
     */
    public double getProgress() {
        return progress;
    }

    /**
     * Set label text displayed on the progress bar.
     */
    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    /**
     * Set progress as percentage (0 to 100).
     */
    public void setPercentage(int percentage) {
        setProgress(percentage / 100.0);
        setLabel(percentage + "%");
    }

    /**
     * Set progress from bytes downloaded / total bytes.
     */
    public void setBytes(long downloaded, long total) {
        if (total > 0) {
            setProgress((double) downloaded / total);
            setLabel(formatBytes(downloaded) + " / " + formatBytes(total));
        } else {
            setProgress(0);
            setLabel("Downloading...");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
