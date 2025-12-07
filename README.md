<div align="center">

# Meteor Addons

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric-0.17.3-3d5dff?style=flat)
![Meteor Client](https://img.shields.io/badge/Meteor_Client-Latest-8a11b6?style=flat)
![Java](https://img.shields.io/badge/Java-21-e28655?style=flat)

A Meteor Client addon that enables browsing, installing, and updating addons directly from within the client.

## Building

To build the project locally:

</div>

```bash
./gradlew build
```

<div align="center">

## Structure

</div>

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

<div align="center">

## Acknowledgements

Based on work from [addon-menu](https://github.com/crosby-moe/addon-menu) by crosby-moe.

## License

This project is licensed under the [CC0-1.0 license](LICENSE).

</div>
