package com.cope.meteoraddons;

import com.cope.meteoraddons.gui.screens.UpdatesAvailableScreen;
import com.cope.meteoraddons.gui.tabs.AddonsTab;
import com.cope.meteoraddons.systems.AddonManager;
import com.cope.meteoraddons.systems.IconPreloadSystem;
import com.cope.meteoraddons.systems.UpdateChecker;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Entry point for Meteor Addons addon.
 * Enables browsing, installing, and updating Meteor Client addons from within the client.
 */
public class MeteorAddonsAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteor Addons");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addons Addon");

        IconPreloadSystem iconPreloadSystem = new IconPreloadSystem();
        Systems.add(iconPreloadSystem);
        LOG.info("IconPreloadSystem registered");

        // Initialize UpdateChecker
        UpdateChecker updateChecker = new UpdateChecker();
        Systems.add(updateChecker);
        LOG.info("UpdateChecker system registered");

        // Set up update notification callback
        updateChecker.setOnUpdatesFound(updates -> {
            LOG.info("Found {} addon updates available", updates.size());
            // Show the updates screen on the next tick (ensures we're on render thread)
            mc.execute(() -> {
                mc.setScreen(new UpdatesAvailableScreen(GuiThemes.get(), updates));
            });
        });

        // Set up AddonManager with callback to trigger update check
        AddonManager addonManager = new AddonManager();
        addonManager.setOnLoadComplete(() -> {
            LOG.info("Addon metadata loaded, starting update check...");
            updateChecker.checkForUpdates();
        });

        Systems.add(addonManager);
        LOG.info("AddonManager system initialized");

        Tabs.add(new AddonsTab());
        LOG.info("Addons tab registered");

        LOG.info("Meteor Addons Addon initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
    }

    @Override
    public String getPackage() {
        return "com.cope.meteoraddons";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cope", "meteor-addons-addon");
    }
}
