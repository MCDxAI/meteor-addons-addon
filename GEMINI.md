# GEMINI.md - Project Context & Developer Guide

This file provides comprehensive context and guidelines for Gemini agents working on the **Meteor Addons** project.

## 1. Project Overview

**Meteor Addons** is a specialized addon for the [Meteor Client](https://meteorclient.com/) (a Minecraft utility mod). It functions as an in-game package manager, allowing users to browse, install, and update other Meteor Client addons directly from the client's GUI.

*   **Type:** Java Project (Fabric Mod / Meteor Client Addon)
*   **Language:** Java 21
*   **Build System:** Gradle (Kotlin DSL)
*   **Dependencies:** Meteor Client, Fabric Loader, OkHttp, Gson
*   **Data Source:** Fetches metadata from the [meteor-addon-scanner](https://github.com/cqb13/meteor-addon-scanner) repository.

## 2. Critical Architecture & Patterns

### 2.1 Threading Model (STRICT)
*   **Render Thread:** exclusively for GUI updates and texture manipulation. **NEVER** block this thread.
*   **Background Threads:** All network operations (HTTP requests) and heavy file I/O **MUST** run on background threads.
    *   Use `MeteorExecutor.execute(() -> { ... })` for background tasks.
*   **Synchronization:** To update the GUI from a background thread, schedule the task back to the render thread:
    *   Use `mc.execute(() -> { ... })`.

### 2.2 Systems Architecture
The project uses Meteor Client's `Systems` pattern for persistent, globally accessible singletons.
*   **Registration:** Systems are added in `MeteorAddonsAddon.onInitialize()` using `Systems.add(new MySystem())`.
*   **Access:** `MySystem.get()` (wrapper for `Systems.get(MySystem.class)`).
*   **Persistence:** Override `toTag()` and `fromTag()` to save/load state (NBT).

### 2.3 GUI Widget Lifecycle
Meteor's GUI framework has a strict initialization order. Violating this causes "theme is null" crashes.

*   **Constructor:** Initialize data fields only. **Do NOT** build UI or call `init()`.
*   **init():** Build your UI here. This method is called automatically by the framework *after* the widget is added to the tree and the `theme` is set.
*   **Theme:** Access the `theme` field (provided by parent) inside `init()`. Never accept `GuiTheme` in the constructor.

### 2.4 Icon Management
*   **Centralized Config:** Icon sizes are defined in `com.cope.meteoraddons.config.IconSizeConfig`.
    *   `ADDON_ICON_SIZE`: Default 64x64.
*   **Preloading:** `IconPreloadSystem` handles async downloading of icon bytes.
*   **Texture Creation:** Occurs on the render thread (via `reload()` or `mc.execute()`) using `NativeImage`.
*   **Instant Lookup:** `IconCache` or `IconPreloadSystem.getTexture()` returns the texture immediately (or a default) to prevent rendering lag.

## 3. Project Structure

```
src/main/java/com/cope/meteoraddons/
├── MeteorAddonsAddon.java      # Main entry point. Registers systems/tabs.
├── addons/                     # Data models
│   ├── Addon.java              # Base interface
│   ├── InstalledAddon.java     # Local addon wrapper (ModContainer)
│   └── OnlineAddon.java        # Remote addon wrapper (AddonMetadata)
├── config/
│   └── IconSizeConfig.java     # Centralized dimension constants
├── gui/
│   ├── screens/                # Full-screen UIs (Browse, Installed)
│   ├── tabs/                   # Tab integration (AddonsTab)
│   └── widgets/                # Reusable components (WAddonCard)
├── models/
│   └── AddonMetadata.java      # JSON mapping for scanner data
├── systems/
│   ├── AddonManager.java       # Core logic: fetching, filtering, downloading
│   └── IconPreloadSystem.java  # Async icon download & GPU texture management
└── util/
    ├── HttpClient.java         # OkHttp wrapper
    └── VersionUtil.java        # Minecraft version compatibility helpers
```

## 4. Build & Execution

**Mandatory:** Use the `gradle-mcp-server` tools for all build tasks. Do NOT use `run_shell_command` with `./gradlew` or `gradle`.

*   **List Tasks:** `gradle_list_tasks(projectPath=...)`
*   **Execute Task:** `gradle_execute(projectPath=..., tasks=["build"])`
*   **Quick Build:** `gradle_build(projectPath=...)`
*   **Project Info:** `gradle_project_info(projectPath=...)`
*   **Dependencies:** `gradle_dependencies(projectPath=...)`

## 5. Key Implementation Details

*   **Data Flow:** `AddonManager.init()` -> Background Fetch -> Filter (Version/Verified) -> Deduplicate -> Async Icon Download -> Texture Creation -> GUI Render.
*   **Networking:** Uses `OkHttp` (bundled via `include` in Gradle). Always verify internet access before assuming success.
*   **File Operations:** Addons are downloaded to `MeteorClient.FOLDER.getParent().resolve("mods")`.
*   **Version Filtering:** Checks `custom.supported_versions` array first, then `mc_version`.

## 6. Common Pitfalls to Avoid
1.  **Calling `init()` manually:** Never do this for Widgets.
2.  **Blocking Render Thread:** Will freeze the game. Use `MeteorExecutor`.
3.  **Texture Size Mismatch:** Always resize `NativeImage` to match the texture dimensions defined in `IconSizeConfig` before uploading to prevent memory violations.
4.  **Theme Shadowing:** Do not create a `theme` field in your widget if extending a Meteor widget; use the protected field from the parent.

## 7. Tools & Reference

### 7.1 Code Search (Mandatory)
Use `code-search-mcp` for all codebase exploration and navigation.
1.  **Initialize:** Always ensure the workspace is added via `add_workspace` (if not already detected/listed).
2.  **Search:** Prefer these tools over `grep` or basic file reading:
    *   `search_symbols`: Find classes, methods, and variables.
    *   `search_ast_pattern`: Find code based on structure (e.g., `Systems.add($$$)`).
    *   `search_text`: Fast, regex-based text search (uses ripgrep).
3.  **Investigate:** Use `codebase_investigator` for complex, high-level architectural questions.

### 7.2 References
*   **AI Reference:** The `ai_reference/` directory contains decompiled/source code of Meteor Client and other addons. Index this directory if you need to search it deeply.
*   **Mappings:** The project uses Yarn mappings.
