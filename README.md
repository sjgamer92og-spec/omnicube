# OmniCube

![OmniCube icon](src/main/resources/assets/omnicube/icon.png)

**OmniCube** is an all-in-one, client-side Fabric toolkit for **Minecraft 1.21.11**.

**Repository:** [sjgamer92og-spec/omnicube](https://github.com/sjgamer92og-spec/omnicube)

**Current release:** `0.2.0+1.21.11`


It brings a configurable HUD, useful quality-of-life modules, waypoints, macros, chat tools, better tooltips, free look, a custom crosshair, streamer privacy, auto-reconnect, conservative entity culling and inventory tools together in one clean, toggleable mod. Every feature can be switched off, and every setting is adjustable in-game — no config-file editing required (although `config/omnicube.json` is human-readable if you prefer).

**v0.2.0 (expanded)** adds streamer mode, the crosshair customizer, chat search and spam collapse, auto-reconnect, conservative entity culling, compatibility auto-disable/UI/reporting, the Low-end preset, FPS safety and an opt-in `/omnicube perf` profiler. It also fixes the audit issues listed in the release notes below.

The name reflects the goal: one compact cube of client-side utilities covering many parts of the game. It is not affiliated with, endorsed by, or derived from Cactus Mod.

## Feature highlights

### HUD (draggable, toggleable)

Open the **HUD Editor** (`H` by default, or `/omnicube hud`) to drag elements anywhere or right-click them to show/hide. Positions are stored as screen fractions, so they survive resolution and GUI-scale changes. HUD elements are clamped to the screen, and the editor/config screens reflow on small GUI sizes.

- FPS and frame time
- Coordinates and chunk coordinates
- Biome and dimension
- Facing direction and yaw
- Server ping
- Movement speed
- Nearby entity count
- Server address
- Java memory usage
- Session playtime
- Real clock and in-game time
- Stopwatch timer
- Nearest primed TNT fuse
- Waypoint compass (direction + distance)
- Active potion effects and durations
- Armor durability
- Experience level and progress
- Held-item durability
- WASD/space/shift keystrokes
- Left/right CPS

**Streamer Mode** locally redacts coordinates, server addresses and IPv4 addresses in the HUD and chat. It is a best-effort local privacy feature, not a security boundary: screenshots, other mods and the game itself can still reveal information.

### Modules

| Category | Feature | What it does |
| --- | --- | --- |
| Render | Fullbright | Raises gamma with a configurable brightness and restores your exact original value when disabled or on quit |
| Render | Zoom | Hold a key to narrow FOV, with an adjustable zoom factor |
| Render | Free Look | Hold **Right Alt** to look around without turning your player; camera rotation is restored when released |
| Render | Crosshair | Shape (Cross/Dot/Square/Circle), color and size; uses Fabric's HUD registry, not renderer internals |
| Render | Entity Culling | Skips mobs whose centers are hidden behind solid blocks. Conservative, capped at 24 raycasts per tick and cached for 5 ticks; off by default |
| Player | Auto Sprint | Sprints while you hold forward (skips sneaking, item use and riding) |
| Player | Fly Speed | Sets creative flight speed and restores your previous speed when disabled |
| Chat | Chat Log | Saves chat and system messages to `logs/omnicube-chat.log` with timestamps |
| Chat | Better Chat | Adds `[HH:mm]` timestamps to chat; **F7** copies the last received message; **F9** opens Chat Search |
| Chat | Chat Spam Collapse | Edits the last line to `hi (2)`, `hi (3)`, ... for consecutive duplicates from the same sender. A different message resets the counter. Off by default |
| Misc | Better Tooltips | Shulker/container contents as a 9-column item grid, plus durability, food values and optional item IDs. The grid is only requested for a hovered stack |
| Misc | Streamer Mode | Locally redacts coordinates and server/IP addresses in the HUD and chat |
| Misc | Auto Reconnect | Retries the last multiplayer server after an unexpected disconnect, with a configurable delay. Manual disconnects are left alone |
| Misc | Inventory Tools | **F8** opens a searchable inventory grid with selection and quick-move |
| Misc | Macros | Bind a chat/slash command to a physical key, with a manager screen |
| Misc | Coordinates Copy | Copy exact position and dimension to the clipboard |
| World | Waypoints | Saved positions, death markers, HUD compass, active-waypoint tracking and an optional world-space beacon |
| World | World Markers | Floating TNT fuse labels and a beacon/label at the active waypoint |
| HUD | HUD Overlay | Master switch, element background, layout editor |
| Performance | Performance | Adaptive particles, render distance, clouds and entity distance based on FPS, with full restoration. Automatically disabled when another optimization mod owns those settings |
| Performance | FPS Safety | If FPS stays below the configurable floor for five seconds, disables optional heavy features and says so |
| Compatibility | Compatibility tab | Detected overlapping mods, auto-disabled features, reasons and keybind warnings. Also `/omnicube compat` |

### Chat spam collapse

Vanilla has no public API for replacing the last chat line, so this feature needs one small **HEAD-only `@Inject`** in `ChatComponent.addMessage(Component, MessageSignature, GuiMessageTag)`:

1. `ChatCollapseModule` compares the complete rendered line (sender + text, with our timestamp prefix ignored).
2. The first line is added normally.
3. A consecutive duplicate cancels the vanilla add and replaces the newest stored `GuiMessage` with `hi (2)`; the next becomes `hi (3)`.
4. Any different message resets the counter. Different players saying “hi” produce different complete lines and are never merged.
5. The mixin never uses `@Overwrite`; it is isolated in `omnicube.chat.mixins.json`, has `require = 0`, runs at priority 1500, and disables this one feature/logs once if it throws.

### Inventory tools

The **Inventory Tools** screen (`F8`) searches item names, shows your 36 main-inventory/hotbar slots as a 9-column grid, and quick-moves the selected stack using the vanilla shift-click packet. **Sort** is a view-only alphabetical sort: it does not rearrange server-side inventory slots, because doing that safely with arbitrary container sizes and server anticheat rules would require a much larger click-simulation layer. This limitation is deliberate and documented rather than shipping an unsafe inventory rewriter.

### Commands

All commands are client-side and start with `/omnicube`:

| Command | Purpose |
| --- | --- |
| `/omnicube config` | Open the config screen |
| `/omnicube hud` | Open the HUD editor |
| `/omnicube list` | List enabled features |
| `/omnicube toggle <feature>` | Toggle a feature by id (blocked features explain why) |
| `/omnicube compat` | Print detected mods, auto-disabled features and reasons |
| `/omnicube perf` | Enable and print the opt-in client callback profiler |
| `/omnicube chatsearch` | Open chat history search |
| `/omnicube lowend` | Apply the Low-end preset |
| `/omnicube reload` | Reload `config/omnicube.json` |
| `/omnicube coords` | Copy your coordinates |
| `/omnicube nbt` | Print the components of your held item |
| `/omnicube screenshots` | Open the screenshots folder |
| `/omnicube waypoint add <name>` | Save the current position |
| `/omnicube waypoint list / remove / set / clear` | Manage waypoints |
| `/omnicube macro add <name> <keycode> <command>` | Create a macro (the Macros screen is easier) |
| `/omnicube macro list / remove / run` | Manage macros |
| `/omnicube timer start / stop / reset` | Control the stopwatch |

### Default keybinds

All of these can be changed in **Options → Controls → Key Binds → OmniCube**. The Compatibility tab warns if an OmniCube action shares a key with vanilla or another OmniCube action.

| Key | Action |
| --- | --- |
| Right Shift | Open the OmniCube config screen |
| V (hold) | Zoom |
| Right Alt (hold) | Free look |
| H | HUD editor |
| B | Add a waypoint at your position |
| F6 | Copy coordinates |
| F7 | Copy the last chat message |
| F8 | Open inventory tools |
| F9 | Search chat history |

## Compatibility

### Overlapping-mod handling

OmniCube checks `FabricLoader.isModLoaded()` at startup. The complete table lives in **one file**: [`CompatRegistry.java`](src/client/java/dev/omnicube/client/compat/CompatRegistry.java). It is deliberately a list of IDs, not hard class references.

| Mod | OmniCube feature(s) disabled |
| --- | --- |
| Essential, Raised, BetterHUD | FPS, Coordinates, Server, Ping HUD |
| Jade, AppleSkin, Inspecio | Armor/Durability HUD |
| Inspecio, Shulker Box Tooltip | Better Tooltips |
| Logical Zoom, Ok Zoomer, Zoomify, Just Zoom | Zoom |
| Gamma Utils, Fullbright | Fullbright |
| Freelook, Perspective Mod Redux | Free Look |
| Chat Heads, Chat Patches, Chat Plus | Better Chat, Chat Spam Collapse |
| Auto Reconnect | Auto Reconnect |
| Entity Culling | Entity Culling, Performance |
| Sodium, Lithium, FerriteCore, ImmediatelyFast, Iris, Indium, ModernFix, Dynamic FPS, Embeddium, Rubidium | Performance |

When a feature is provided elsewhere:

- It is disabled automatically before the first frame and shown **greyed out with “N/A”** in the config screen.
- Enabling it from the config screen, HUD editor or command shows exactly: `This feature is already provided by <ModName>. Remove that mod first to enable it in OmniCube.`
- Both versions never run together. The previous enabled state is preserved while the mod is present, so removing that mod can restore it.
- The startup log lists every detected overlapping mod, every disabled feature and the reason. The **Compat** button on the config screen (and `/omnicube compat`) shows the same report.

HUD and world rendering use Fabric's HUD/world render events rather than vanilla renderer internals, so Sodium/Iris do not remove the HUD, crosshair or world markers.

### Mixins and safety

There are six optional client hooks in three mixin configs: camera/mouse free look, tooltip image injection, vanilla name-tag suppression, chat-line collapse and entity culling. Each one:

- uses `@Inject` only (never `@Overwrite`),
- has an explicit `require = 0` and `defaultRequire = 0`,
- runs at priority 1500 so other mods' transformations apply first,
- is isolated in its own optional config,
- wraps its body in `try/catch`, logs once and disables only that feature for the session after a failure.

The accessor mixin for the private chat refresh method is in the chat config as well. No optional mod class is loaded directly; integrations are only loader-ID checks. There are no hard dependencies on Sodium, Iris, Jade or any other mod.

Malformed/empty/missing config files are backed up and replaced with defaults instead of preventing startup. Per-feature callbacks are individually wrapped so one broken feature cannot crash the client.

Some modules (Fly Speed, Auto Sprint) send ordinary client-side state that a server may reject or consider against its rules. Use them responsibly and follow each server's rules.

## Performance and low-end use

Every module and HUD element has a toggle. Heavy optional features (free look, name tags, world markers, entity culling, inventory tools) are off by default, as are all new feature modules except FPS Safety and Better Chat.

- The **Low-end preset** button on the Compatibility screen (or `/omnicube lowend`) disables heavy optional features and HUD elements in one click, and enables the Performance module only when no optimization mod owns those settings.
- **FPS Safety** measures once per second; if FPS stays below its configurable floor (30 by default) for five seconds, it disables optional heavy features and shows a chat message. It is on by default and can be turned off in the config.
- Entity culling caches results and is capped at 24 raycasts per tick, 64 blocks by default, with a 512-entry cache cap. World markers are capped at 24. No feature scans every block, and no file/network work happens in the render path.
- `/omnicube perf` enables a lightweight profiler for module ticks, HUD elements, chat handling, tooltips, world labels, crosshair, entity culling and free look. It reports average client callback time per feature (not GPU time). It is opt-in so the normal game path stays allocation-free.

The profiler was added because a real frame-time number cannot be produced in the build environment used for this release. Run `/omnicube perf` in-game for your own hardware; report values as client callback time, not a claim of FPS improvement.

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader 0.18+** for Minecraft **1.21.11**.
3. Put **Fabric API 0.141.6+1.21.11** (or newer for 1.21.11) in your `mods` folder.
4. Put `omnicube-0.2.0+1.21.11.jar` from [`releases/`](releases/) into the same `mods` folder.
5. Launch the Fabric 1.21.11 profile.

OmniCube is **client-side only**. Do not install it on a server; it is not required there and will not load.

## Building from source

```bash
./gradlew clean build
```

- Gradle **9.8.2** and **JDK 25** are needed to run the build (Fabric Loom 1.19 requires a Java 25 JVM).
- The mod itself compiles and runs on **Java 21**, which is what Minecraft 1.21.11 uses.
- The remapped mod jar is produced in `build/libs/` and copied to `releases/` by the `remapJar` task.

Project layout:

```
src/main/java/dev/omnicube/          common entrypoint (no client imports)
src/main/resources/               fabric.mod.json, icon, language file
src/client/java/dev/omnicube/client/
  compat/                         loader-ID compatibility table and client shims
  config/                         safe atomic JSON config + legacy migration
  feature/  module/  setting/     feature + module + setting framework
  hud/                            HUD elements and layout persistence
  gui/                            config, settings, HUD editor, macros, chat search, compatibility
  command/  keybind/  macro/      client commands, keybinds, macros, conflict checks
  render/  tooltip/                world-space labels/beacons and container tooltip data
  mixin/                          optional client hooks (see Compatibility)
  optimization/                   performance backend SPI + built-in backend
  perf/                           opt-in callback profiler
```

## Audit fixes in this round

- Chat: removed the double-logging path when timestamps cancel and re-add a message; logs are now written once. Chat history has a 200-message cap and the search screen has responsive footer buttons.
- GUI: settings now scroll instead of overlapping the Done button on small screens; HUD editor, Compatibility and Inventory Tools footer buttons reflow; unavailable rows are visibly greyed and explain themselves.
- Lifecycle/state: chat collapse and auto-reconnect reset on world join/leave; Fly Speed restores the previous flight speed; FPS Safety resets per world; blocked HUD elements cannot be re-enabled by resetting the layout.
- Mixins: every hook has priority 1500, `require = 0`, a try/catch and a one-time failure path; all failures disable only the affected feature.
- Rendering: crosshair uses the Fabric HUD registry; HUD/world markers stay on Fabric events under Sodium/Iris.
- Keybinds: conflict detection is shown in the Compatibility report and as a warning on the config screen.

## Testing status

- `./gradlew clean build` is the release check and must pass with no errors or warnings. The build is compiled and remapped locally; no test sources exist in this repository.
- Real in-game visual, multiplayer and third-party-mod tests cannot run in this headless cloud workspace (no Minecraft display/client). The Fabric-API-only, Sodium+Lithium+Iris+ModMenu and HUD/tooltip mod combinations are therefore **not runtime-tested here**; test them in a normal client before publishing a binary to other users.
- `./gradlew clean build` success verifies compilation, mixin remapping and packaging only. It does not verify rendering or gameplay.

## What is not in v0.2.0 (roadmap)

OmniCube is an independent reimplementation of the "all-in-one toolkit" idea, not a copy of Cactus Mod (which is **All Rights Reserved**; no code or assets were taken from it). The following remain planned but are not implemented yet:

- True server-side inventory sorting (v0.2.0 ships a view-only sort; see the inventory note above)
- Fluid markers, BUD-switch highlighting and more world-space overlays
- Auto repeater, auto top, auto craft
- Inventory tabs and anti-force-pack
- Account switcher, local server hosting, content packs and userscripts
- NBT editor and armor-stand editor
- Discord RPC, capes and cosmetics

Sound control, Thumbnail Studio/pose/dummy-player features, minimap, replay, shaders and cosmetics are intentionally **not** included. Those require deeper renderer, auth or server-interaction hooks; they will be added carefully once the 1.21.11 base is proven. Cactus Mod is All Rights Reserved and is never used as a source.



```java
```

where the backend implements `dev.omnicube.client.optimization.OptimizationBackend`. See `CREDITS.md` for the attribution plan once that code is supplied.

## License and credits

OmniCube is released under the **MIT License**. See [LICENSE](LICENSE) and [CREDITS.md](CREDITS.md).
