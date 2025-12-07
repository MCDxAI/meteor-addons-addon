package com.cope.meteoraddons.gui.widgets;

import com.cope.meteoraddons.addons.Addon;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable vertical list of addons with automatic separators.
 * Used in BrowseAddonsScreen and InstalledAddonsScreen.
 */
public class WAddonList extends WVerticalList {
    private final List<Addon> addons;
    private final Function<Addon, Runnable> onClickProvider;
    private final Function<Addon, Consumer<WButton>> onInstallProvider;

    /**
     * Create a new addon list widget.
     *
     * @param addons The list of addons to display
     * @param onClickProvider Function that provides click handler for each addon (for detail screen)
     * @param onInstallProvider Function that provides install button handler (nullable for installed addons)
     */
    public WAddonList(
        List<Addon> addons,
        Function<Addon, Runnable> onClickProvider,
        Function<Addon, Consumer<WButton>> onInstallProvider
    ) {
        this.addons = addons;
        this.onClickProvider = onClickProvider;
        this.onInstallProvider = onInstallProvider;
    }

    @Override
    public void init() {
        for (int i = 0; i < addons.size(); i++) {
            Addon addon = addons.get(i);

            // Get handlers for this addon
            Runnable onClick = onClickProvider.apply(addon);
            Consumer<WButton> onInstall = onInstallProvider != null ? onInstallProvider.apply(addon) : null;

            // Add the list item
            add(new WAddonListItem(addon, onClick, onInstall)).expandX();

            // Add separator between items (not after last item)
            if (i < addons.size() - 1) {
                add(theme.horizontalSeparator()).expandX();
            }
        }
    }
}
