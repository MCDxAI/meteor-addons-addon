# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Meteor Client addon** that provides an in-game GUI for browsing, installing, and updating other Meteor Client addons. It fetches addon metadata from [meteor-addon-scanner](https://github.com/cqb13/meteor-addon-scanner) and allows users to manage addons without leaving Minecraft.

**Critical Context**: This is NOT a standalone Minecraft mod - it's an addon for Meteor Client, a utility mod for Minecraft. The addon extends Meteor's GUI and systems.

## Build Commands

```bash
# Build the addon (output: build/libs/meteor-addons-*.jar)
./gradlew build

# Clean build artifacts
./gradlew clean

# Clean + build
./gradlew clean build
```

## Architecture

### Threading Model (CRITICAL)

**NEVER block the render thread.** All network operations and heavy processing MUST run on background threads:

- Use `MeteorExecutor.execute(() -> { ... })` for all HTTP requests and file I/O
- Use `mc.execute(() -> { ... })` to schedule GUI updates back on the render thread
- See `AddonManager.fetchAddonMetadata()` for the pattern

**Icon Download Pattern**: CacheManager uses a 4-worker thread pool with `CountDownLatch` for parallel downloads. This is the correct pattern for bulk network operations.

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

### Texture Rendering

For custom textures (like addon icons):

1. Extend `meteordevelopment.meteorclient.renderer.Texture` (NOT Minecraft's texture classes)
2. Use `NativeImage.read()` to load images (handles any size)
3. Resize to target dimensions using `NativeImage.resizeSubRectTo()`
4. Convert to RGBA byte array via `makePixelArray()` and manual color conversion (ABGR → RGBA)
5. Upload with `upload(byte[])`

See `AddonIconTexture` for the complete implementation. Icons are standardized to 48x48 and cached locally.

### Data Flow

1. **Startup**: `AddonManager.init()` triggers background fetch of addons.json
2. **Filtering**: Streams filter by MC version + verified status, deduplicate by name
3. **Icon Caching**: CacheManager downloads icons in parallel to `meteor-client/meteor-addons/icons/`
4. **GUI**: AddonsTab shows cards in 5-column grid, fixed 180px width
5. **Details**: AddonDetailScreen modal shows full metadata + download button
6. **Download**: `AddonManager.downloadAddon()` saves JAR to parent `mods/` folder

### Key Classes

- **MeteorAddonsAddon**: Entry point, registers systems and tabs
- **AddonManager**: System that manages addon state, fetching, filtering, downloading
- **CacheManager**: Icon download/cache management with multi-threading
- **AddonMetadata**: JSON model with helper methods for priority fields (custom.* over regular)
- **AddonsTab**: Main GUI tab with card grid
- **WAddonCard**: Individual addon card widget (icon + title + button)
- **AddonDetailScreen**: Modal overlay with full addon info
- **AddonIconTexture**: Texture implementation for resized addon icons
- **HttpClient**: OkHttp wrapper with fallback URL support
- **VersionUtil**: Minecraft version detection via `SharedConstants.getGameVersion().id()`

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
- **Fix**: Always resize images to match declared texture dimensions (e.g., 48x48)

### Icons not loading
- **Check**: Cache directory exists at `meteor-client/meteor-addons/icons/`
- **Check**: `CacheManager.init()` called before icon access
- **Check**: Network operations on background thread

### Duplicate addons in list
- **Fix**: Deduplication via `Collectors.toMap()` by addon name (already implemented)

## URL Opening

Use `Util.getOperatingSystem().open(url)` for cross-platform URL opening in default browser. This is Minecraft's utility, not Java's Desktop API.

## Build Output

Final JAR: `build/libs/meteor-addons-0.1.0.jar` (~1.1 MB with bundled dependencies)

Install by placing in `.minecraft/mods/` alongside Meteor Client and Fabric Loader.
