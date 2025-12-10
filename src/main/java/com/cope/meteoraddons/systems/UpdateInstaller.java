package com.cope.meteoraddons.systems;

import com.cope.meteoraddons.MeteorAddonsAddon;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Handles installing staged updates by:
 * 1. Writing a script to replace JARs after Minecraft exits
 * 2. Cleanly shutting down Minecraft
 *
 * The script runs after MC exits, deletes old JARs, moves new JARs into place.
 */
public class UpdateInstaller {
    private static final String SCRIPT_NAME_WIN = "meteor-addon-updater.bat";
    private static final String SCRIPT_NAME_UNIX = "meteor-addon-updater.sh";

    /**
     * Install staged updates and exit Minecraft.
     */
    public static void installStagedUpdates(List<UpdateDownloadManager.StagedUpdate> stagedUpdates) {
        if (stagedUpdates.isEmpty()) {
            MeteorAddonsAddon.LOG.warn("No staged updates to install");
            return;
        }

        try {
            // Get mods directory
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");

            // Create update script
            Path scriptPath = createUpdateScript(stagedUpdates, modsDir);

            MeteorAddonsAddon.LOG.info("Update script created at: {}", scriptPath);
            MeteorAddonsAddon.LOG.info("Exiting Minecraft to apply {} updates...", stagedUpdates.size());

            // Launch the script
            launchUpdateScript(scriptPath);

            // Give script time to start
            Thread.sleep(500);

            // Exit Minecraft
            mc.scheduleStop();

        } catch (Exception e) {
            MeteorAddonsAddon.LOG.error("Failed to install updates", e);
        }
    }

    private static Path createUpdateScript(List<UpdateDownloadManager.StagedUpdate> updates, Path modsDir) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path scriptPath = modsDir.getParent().resolve(isWindows ? SCRIPT_NAME_WIN : SCRIPT_NAME_UNIX);

        List<String> lines = new ArrayList<>();

        if (isWindows) {
            createWindowsScript(lines, updates, modsDir);
        } else {
            createUnixScript(lines, updates, modsDir);
        }

        Files.write(scriptPath, lines, StandardCharsets.UTF_8);

        // Make script executable on Unix
        if (!isWindows) {
            scriptPath.toFile().setExecutable(true);
        }

        return scriptPath;
    }

    private static void createWindowsScript(List<String> lines, List<UpdateDownloadManager.StagedUpdate> updates, Path modsDir) {
        lines.add("@echo off");
        lines.add("echo Meteor Addon Updater");
        lines.add("echo Waiting for Minecraft to close...");
        lines.add("");

        // Wait for Minecraft to fully exit (check if javaw is still running with our game)
        lines.add("timeout /t 3 /nobreak >nul");
        lines.add("");

        lines.add("echo Applying updates...");
        lines.add("");

        for (UpdateDownloadManager.StagedUpdate update : updates) {
            Path oldJar = update.updateInfo.getLocalJarPath();
            Path newJar = update.tempFilePath;
            String newFileName = newJar.getFileName().toString();
            Path targetPath = modsDir.resolve(newFileName);

            lines.add("echo Updating " + update.updateInfo.getAddonName() + "...");

            // Delete old JAR
            lines.add("if exist \"" + oldJar.toAbsolutePath() + "\" (");
            lines.add("    del /f /q \"" + oldJar.toAbsolutePath() + "\"");
            lines.add("    if exist \"" + oldJar.toAbsolutePath() + "\" (");
            lines.add("        echo ERROR: Could not delete old JAR: " + oldJar.getFileName());
            lines.add("    ) else (");
            lines.add("        echo Deleted old JAR: " + oldJar.getFileName());
            lines.add("    )");
            lines.add(")");

            // Move new JAR
            lines.add("if exist \"" + newJar.toAbsolutePath() + "\" (");
            lines.add("    move /y \"" + newJar.toAbsolutePath() + "\" \"" + targetPath.toAbsolutePath() + "\"");
            lines.add("    if exist \"" + targetPath.toAbsolutePath() + "\" (");
            lines.add("        echo Installed: " + newFileName);
            lines.add("    ) else (");
            lines.add("        echo ERROR: Failed to move new JAR");
            lines.add("    )");
            lines.add(")");
            lines.add("");
        }

        // Clean up temp directories
        for (UpdateDownloadManager.StagedUpdate update : updates) {
            Path tempDir = update.tempFilePath.getParent();
            lines.add("rmdir /s /q \"" + tempDir.toAbsolutePath() + "\" 2>nul");
        }

        lines.add("");
        lines.add("echo.");
        lines.add("echo Update complete! You can now restart Minecraft.");
        lines.add("echo.");
        lines.add("pause");

        // Self-delete the script
        lines.add("del \"%~f0\"");
    }

    private static void createUnixScript(List<String> lines, List<UpdateDownloadManager.StagedUpdate> updates, Path modsDir) {
        lines.add("#!/bin/bash");
        lines.add("echo 'Meteor Addon Updater'");
        lines.add("echo 'Waiting for Minecraft to close...'");
        lines.add("");

        // Wait for Minecraft to fully exit
        lines.add("sleep 3");
        lines.add("");

        lines.add("echo 'Applying updates...'");
        lines.add("");

        for (UpdateDownloadManager.StagedUpdate update : updates) {
            Path oldJar = update.updateInfo.getLocalJarPath();
            Path newJar = update.tempFilePath;
            String newFileName = newJar.getFileName().toString();
            Path targetPath = modsDir.resolve(newFileName);

            lines.add("echo 'Updating " + update.updateInfo.getAddonName() + "...'");

            // Delete old JAR
            lines.add("if [ -f \"" + oldJar.toAbsolutePath() + "\" ]; then");
            lines.add("    rm -f \"" + oldJar.toAbsolutePath() + "\"");
            lines.add("    echo 'Deleted old JAR: " + oldJar.getFileName() + "'");
            lines.add("fi");

            // Move new JAR
            lines.add("if [ -f \"" + newJar.toAbsolutePath() + "\" ]; then");
            lines.add("    mv -f \"" + newJar.toAbsolutePath() + "\" \"" + targetPath.toAbsolutePath() + "\"");
            lines.add("    echo 'Installed: " + newFileName + "'");
            lines.add("fi");
            lines.add("");
        }

        // Clean up temp directories
        for (UpdateDownloadManager.StagedUpdate update : updates) {
            Path tempDir = update.tempFilePath.getParent();
            lines.add("rm -rf \"" + tempDir.toAbsolutePath() + "\" 2>/dev/null");
        }

        lines.add("");
        lines.add("echo ''");
        lines.add("echo 'Update complete! You can now restart Minecraft.'");
        lines.add("echo ''");
        lines.add("read -p 'Press Enter to continue...'");

        // Self-delete the script
        lines.add("rm -f \"$0\"");
    }

    private static void launchUpdateScript(Path scriptPath) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder pb;
        if (isWindows) {
            // Open in new command prompt window
            pb = new ProcessBuilder("cmd", "/c", "start", "\"Meteor Addon Updater\"", scriptPath.toAbsolutePath().toString());
        } else {
            // Try to open in terminal on Unix/Mac
            String terminal = findTerminal();
            if (terminal != null) {
                pb = new ProcessBuilder(terminal, "-e", scriptPath.toAbsolutePath().toString());
            } else {
                // Fallback: run directly
                pb = new ProcessBuilder("/bin/bash", scriptPath.toAbsolutePath().toString());
            }
        }

        pb.directory(scriptPath.getParent().toFile());
        pb.start();
    }

    private static String findTerminal() {
        // Check for common terminal emulators
        String[] terminals = {
            "gnome-terminal", "konsole", "xfce4-terminal",
            "mate-terminal", "xterm", "kitty", "alacritty"
        };

        for (String term : terminals) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"which", term});
                if (p.waitFor() == 0) {
                    return term;
                }
            } catch (Exception ignored) {
            }
        }

        // macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            return "open";
        }

        return null;
    }
}
