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

## Architecture

### Threading Model (CRITICAL)

**NEVER block the render thread.** All network operations and heavy processing MUST run on background threads:

- Use `MeteorExecutor.execute(() -> { ... })` for all HTTP requests and file I/O
- Use `mc.execute(() -> { ... })` to schedule GUI updates back on the render thread
- See `AddonManager.fetchAddonMetadata()` for the pattern

**Icon Cache Pattern**: `IconCache` uses async texture loading - always returns immediately with a default texture, then loads the real texture in the background and triggers a UI refresh callback.

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

See `AddonIconTexture` for the complete implementation. Icons are cached at multiple sizes (64x64 for grid view, 128x128 for list view).

### Data Flow

1. **Startup**: `AddonManager.init()` triggers background fetch of addons.json
2. **Filtering**: Streams filter by MC version + verified status, deduplicate by name
3. **Icon Caching**: IconCache loads textures asynchronously with default fallbacks
4. **GUI**: AddonsTab provides navigation to Browse/Installed screens
5. **Details**: AddonDetailScreen modal shows full metadata + download button
6. **Download**: `AddonManager.downloadAddon()` saves JAR to parent `mods/` folder

### Key Classes

- **MeteorAddonsAddon**: Entry point, registers systems and tabs
- **AddonManager**: System that manages addon state, fetching, filtering, downloading
- **IconCache**: Async icon loading with callback-based UI refresh
- **AddonMetadata**: JSON model with helper methods for priority fields (custom.* over regular)
- **AddonsTab**: Main GUI tab with navigation to Browse/Installed screens
- **BrowseAddonsScreen**: Main screen with grid/list view of online addons
- **InstalledAddonsScreen**: Shows locally installed Meteor addons
- **WAddonCard**: Individual addon card widget (icon + title + button)
- **WAddonListItem**: List view item for addons
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
- **Fix**: Always resize images to match declared texture dimensions (e.g., 64x64)

### Icons not loading
- **Check**: IconCache callback is set for UI refresh (`IconCache.setOnTexturesLoadedCallback()`)
- **Check**: Network operations on background thread
- **Check**: Texture creation on render thread via `mc.execute()`

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
- **Texture rendering**: See current `AddonIconTexture.java` implementation

## Build Output

Final JAR: `build/libs/meteor-addons-0.1.0.jar` (~1.1 MB with bundled dependencies)

Install by placing in `.minecraft/mods/` alongside Meteor Client and Fabric Loader.
