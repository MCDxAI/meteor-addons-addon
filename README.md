<div align="center">

# Meteor Addons

A Meteor Client addon that enables browsing, installing, and updating addons directly from within the client.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green?style=for-the-badge)](https://github.com/mcdxai/meteor-addons)
[![Fabric](https://img.shields.io/badge/Fabric-0.17.3-blue?style=for-the-badge)](https://fabricmc.net/)
[![Meteor Client](https://img.shields.io/badge/Meteor_Client-1.21.10-blueviolet?style=for-the-badge)](https://meteorclient.com/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)](https://adoptium.net/)

<br />

Meteor Addons enables browsing, installing, and updating addons directly from within the Meteor Client.
Addon data is sourced from meteor-addon-scanner to ensure a wide range of available extensions.

</div>

## Features

| Feature | Description |
| :--- | :--- |
| **Addon Browser** | Browse available Meteor Client addons directly in-game |
| **Metadata Viewer** | View descriptions, features, and stats for each addon |
| **One-Click Install** | Install new addons instantly without leaving the game |
| **Update Manager** | Keep your installed addons up to date with a single click |
| **Addon Scanner** | Integration with meteor-addon-scanner for a comprehensive list |

## Building

To build the project locally:

```bash
./gradlew build
```

## Structure

```
src/main/java/com/cope/meteoraddons/
├── MeteorAddonsAddon.java      # Main addon entry point
├── addons/
│   ├── Addon.java              # Abstract addon class
│   ├── InstalledAddon.java     # Represents a locally installed addon
│   └── OnlineAddon.java        # Represents an addon available online
├── gui/
│   ├── screens/                # Custom screens
│   ├── tabs/
│   │   └── AddonsTab.java      # GUI tab for addon browser
│   └── widgets/                # Custom widgets
├── models/
│   └── AddonMetadata.java      # Data model for addon metadata
├── systems/
│   └── AddonManager.java       # System for managing addon state
└── util/
    ├── AddonIconTexture.java   # Helper for loading addon icons
    ├── HttpClient.java         # HTTP client wrapper
    ├── IconCache.java          # Caching system for icons
    └── VersionUtil.java        # Utility for version comparison
```

## Acknowledgements

Based on work from [addon-menu](https://github.com/crosby-moe/addon-menu) by crosby-moe.

## License

This project is licensed under the [CC0-1.0 license](LICENSE).
