# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Meteor Client addon** that provides an in-game GUI for browsing, installing, and updating other Meteor Client addons. It fetches addon metadata from [meteor-addon-scanner](https://github.com/cqb13/meteor-addon-scanner) and allows users to manage addons without leaving Minecraft.

**Critical Context**: This is NOT a standalone Minecraft mod - it's an addon for Meteor Client, a utility mod for Minecraft. The addon extends Meteor's GUI and systems.

## Build Commands

**IMPORTANT**: Always use the `gradle-mcp-server` tool for Gradle operations. Never invoke Gradle commands manually in the terminal.

```bash
# Build the addon (output: build/libs/meteor-addons-*.jar)
./gradlew build

# Clean build artifacts
./gradlew clean

# Clean + build
./gradlew clean build
```

## Project Structure

```
src/main/java/com/cope/meteoraddons/
├── MeteorAddonsAddon.java           # Entry point, addon initialization
├── config/
│   └── IconSizeConfig.java          # Centralized icon size constants
├── systems/
│   ├── AddonManager.java            # Core system: addon fetching, filtering, downloads
│   └── IconPreloadSystem.java       # Icon download & GPU texture management
├── addons/
│   ├── Addon.java                   # Base interface for all addons
│   ├── OnlineAddon.java             # Addon from meteor-addon-scanner API
│   └── InstalledAddon.java          # Local addon from mods/ folder
├── models/
│   └── AddonMetadata.java           # JSON model for addon metadata
├── gui/
│   ├── tabs/
│   │   └── AddonsTab.java           # Main tab in Meteor GUI
│   ├── screens/
│   │   ├── BrowseAddonsScreen.java  # Online addons browse screen
│   │   ├── InstalledAddonsScreen.java # Installed addons screen
│   │   └── AddonDetailScreen.java   # Detail modal for individual addon
│   └── widgets/
│       └── WAddonCard.java          # Grid view addon card widget
└── util/
    ├── IconCache.java               # Facade for instant icon lookups
    ├── HttpClient.java              # OkHttp wrapper with fallbacks
    ├── VersionUtil.java             # Minecraft version detection
    └── TimeUtil.java                # Relative time formatting

src/main/resources/
├── fabric.mod.json                  # Fabric mod metadata
└── assets/meteor-addons/
    ├── icon.png                     # Addon icon (shown in mod menu)
    └── installed-icon.png           # Badge overlay for installed addons
```

**Key Patterns:**
- `config/` - Configuration constants (sizes, URLs, etc.)
- `systems/` - Meteor Systems (singleton managers with NBT persistence)
- `gui/` - All GUI code organized by type (tabs, screens, widgets)
- `util/` - Utility classes with static helper methods
- `models/` - Data classes mapping to JSON structures

## Architecture

### Threading Model (CRITICAL)

**NEVER block the render thread.** All network operations and heavy processing MUST run on background threads:

- Use `MeteorExecutor.execute(() -> { ... })` for all HTTP requests and file I/O
- Use `mc.execute(() -> { ... })` to schedule GUI updates back on the render thread
- See `AddonManager.fetchAddonMetadata()` for the pattern

**Icon Cache Pattern**: Icons are preloaded during startup. `IconPreloadSystem` downloads PNG bytes in background threads, then converts them to GPU textures during resource reload (render thread). `IconCache.get(addon)` returns instantly - either the cached texture or a gray placeholder.

### Systems Architecture

Meteor Client uses a **Systems** pattern for persistent, globally-accessible managers:

```java
// In MeteorAddonsAddon.onInitialize():
Systems.add(new AddonManager());

// Anywhere else:
AddonManager manager = AddonManager.get();
```

Systems automatically handle NBT serialization/persistence. Override `toTag()` and `fromTag()` if you need to persist state.

### GUI Widget Lifecycle (CRITICAL)

Meteor's GUI framework has a specific initialization pattern that MUST be followed:

**CORRECT Pattern:**
```java
public class MyWidget extends WVerticalList {
    private final Data data;

    public MyWidget(Data data) {
        this.data = data;
        // Do NOT call init() here!
    }

    @Override
    public void init() {
        // Framework calls this AFTER widget is added to tree with theme set
        add(theme.label("text")).centerX();
    }
}
```

**WRONG Pattern (causes NPE crashes):**
```java
public MyWidget(GuiTheme theme, Data data) {
    this.theme = theme;  // WRONG: shadowing parent field
    init();              // WRONG: theme not set yet in parent
}
```

The `theme` field is set by the framework when the widget is added to the widget tree. Never set it manually or call `init()` from the constructor.

### Icon Sizing (IMPORTANT)

All addon icon sizes are centralized in `IconSizeConfig.java`:

```java
public static final int ADDON_ICON_SIZE = 64;  // Change here to adjust globally
public static final int INSTALLED_INDICATOR_SIZE = 32;
```

**All icons display at 64x64 pixels** across grid view, list view, detail screens, and installed addons. To change icon sizes:
1. Edit `IconSizeConfig.ADDON_ICON_SIZE`
2. Rebuild - all GUI components and texture creation automatically use the new size

### Texture Rendering

Icon textures are managed by `IconPreloadSystem`:

1. **Download Phase**: HTTP downloads cache raw PNG bytes (`iconDataCache`)
2. **Resource Reload**: `reload()` converts cached PNGs to GPU textures on render thread
3. **Texture Creation**: Uses `NativeImage.read()` → resize to `IconSizeConfig.ADDON_ICON_SIZE` → ABGR→RGBA conversion → `Texture.upload()`
4. **Instant Lookup**: `IconCache.get(addon)` returns cached texture or default gray placeholder

See `IconPreloadSystem.createTextureFromNativeImage()` for implementation. Icons are created at a single size (64x64 by default) and used everywhere.

### Data Flow

1. **Startup**: `AddonManager.init()` triggers background fetch of addons.json and icon downloads
2. **Icon Preload**: `IconPreloadSystem` downloads icons to byte cache, converts to textures on resource reload
3. **Filtering**: Streams filter by MC version + verified status, deduplicate by name
4. **GUI**: AddonsTab provides navigation to Browse/Installed screens
5. **Details**: AddonDetailScreen modal shows full metadata + download button
6. **Download**: `AddonManager.downloadAddon()` saves JAR to parent `mods/` folder

### Key Classes

**Configuration:**
- **IconSizeConfig**: Centralized icon size constants (`ADDON_ICON_SIZE`, `INSTALLED_INDICATOR_SIZE`)

**Core Systems:**
- **MeteorAddonsAddon**: Entry point, registers systems and tabs
- **AddonManager**: System that manages addon state, fetching, filtering, downloading
- **IconPreloadSystem**: System for async icon downloading and GPU texture creation
- **IconCache**: Facade for instant icon texture lookups (delegates to IconPreloadSystem)

**Data Models:**
- **AddonMetadata**: JSON model with helper methods for priority fields (custom.* over regular)
- **Addon**: Base interface for online and installed addons

**GUI Screens:**
- **AddonsTab**: Main GUI tab with navigation to Browse/Installed screens
- **BrowseAddonsScreen**: Main screen with grid/list view of online addons
- **InstalledAddonsScreen**: Shows locally installed Meteor addons
- **AddonDetailScreen**: Modal overlay with full addon info

**GUI Widgets:**
- **WAddonCard**: Individual addon card widget for grid view (icon + title + button)

**Utilities:**
- **HttpClient**: OkHttp wrapper with fallback URL support
- **VersionUtil**: Minecraft version detection via `SharedConstants.getGameVersion().id()`
- **TimeUtil**: Relative time formatting for "updated 2 days ago" labels

## Version Filtering

Addons support two version fields:
- `mc_version`: Single version string
- `custom.supported_versions`: Array of versions (prioritized)

`AddonMetadata.supportsCurrentVersion()` handles both, checking `custom.supported_versions` first.

## Dependencies

- **OkHttp 4.12.0**: HTTP client (included in JAR via `include()`)
- **Gson 2.11.0**: JSON parsing (included in JAR)
- **Meteor Client 1.21.10-SNAPSHOT**: Host mod (provided)

The `include()` directive in build.gradle.kts bundles dependencies into the addon JAR since Meteor doesn't provide them.

## Common Issues

### Crash: "theme is null" in WTexture
- **Cause**: Called `init()` from widget constructor or set theme manually
- **Fix**: Override `init()`, never call it manually, never accept theme in constructor

### Crash: Memory access violation in array copy
- **Cause**: Texture dimensions don't match uploaded buffer size
- **Fix**: Always resize images to match declared texture dimensions (e.g., 64x64)

### Icons not loading
- **Check**: Icons downloaded during `AddonManager.fetchAddonMetadata()` background phase
- **Check**: `IconPreloadSystem.reload()` called during resource reload to convert to textures
- **Check**: For installed addons with JAR icons, `IconCache.get()` loads them on-demand from JAR `InputStream`

### Duplicate addons in list
- **Fix**: Deduplication via `Collectors.toMap()` by addon name (already implemented)

## URL Opening

Use `Util.getOperatingSystem().open(url)` for cross-platform URL opening in default browser. This is Minecraft's utility, not Java's Desktop API.

## MCP Server Tools

### Gradle Operations
**Always use `gradle-mcp-server` tool** for all Gradle operations instead of manual terminal commands. Available functions:
- `gradle_build`: Build the project
- `gradle_execute`: Execute specific Gradle tasks
- `gradle_list_tasks`: List all available tasks
- `gradle_dependencies`: View dependency tree

### Minecraft Mappings
**Use `linkie-mcp-server` tool** for all Minecraft obfuscation mapping lookups:
- `search_mappings`: Find mappings by obfuscated/intermediary/named names
- `lookup_class`: Get specific class mapping details
- `lookup_method`: Get method mapping details
- `lookup_field`: Get field mapping details
- `translate_mappings`: Convert between Yarn/Mojang/MCP namespaces
- `get_source_code`: Get decompiled source for a class

### Mixin Development
**Use `mixin-mcp-server` tool proactively** when working with Mixins:
- `load_class`: Load and inspect class structure
- `inspect_method`: Get detailed method info with bytecode
- `find_injection_point`: Locate injection points (HEAD, TAIL, INVOKE, etc.)
- `validate_mixin`: Comprehensive mixin validation
- `generate_mixin_template`: Generate correct mixin boilerplate
- `show_mapped_code`: View bytecode with injection point markers

### Code Search
**Use `code-search-mcp` tool** for large-scale searches, especially in `ai_reference/`:
- **Prefer AST search first** (highest accuracy, fastest):
  - `search_ast_pattern`: Pattern matching with metavariables (e.g., `function $FUNC($ARG) { $$$ }`)
  - `search_ast_rule`: Complex rules with relational operators
- `search_symbols`: Find classes, methods, functions by name
- `search_text`: Regex/literal text search (fallback when AST won't work)
- `search_files`: Find files by name/pattern

## AI Reference Directory

The `ai_reference/` folder (git-ignored) contains complete source code for:
- **meteor-client**: Core framework, base classes, event system, GUI patterns
- **starscript**: Expression language for dynamic text
- **orbit**: Event system library
- **meteor-rejects**: Feature-rich addon with many examples
- **MeteorPlus**: Advanced addon with mode systems and anticheat bypasses
- **meteor-villager-roller**: Minimal addon example

**Always check `ai_reference/INDEX.md` first** when looking for examples of how to implement features. The index provides detailed explanations of patterns, best practices, and where to find specific implementations.

### Common Reference Patterns

- **Module creation**: See `meteor-client/systems/modules/` and `meteor-rejects/modules/`
- **GUI widgets**: See `meteor-client/gui/widgets/`
- **Command implementation**: See `meteor-rejects/commands/`
- **Event handling**: See `meteor-client/events/` and `orbit/`
- **Settings framework**: See `meteor-client/settings/`
- **Texture rendering**: See `IconPreloadSystem.java` in current codebase
- **Icon sizing**: See `IconSizeConfig.java` for centralized size constants

## Build Output

Final JAR: `build/libs/meteor-addons-0.2.0.jar` (~1.1 MB with bundled dependencies)

Install by placing in `.minecraft/mods/` alongside Meteor Client and Fabric Loader.
