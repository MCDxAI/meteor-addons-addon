<div align="center">

# Meteor Addons

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric-0.18.2-3d5dff?style=flat)
![Meteor Client](https://img.shields.io/badge/Meteor_Client-1.21.11-8a11b6?style=flat)
![Java](https://img.shields.io/badge/Java-21-e28655?style=flat)

**Browse, install, and update Meteor Client addons without leaving Minecraft**

</div>

<div align="center">

## Features

| Capability | Details |
| --- | --- |
| **In-Game Addon Browser** | Browse available Meteor addons directly from within Minecraft through an integrated GUI |
| **One-Click Installation** | Download and install addons with a single click • No manual file management required |
| **Automatic Updates** | Built-in update system with hash verification for addon integrity |
| **Smart Filtering** | Automatically filters addons by Minecraft version compatibility • Shows only verified, working addons |
| **Grid & List Views** | Switch between grid cards and compact list view based on preference |
| **Icon Preloading** | Async icon loading system with GPU texture caching for smooth performance |
| **Detailed Addon Info** | View full metadata including description, author, download count, and last update |

</div>

<div align="center">

## Quick Start

| Step | Instructions |
| --- | --- |
| **Requirements** | • Java 21 or higher<br>• Minecraft 1.21.11<br>• Fabric Loader 0.18.2+<br>• Meteor Client 1.21.11+ |
| **Installation** | 1. Download the latest `.jar` from [releases](https://github.com/MCDxAI/meteor-addons-addon/releases)<br>2. Place in `.minecraft/mods/` alongside Meteor Client<br>3. Launch Minecraft with Fabric profile |
| **Usage** | 1. Open Meteor Client GUI (Right Shift by default)<br>2. Navigate to the **Addons** tab<br>3. Browse online addons or view installed addons<br>4. Click any addon for details and installation |

</div>

<div align="center">

## Development

| Task | Command |
| --- | --- |
| **Build** | `./gradlew build` – Compiles and packages addon to `build/libs/` |
| **Clean Build** | `./gradlew clean build` – Removes old artifacts and rebuilds |
| **Run Tests** | `./gradlew test` – Executes JUnit test suite |
| **Dependencies** | Bundled: OkHttp 4.12.0, Gson 2.11.0 • Provided: Meteor Client, Fabric Loader |

</div>

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
