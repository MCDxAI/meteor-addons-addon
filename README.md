<div align="center">

# Meteor Addons

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric-0.18.2-3d5dff?style=flat)
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

## Project Structure
  
  </div>
  
  ```
  src/main/java/com/cope/meteoraddons/
  ├── MeteorAddonsAddon.java      # Main addon entry point
  ├── addons/
  │   ├── Addon.java              # Abstract addon class
  │   ├── InstalledAddon.java     # Represents a locally installed addon
  │   └── OnlineAddon.java        # Represents an addon available online
  ├── config/
  │   └── IconSizeConfig.java     # Icon size configuration
  ├── gui/
  │   ├── screens/
  │   │   ├── AddonDetailScreen.java     # Screen showing details of an addon
  │   │   ├── BrowseAddonsScreen.java    # Screen for browsing online addons
  │   │   └── InstalledAddonsScreen.java # Screen for managing installed addons
  │   ├── tabs/
  │   │   └── AddonsTab.java      # GUI tab for addon browser
  │   └── widgets/
  │       ├── WAddonCard.java     # Widget for displaying an addon in a grid
  │       └── WAddonListItem.java # Widget for displaying an addon in a list
  ├── models/
  │   └── AddonMetadata.java      # Data model for addon metadata
  ├── systems/
  │   ├── AddonManager.java       # System for managing addon state
  │   └── IconPreloadSystem.java  # System for async icon loading
  └── util/
      ├── HttpClient.java         # HTTP client wrapper
      ├── IconCache.java          # Caching system for icons
      ├── TimeUtil.java           # Time utility functions
      └── VersionUtil.java        # Utility for version comparison
  ```

<div align="center">

## Acknowledgements

Based on work from [addon-menu](https://github.com/crosby-moe/addon-menu) by crosby-moe.

## License

This project is licensed under the [CC0-1.0 license](LICENSE).

</div>
