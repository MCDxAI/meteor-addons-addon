package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.gui.widgets.WProgressBar;
import com.cope.meteoraddons.models.UpdateInfo;
import com.cope.meteoraddons.systems.UpdateDownloadManager;
import com.cope.meteoraddons.systems.UpdateInstaller;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Screen showing available addon updates with selection and download capabilities.
 */
public class UpdatesAvailableScreen extends WindowScreen {
    private final List<UpdateInfo> updates;
    private final Map<UpdateInfo, WCheckbox> checkboxes = new LinkedHashMap<>();
    private final UpdateDownloadManager downloadManager = new UpdateDownloadManager();

    private WButton updateSelectedButton;
    private WButton updateAllButton;
    private WProgressBar progressBar;
    private WVerticalList contentList;

    private boolean isDownloading = false;

    public UpdatesAvailableScreen(GuiTheme theme, List<UpdateInfo> updates) {
        super(theme, "Addon Updates Available");
        this.updates = new ArrayList<>(updates);
    }

    @Override
    public void initWidgets() {
        // Header
        add(theme.label("The following addon updates are available:", true)).expandX();
        add(theme.horizontalSeparator()).expandX().padVertical(4);

        // Content list (scrollable)
        contentList = add(theme.verticalList()).expandX().widget();

        // Update list table
        WTable table = contentList.add(theme.table()).expandX().widget();

        // Header row
        table.add(theme.label(""));  // Checkbox column
        table.add(theme.label("Addon")).expandCellX();
        table.add(theme.label("Version"));
        table.add(theme.label(""));  // Changelog button column
        table.row();

        // Update rows
        for (UpdateInfo update : updates) {
            // Checkbox
            WCheckbox checkbox = table.add(theme.checkbox(true)).widget();
            checkboxes.put(update, checkbox);

            // Addon name
            table.add(theme.label(update.getAddonName())).expandCellX();

            // Version change
            table.add(theme.label(update.getVersionChangeDisplay()).color(theme.textSecondaryColor()));

            // Changelog button
            WButton changelogBtn = table.add(theme.button("Changelog")).widget();
            changelogBtn.action = () -> mc.setScreen(new ChangelogScreen(theme, update, this));

            table.row();
        }

        contentList.add(theme.horizontalSeparator()).expandX().padVertical(4);

        // Progress bar (hidden initially)
        progressBar = contentList.add(new WProgressBar(300, 20)).expandX().widget();
        progressBar.setLabel("Ready");
        progressBar.visible = false;

        contentList.add(theme.horizontalSeparator()).expandX().padVertical(4);

        // Action buttons
        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();

        // Select All / None toggle
        WButton selectToggle = buttons.add(theme.button("Select All")).widget();
        selectToggle.action = () -> {
            boolean allSelected = checkboxes.values().stream().allMatch(cb -> cb.checked);
            for (WCheckbox cb : checkboxes.values()) {
                cb.checked = !allSelected;
            }
            selectToggle.set(allSelected ? "Select All" : "Select None");
        };

        buttons.add(theme.horizontalSeparator()).padHorizontal(8);

        // Update Selected button
        updateSelectedButton = buttons.add(theme.button("Update Selected")).expandCellX().widget();
        updateSelectedButton.action = this::downloadSelectedUpdates;

        // Update All button
        updateAllButton = buttons.add(theme.button("Update All")).widget();
        updateAllButton.action = this::downloadAllUpdates;

        // Close button
        WButton closeButton = buttons.add(theme.button("Later")).widget();
        closeButton.action = this::close;
    }

    private List<UpdateInfo> getSelectedUpdates() {
        List<UpdateInfo> selected = new ArrayList<>();
        for (Map.Entry<UpdateInfo, WCheckbox> entry : checkboxes.entrySet()) {
            if (entry.getValue().checked) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private void downloadSelectedUpdates() {
        List<UpdateInfo> selected = getSelectedUpdates();
        if (selected.isEmpty()) {
            return;
        }
        startDownloads(selected);
    }

    private void downloadAllUpdates() {
        startDownloads(new ArrayList<>(updates));
    }

    private void startDownloads(List<UpdateInfo> toDownload) {
        if (isDownloading) return;

        isDownloading = true;
        updateSelectedButton.visible = false;
        updateAllButton.visible = false;
        progressBar.visible = true;
        progressBar.setLabel("Starting downloads...");
        progressBar.setProgress(0);

        final int total = toDownload.size();
        final int[] completed = {0};

        downloadManager.downloadUpdates(
            toDownload,
            // Individual progress
            (update, progress) -> mc.execute(() -> {
                int percent = (int) (progress * 100);
                progressBar.setLabel(update.getAddonName() + ": " + percent + "%");
                // Calculate overall progress
                double overallProgress = (completed[0] + progress) / total;
                progressBar.setProgress(overallProgress);
            }),
            // Overall progress
            (done, totalCount) -> mc.execute(() -> {
                completed[0] = done;
                if (done < totalCount) {
                    progressBar.setProgress((double) done / totalCount);
                }
            }),
            // Completion
            (success, error) -> mc.execute(() -> {
                isDownloading = false;

                if (success) {
                    progressBar.setLabel("Downloads complete!");
                    progressBar.setProgress(1.0);
                    showInstallPrompt();
                } else {
                    progressBar.setLabel("Some downloads failed");
                    updateSelectedButton.visible = true;
                    updateAllButton.visible = true;

                    // Show error details
                    if (error != null) {
                        contentList.add(theme.label("Errors:").color(theme.textSecondaryColor()));
                        contentList.add(theme.label(error).color(theme.textSecondaryColor()));
                    }
                }
            })
        );
    }

    private void showInstallPrompt() {
        // Clear buttons and show install prompt
        clear();

        add(theme.label("Updates Downloaded Successfully!", true)).expandX().centerX();
        add(theme.horizontalSeparator()).expandX().padVertical(8);

        add(theme.label("The following updates are ready to install:")).expandX();

        WTable table = add(theme.table()).expandX().widget();
        for (UpdateDownloadManager.StagedUpdate staged : downloadManager.getStagedUpdates()) {
            table.add(theme.label(staged.updateInfo.getAddonName()));
            table.add(theme.label(staged.updateInfo.getVersionChangeDisplay()).color(theme.textSecondaryColor()));
            table.row();
        }

        add(theme.horizontalSeparator()).expandX().padVertical(8);

        add(theme.label("Minecraft must be restarted to apply updates.")).expandX();
        add(theme.label("Your old addon JARs will be replaced with the new versions.").color(theme.textSecondaryColor())).expandX();

        add(theme.horizontalSeparator()).expandX().padVertical(8);

        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();

        WButton restartNowBtn = buttons.add(theme.button("Restart Now")).expandCellX().widget();
        restartNowBtn.action = () -> {
            // Install updates and exit
            UpdateInstaller.installStagedUpdates(downloadManager.getStagedUpdates());
        };

        WButton laterBtn = buttons.add(theme.button("Later")).widget();
        laterBtn.action = () -> {
            // Clean up and close without installing
            downloadManager.clearStagedUpdates();
            close();
        };
    }
}
