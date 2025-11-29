# Meteor Addons

A Meteor Client addon that enables browsing, installing, and updating addons directly from within the client.

## Features

- Browse available Meteor Client addons
- View addon metadata (description, features, stats)
- Install addons with one click
- Update installed addons
- Addon data sourced from [meteor-addon-scanner](https://github.com/cqb13/meteor-addon-scanner)

## Building

```bash
./gradlew build
```

## Development Status

**Current:** Basic project structure created
**Next:** Implement addon fetching and GUI

## Structure

```
src/main/java/com/cope/meteoraddons/
├── MeteorAddonsAddon.java      # Main addon entry point
├── gui/
│   └── tabs/
│       └── AddonsTab.java      # GUI tab for addon browser
├── systems/
│   └── AddonManager.java       # System for managing addon state
├── models/
│   └── AddonMetadata.java      # Data model for addon metadata
└── util/                       # Utility classes (TBD)
```

## License

This project is licensed under the MIT License.
