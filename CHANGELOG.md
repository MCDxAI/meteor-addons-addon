# Changelog

All notable changes to the Meteor Addons addon are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Compare links are at the bottom of this file.

## [Unreleased]

### Changed

- fabric.mod.json now requires meteor-client `>=26.2-0`, which matches Meteor's
  distributed `26.2-N` pre-release versioning.
- README version references updated to Minecraft 26.2.

## [0.4.0] - 2026-08-14

### Migration

- Updated to Minecraft 26.2, Meteor Client 26.2-SNAPSHOT, fabric-loader 0.19.3
- Gradle 9.6.1, Fabric Loom 1.17-SNAPSHOT
- Addon version 0.4.0

### Ported

- Adapted the addon-browser screens (AddonDetail, Browse, Installed, Updates,
  Changelog, AddonsTab) to the Meteor 26.2 APIs
- Icon preload system moved to the 26.2 texture-format API

### Docs

- README, badges, and project-structure tree refreshed for 26.2

## [0.3.0] - 2026-04-29

### Migration

- Updated to Minecraft 26.1.2, fabric-loader 0.19.2
- Java/release target 21 → 25, Gradle 9.4.1, Loom 1.16
- Dropped yarn mappings; meteor and fabric-loader moved to plain `implementation`
- `modInclude` refactored into a Configuration extending `implementation` + `include`
- Added foojay toolchain resolver and Fabric Snapshots maven repo

### CI

- New unified `release.yml` workflow — JDK 25, timestamped dev tags, PR build
  validation
- New `test.yml` workflow for continuous test runs
- Auto-generated release notes on stable releases

### New

- JUnit 5 test suite under `src/test/` covering addon metadata, version
  utilities, update checking, and addon manager logic

## [0.2.1] - 2026-01-07

### Fixed

- Addon metadata handling fixes, including a contribution from cqb13 (#7)

### Changed

- Added `meteor-addon-list.json` for the Meteor addon directory

## [0.2.0] - 2025-12-15

### Changed

- Updated to Minecraft 1.21.11 and Meteor Client 1.21.11
- Version management centralized in the Gradle version catalog

### New

- Automatic in-addon update system with hash verification

## [0.1.0] - 2025-12-07

- Initial release.

[Unreleased]: https://github.com/MCDxAI/meteor-addons-addon/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/MCDxAI/meteor-addons-addon/releases/tag/v0.4.0
[0.3.0]: https://github.com/MCDxAI/meteor-addons-addon/releases/tag/v0.3.0
[0.2.1]: https://github.com/MCDxAI/meteor-addons-addon/releases/tag/v0.2.1
[0.2.0]: https://github.com/MCDxAI/meteor-addons-addon/releases/tag/v0.2.0
[0.1.0]: https://github.com/MCDxAI/meteor-addons-addon/releases/tag/v0.1.0
