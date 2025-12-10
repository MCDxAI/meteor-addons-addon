package com.cope.meteoraddons.gui.screens;

import com.cope.meteoraddons.models.UpdateInfo;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/**
 * Screen for displaying an addon's changelog/release notes.
 * Renders markdown-style text with basic formatting and clickable links.
 */
public class ChangelogScreen extends WindowScreen {
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s<>\"\\)]+)");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");

    private final UpdateInfo update;
    private final Screen parent;

    public ChangelogScreen(GuiTheme theme, UpdateInfo update, Screen parent) {
        super(theme, update.getAddonName() + " - Changelog");
        this.update = update;
        this.parent = parent;
    }

    @Override
    public void initWidgets() {
        // Header
        WHorizontalList header = add(theme.horizontalList()).expandX().widget();
        header.add(theme.label(update.getAddonName(), true));
        header.add(theme.label(" v" + update.getNewVersion()).color(theme.textSecondaryColor()));

        add(theme.label(update.getVersionChangeDisplay()).color(theme.textSecondaryColor()));
        add(theme.horizontalSeparator()).expandX().padVertical(4);

        // Changelog content
        String changelog = update.getChangelog();
        if (changelog == null || changelog.trim().isEmpty()) {
            add(theme.label("No changelog provided for this release.").color(theme.textSecondaryColor()));
        } else {
            renderChangelog(changelog);
        }

        add(theme.horizontalSeparator()).expandX().padVertical(4);

        // Buttons
        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();

        // View on GitHub button
        if (update.getDownloadUrl() != null) {
            WButton githubBtn = buttons.add(theme.button("View on GitHub")).widget();
            githubBtn.action = () -> {
                // Convert download URL to release page URL
                String releaseUrl = update.getDownloadUrl()
                    .replaceFirst("/releases/download/[^/]+/.*", "/releases/latest");
                Util.getOperatingSystem().open(releaseUrl);
            };
        }

        buttons.add(theme.horizontalSeparator()).expandCellX();

        WButton backBtn = buttons.add(theme.button("Back")).widget();
        backBtn.action = () -> mc.setScreen(parent);
    }

    private void renderChangelog(String changelog) {
        // Split by lines and render
        String[] lines = changelog.split("\n");
        double maxWidth = getWindowWidth() * 0.6;

        WVerticalList content = add(theme.verticalList()).expandX().widget();

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                // Empty line - add small spacing
                content.add(theme.label("")).padVertical(2);
                continue;
            }

            // Handle markdown headers
            if (line.startsWith("### ")) {
                content.add(theme.label(line.substring(4), true)).padTop(8);
                continue;
            }
            if (line.startsWith("## ")) {
                content.add(theme.label(line.substring(3), true)).padTop(8);
                continue;
            }
            if (line.startsWith("# ")) {
                content.add(theme.label(line.substring(2), true)).padTop(8);
                continue;
            }

            // Handle bullet points
            if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "  " + line; // Indent bullet points
            }

            // Check for markdown links [text](url)
            Matcher linkMatcher = MARKDOWN_LINK_PATTERN.matcher(line);
            if (linkMatcher.find()) {
                // Has markdown links - render with clickable parts
                renderLineWithLinks(content, line, maxWidth);
                continue;
            }

            // Check for plain URLs
            Matcher urlMatcher = URL_PATTERN.matcher(line);
            if (urlMatcher.find()) {
                renderLineWithLinks(content, line, maxWidth);
                continue;
            }

            // Plain text
            content.add(theme.label(line, maxWidth).color(theme.textSecondaryColor()));
        }
    }

    private void renderLineWithLinks(WVerticalList content, String line, double maxWidth) {
        // First try markdown links
        Matcher mdMatcher = MARKDOWN_LINK_PATTERN.matcher(line);
        if (mdMatcher.find()) {
            // For simplicity, just extract the URL and make the whole line clickable
            // In a more complex implementation, we'd render parts separately
            String linkText = mdMatcher.group(1);
            String linkUrl = mdMatcher.group(2);

            // Replace markdown link with just the text
            String displayLine = line.replace(mdMatcher.group(), linkText);

            var label = content.add(theme.label(displayLine, maxWidth)).widget();
            label.color(theme.textColor());
            label.action = () -> Util.getOperatingSystem().open(linkUrl);
            return;
        }

        // Plain URLs - make the whole line clickable
        Matcher urlMatcher = URL_PATTERN.matcher(line);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1);

            var label = content.add(theme.label(line, maxWidth)).widget();
            label.color(theme.textColor());
            label.action = () -> Util.getOperatingSystem().open(url);
            return;
        }

        // Fallback to plain text
        content.add(theme.label(line, maxWidth).color(theme.textSecondaryColor()));
    }
}
