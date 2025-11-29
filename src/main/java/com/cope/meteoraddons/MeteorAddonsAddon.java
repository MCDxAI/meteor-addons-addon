package com.cope.meteoraddons;

import com.cope.meteoraddons.gui.tabs.AddonsTab;
import com.cope.meteoraddons.systems.AddonManager;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meteor Addons Addon - Entry Point
 *
 * Enables browsing, installing, and updating Meteor Client addons directly from within the client.
 * Fetches addon metadata from the meteor-addon-scanner repository.
 *
 * @author Cope
 */
public class MeteorAddonsAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteor Addons");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addons Addon");

        // Initialize AddonManager system (manages addon state, downloads, updates)
        Systems.add(new AddonManager());
        LOG.info("AddonManager system initialized");

        // Register Addons tab in Meteor GUI
        Tabs.add(new AddonsTab());
        LOG.info("Addons tab registered");

        // Start fetching addon metadata on a background thread
        AddonManager.get().init();
        LOG.info("Started fetching addon metadata");

        LOG.info("Meteor Addons Addon initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
        // No custom module categories needed for this addon
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
