# OmniCube

![OmniCube icon](src/main/resources/assets/omnicube/icon.png)

**OmniCube** is an all-in-one, client-side Fabric toolkit for **Minecraft 1.21.11**.

**Repository:** [sjgamer92og-spec/omnicube](https://github.com/sjgamer92og-spec/omnicube)

**Current release:** `0.2.3+1.21.11` — see [CHANGELOG.md](CHANGELOG.md)

It brings a configurable HUD, useful quality-of-life modules, waypoints, macros, chat tools, better tooltips, free look, a custom crosshair, streamer privacy, auto-reconnect, conservative entity culling and inventory tools together in one clean, toggleable mod. Every feature can be switched off, and every setting is adjustable in-game — no config-file editing required (although `config/omnicube.json` is human-readable if you prefer).

**v0.2.0 (expanded)** adds streamer mode, the crosshair customizer, chat search and spam collapse, auto-reconnect, conservative entity culling, compatibility auto-disable/UI/reporting, the Low-end preset, FPS safety and an opt-in `/omnicube perf` profiler.

**v0.2.1 is a bug-fix round with no new features.** It fixes the reasons features could fail to appear (a single startup failure aborting every later registration, a config list clipped by an unreleased scissor, over-eager compatibility rules and FPS Safety disabling features by default) and adds a diagnostics log, an in-game **Diagnostics** tab and `/omnicube diag`. Details are in the release notes and [CHANGELOG.md](CHANGELOG.md).

**v0.2.2 relicenses OmniCube to the PolyForm Shield License 1.0.0** — still no new features. The mod stays source-available: you may read, use and modify the code, but not ship a product that competes with OmniCube. See [License](#license).

**v0.2.3 is the comfort and polish round.** Free Look is limited to a configurable arc, stays in first person and eases back when released; Zoom animates per frame; the Mojang loading screen is replaced by a branded OmniCube splash; Fullbright fully removes night and cave darkness; the armor HUD shows the material you wear with green/amber/red durability; empty armor and potion panels hide themselves; and the TNT fuse element can no longer crash the game.

The name reflects the goal: one compact cube of client-side utilities covering many parts of the game. It is not affiliated with, endorsed by, or derived from Cactus Mod.

## Features

Every feature is client-side, can be switched off, and needs no server-side mod. Hover any row in the config screen for its exact state and the reason it is off.

### HUD
- **20 draggable elements**: FPS and frame time, coordinates and chunk, biome and dimension, facing and yaw, server ping, movement speed, nearby entities, server address, memory use, session time, clock and in-game time, stopwatch, nearest TNT fuse, waypoint compass, potion effects, armor durability, experience, held-item durability, keystrokes and CPS.
- **Armor, potion and durability panels that show only what exists** — the armor element names each worn piece ("Iron 100%") and colours durability green, amber or red; with no armor, or no active potion, the panel disappears instead of showing "none".
- **HUD editor** with drag-and-drop, right-click show/hide, fraction-based layout that survives resolution and GUI-scale changes, screen clamping and a master switch.

### Visuals and camera
- **Fullbright** — removes night and cave darkness (gamma 16 by default) and restores your exact original value.
- **Zoom** — hold a key to narrow the field of view with a smooth, adjustable animation; your saved FOV is never changed.
- **Free Look** — hold a key to look around within a configurable arc (110° by default) without turning your player; it stays in first person and eases back when released.
- **Custom Splash** — replaces the Mojang studios loading screen with handled rectangles, a progress bar, key hints and the OmniCube wordmark.
- **Crosshair** — shape, colour, outline and size, drawn through Fabric's HUD registry.

### Player
- **Name Tags+** — client-side player tags with health and distance.
- **Auto Sprint** — sprints while you hold forward, unless sneaking or using an item.
- **Fly Speed** — sets creative flight speed and restores the previous value.
- **Coordinates Copy** — copies your exact position and dimension to the clipboard.

### World
- **Waypoints** — save, list, set and remove points, with a compass HUD element and an optional death marker.
- **World Markers** — TNT fuse labels and a beacon beam at the active waypoint.
- **Entity Culling** — skips entities fully hidden behind solid blocks; capped, cached and conservative.

### Chat
- **Better Chat** — timestamps and copy-the-last-message.
- **Chat Spam Collapse** — folds repeated lines into `message (2)`, `message (3)`, …
- **Chat Log** — writes received chat and server messages to `logs/omnicube-chat.log` through a bounded background queue.
- **Chat Search** — searchable in-game history screen.

### Items and inventory
- **Better Tooltips** — shulker and container previews, durability, food values and optional item IDs.
- **Inventory Tools** — search your inventory and quick-move the selected stack with vanilla shift-click.
- **NBT Viewer** — prints the components of the held item.

### Performance
- **Hardware-aware adaptive quality** — one-time hardware tier, matching quality preset, then a per-second quality ladder driven by minimum and comfort frame rates. Vanilla options only, always restored.
- **FPS Safety** — optional guard that sheds heavy features after a sustained drop and tells you what changed.
- **Low-end preset** — one command that switches on the lean configuration.
- **Callback profiler** — `/omnicube perf` for per-feature client callback timings.

### Privacy
- **Streamer Mode** — redacts coordinates, server addresses and IPv4 addresses in the HUD, chat and the diagnostics log.

### Configuration, diagnostics and compatibility
- **Config screen** with Features, HUD, Diagnostics and Compatibility tabs, scrolling lists and per-row explanations.
- **Diagnostics log** — nine-section report in `logs/omnicube-diagnostics.log`, previous run kept, written off the render thread, plus `/omnicube diag` and a Copy-report button.
- **Startup self-check** — verifies every feature, keybind, screen, HUD hook and mixin, logs failures and warns once in chat.
- **Compatibility registry** — detects overlapping mods, disables only the duplicated feature, shows the exact reason and never runs both.
- **Commands and keybinds** — `/omnicube …` for config, HUD, waypoints, macros, timer and tools, with conflict detection against vanilla and other mods.

## Feature details

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
| Performance | FPS Safety | If FPS stays below the configurable floor for five seconds, disables optional heavy features and says so. Off by default; ignored for the first ten seconds of a world |
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
| Inspecio, Shulker Box Tooltip | Better Tooltips |
| Logical Zoom, Ok Zoomer, Zoomify, Just Zoom | Zoom |
| Gamma Utils, Fullbright | Fullbright |
| Freelook, Perspective Mod Redux | Free Look |
| Chat Patches, Chat Plus | Better Chat, Chat Spam Collapse |
| Auto Reconnect | Auto Reconnect |
| Entity Culling | Entity Culling |
| Sodium, Sodium Extra, VulkanMod, Embeddium, Rubidium, Dynamic FPS, OptiFine, Lithium | Performance |

Rules are intentionally narrow: a mod is listed only for the feature it really provides. AppleSkin (food values), Jade (block/entity info), Chat Heads (chat avatars), Iris, FerriteCore, ImmediatelyFast, ModernFix and Indium do **not** provide the OmniCube features they were previously matched with, so they no longer disable anything. Lithium is the one exception: it already owns chunk and simulation ticking, so it does disable the Performance module, with that reason shown.

When a feature is provided elsewhere:

- It is disabled automatically before the first frame and shown **greyed out with “N/A”** in the config screen.
- Enabling it from the config screen, HUD editor or command shows exactly: `This feature is already provided by <ModName>. Remove that mod first to enable it in OmniCube.`
- Both versions never run together. The previous enabled state is preserved while the mod is present, so removing that mod can restore it.
- The startup log lists every detected overlapping mod, every disabled feature and the reason. The **Compat** button on the config screen (and `/omnicube compat`) shows the same report.
- The Performance module is fully disabled when Sodium, Sodium Extra, VulkanMod, Embeddium, Rubidium, Dynamic FPS, OptiFine or Lithium is present, so it never changes video settings another mod is managing.

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

Every module and HUD element has a toggle. Heavy optional features (free look, name tags, world markers, entity culling, inventory tools) are off by default, as are all feature modules except Better Chat and the HUD Overlay master switch.

- The **Low-end preset** button on the Compatibility screen (or `/omnicube lowend`) disables heavy optional features and HUD elements in one click, and enables the Performance module only when no optimization mod owns those settings.
- **FPS Safety** is **off by default**. When enabled, it ignores the first ten seconds of a world and then, if FPS stays below its configurable floor (30 by default) for five seconds, disables optional heavy features with a chat message that states the measured FPS. Its automatic disables are not written to the config, so they are undone on the next launch, and the reason is recorded in the Diagnostics tab and log.
- Entity culling caches results and is capped at 24 raycasts per tick, 64 blocks by default, with a 512-entry cache cap. World markers are capped at 24. No feature scans every block, and no file/network work happens in the render path.
- `/omnicube perf` enables a lightweight profiler for module ticks, HUD elements, chat handling, tooltips, world labels, crosshair, entity culling and free look. It reports average client callback time per feature (not GPU time). It is opt-in so the normal game path stays allocation-free.

The profiler was added because a real frame-time number cannot be produced in the build environment used for this release. Run `/omnicube perf` in-game for your own hardware; report values as client callback time, not a claim of FPS improvement.

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader 0.18+** for Minecraft **1.21.11**.
3. Put **Fabric API 0.141.6+1.21.11** (or newer for 1.21.11) in your `mods` folder.
4. Put `omnicube-0.2.2+1.21.11.jar` from [`releases/`](releases/) into the same `mods` folder.
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
  diag/                           diagnostics log, startup self-check and feature status
  feature/  module/  setting/     feature + module + setting framework
  hud/                            HUD elements and layout persistence
  gui/                            config, settings, HUD editor, macros, chat search, compatibility
  command/  keybind/  macro/      client commands, keybinds, macros, conflict checks
  render/  tooltip/                world-space labels/beacons and container tooltip data
  mixin/                          optional client hooks (see Compatibility)
  optimization/                   performance backend SPI + built-in backend
  perf/                           opt-in callback profiler
```

## Diagnostics

Every launch writes `logs/omnicube-diagnostics.log` and keeps the previous run as
`logs/omnicube-diagnostics-prev.log`. The file has nine fixed sections:

1. **Environment** — OmniCube, Minecraft, Fabric Loader/API and Java versions, OS, JVM heap, CPU
   threads, launcher if it can be detected.
2. **Mods detected** — every loaded mod id, version and name.
3. **Feature status table** — one line per module and HUD element:
   `name | state | reason | time`, where state is `ENABLED`, `DISABLED`, `BLOCKED` or `FAILED` and the
   reason is specific, for example `Disabled by user`, `Blocked: provided by Sodium (sodium)`,
   `Disabled by Low-end preset`, `Disabled by FPS Safety (FPS 14 < 30 for 5s)`,
   `Failed: NullPointerException in hud_tnt.render() (see stack trace)` or
   `Mixin failed to apply: ChatComponent (see MIXIN REPORT)`.
4. **Collisions and conflicts** — every keybind conflict (key, action and owning mod), every
   compatibility disable and every registration that failed.
5. **Mixin report** — each of the six OmniCube mixins and the two HUD hooks, applied or not.
6. **Exceptions** — full stack traces with the feature name, a timestamp and a repeat counter; the
   same exception is logged once and then counted instead of spamming.
7. **Config** — load result, migration result, which file was backed up or reset and why.
8. **Performance** — FPS samples every 30 seconds, callback timings from `/omnicube perf`, and every
   FPS Safety trigger.
9. **Runtime events** — world join/leave, opened screens, feature toggles and lifecycle events, all
   timestamped.

Writing happens on a dedicated daemon thread with a bounded queue; the render thread never touches the
disk. The log is flushed on disconnect and on client stop/crash, and a feature that fails is written
immediately when it fails. Streamer Mode also redacts the log (server address, IPv4 addresses,
coordinates), and no passwords, tokens or session ids are ever logged.

The same table is available in-game in the config screen's **Diagnostics** tab, with **Open log
folder** and **Copy report** buttons, and in chat via **`/omnicube diag`**. A startup self-check proves
that every feature id, keybind, screen, HUD hook and mixin is really present; anything missing is
logged as `FAILED` and announced once in chat as
`OmniCube: X features failed to load. See logs/omnicube-diagnostics.log`.

## Audit fixes in v0.2.0

- Config: missing, empty and corrupt config paths now fail safely, back up damaged files and preserve valid settings from an earlier config file. Atomic saves are unchanged.
- Chat: removed the double-logging path when timestamps cancel and re-add a message; logs are now written once. Chat history has a 200-message cap and the search screen has responsive footer buttons. Chat Log writes through a bounded background queue instead of doing file I/O while receiving a message.
- GUI: settings now scroll instead of overlapping the Done button on small screens; HUD editor, Compatibility and Inventory Tools footer buttons reflow; unavailable rows are visibly greyed and explain themselves. The config screen caches its keybind warning instead of rescanning every frame.
- Lifecycle/state: chat collapse and auto-reconnect reset on world join/leave; Fly Speed restores the previous flight speed; FPS Safety resets per world; blocked HUD elements cannot be re-enabled by resetting the layout.
- Mixins: every hook has priority 1500, `require = 0`, a try/catch and a one-time failure path; all failures disable only the affected feature.
- Rendering: crosshair uses the Fabric HUD registry and keeps every arm visible at the minimum size; container grids are capped at 27 stacks; HUD/world markers stay on Fabric events under Sodium/Iris.
- Keybinds: conflict detection is shown in the Compatibility report and as a cached warning on the config screen; duplicated OmniCube-only warnings are removed.

## Bug fixes in v0.2.1

Root causes found for features that did not appear on screen:

1. **One startup failure aborted every later registration.** `onInitializeClient` chained ~20
   registrations with no isolation. A throw from the HUD overlay attach (or command registration)
   meant the HUD was never attached *and* `ClientTickEvents` was never registered — so no keybind could
   open the config screen either. Every step is now individually guarded and logged, and the HUD hook
   has a fallback registration path.
2. **The feature list could be visually wiped by a single row.** The list region enabled a scissor and
   drew rows without `try/finally`; one throwing row left the scissor enabled, clipping the rest of the
   list and every widget drawn afterwards. Rows are individually guarded and the scissor is always
   released.
3. **Compatibility rules were too broad.** AppleSkin/Jade/Inspecio blocked the armor and durability
   HUD, Entity Culling blocked Performance, and Iris/Lithium/FerriteCore/ImmediatelyFast/ModernFix/
   Indium blocked Performance although they do not manage the options OmniCube changes. Blocked
   features render as grey `N/A` rows and cannot be enabled, which reads as "the feature is gone".
   The table is now limited to mods that really provide the matched feature.
4. **FPS Safety was on by default with a 30 FPS floor.** On ordinary hardware it disabled heavy
   features after five seconds (including during world load) and persisted that to the config. It is
   now off by default, ignores the first ten seconds of a world, and its automatic disables are not
   saved.

Other fixes in this round:

- HUD elements and modules report why they are off (user, compatibility, low-end preset, FPS Safety,
  failure, master switch) in the GUI tooltip, `/omnicube diag` and the log.
- Every `catch` block reports to the diagnostics log with a full stack trace; failures are attributed
  to the feature that failed instead of one global "first error" flag.
- Saving the config no longer drops entries for blocked or automatically disabled features, so their
  previous user state comes back when the cause goes away. Untouched modules are written on first run
  instead of being omitted.
- Keybind conflicts name the owning mod as well as the key and action.
- Config screen footer buttons no longer overlap Done on narrow GUIs; the Diagnostics tab has its own
  action row.

## Testing status

- `./gradlew clean build` is the release check and must pass with no errors or warnings. The build is compiled and remapped locally; no test sources exist in this repository.
- Real in-game visual, multiplayer and third-party-mod tests cannot run in this headless cloud workspace (no Minecraft display/client). The Fabric-API-only, Sodium+Lithium+Iris+ModMenu and HUD/tooltip mod combinations are therefore **not runtime-tested here**; test them in a normal client before publishing a binary to other users. The diagnostics log and self-check compile and are packaged, but their runtime output has not been observed on a real client in this environment.
- `./gradlew clean build` success verifies compilation, mixin remapping and packaging only. It does not verify rendering or gameplay.

## Roadmap

Kept up to date with every release; the same plan lives in [ROADMAP.md](ROADMAP.md).

**Versioning:** every release bumps only the last number (`0.2.2` → `0.2.3`), and the middle number stays fixed for the whole 0.2 series. The finished product ships as `1.0.0` and moves on with `1.0.1`, `1.1.0` and so on.

### Released

| Version | Highlights |
| --- | --- |
| 0.1.0 | First release: configurable HUD, core modules, waypoints, macros, tooltips |
| 0.2.0 | Free look, name tags, chat tools, streamer mode, crosshair, auto-reconnect, entity culling, compatibility registry, low-end preset, FPS Safety |
| 0.2.1 | Bug-fix round: startup isolation, list clipping, compatibility rules, FPS Safety defaults; diagnostics log, in-game Diagnostics tab, `/omnicube diag`, startup self-check |
| 0.2.2 | PolyForm Shield licence; hardware-tier presets and adaptive scaling in the Performance module |
| 0.2.3 | Bounded first-person free look with smooth return; frame-animated zoom; custom OmniCube loading screen; full-brightness fix; armour material names and durability colours; auto-hiding empty HUD panels; TNT fuse crash fix |

### Next — 0.2.x candidates

- **True server-safe inventory sorting** — click simulation, container-size and anti-cheat review first.
- **More world overlays** — fluids, BUD switches, spawn-proofing.
- **Config and migration tests** — automated checks for damaged, partial and legacy config files.
- **Diagnostics polish** — sharper performance attribution and per-feature frame-cost history.

### Out of scope

OmniCube is an independent toolkit, not a copy of Cactus Mod (which is **All Rights Reserved**; no code or assets were taken from it). These stay out until there is a safe, maintainable Fabric 1.21.11 design:

- Sound control
- Thumbnail Studio, pose tools or dummy-player features
- Minimap
- Local server hosting
- Replay system
- Shaders
- Cosmetics or capes
- Account switcher, content packs and userscripts
- Discord RPC

## Performance backend

The Performance module is hardware-aware. It works in two layers, both toggleable in the config screen:

1. **Hardware tiers and presets.** On the first run it classifies the machine (heap, device RAM, CPU threads, renderer family — including GL4ES, ANGLE, VirGL, software and mobile GPUs) and applies a matching preset once: *Minimum*, *Low*, *Balanced* or *Quality*. A preset only ever lowers a value; it never raises render or simulation distance above what you already had. Turning **Hardware tiers and presets** off skips this layer entirely.
2. **Adaptive scaling.** Once per second it compares your frame rate against a minimum (30 FPS on very low/low hardware, 45 on mid, 60 on high) and a comfort level 1.25x above it, then steps a 0.35 → 1.00 quality ladder up or down with hysteresis, cooldowns and a 10-second settle window after a world join. It never changes a value the preset did not already own, and it does nothing while a screen is open or a world is loading.

Only vanilla video options are touched (render distance, simulation distance, particles, clouds, entity distance, biome blend), so nothing fights a renderer mod; every original value is captured first and restored when you disable the module, switch the toggle off, or quit. If the adaptive path ever throws, it restores your settings and hands the session to the conservative built-in backend instead of failing the module. The module disables itself entirely when Sodium, Sodium Extra, VulkanMod, Embeddium, Rubidium, Dynamic FPS, OptiFine or Lithium is installed.

## License

OmniCube is **source-available**, not open source. It is released under the **[PolyForm Shield License 1.0.0](LICENSE)**: anyone may read, use, modify and share the code, but may not use it to provide a product that competes with OmniCube.

`Required Notice: Copyright (c) 2026 sjgamer92og (OmniCube)` — keep this line with any copy of the software, as the license requires.

Third-party components keep their own licenses; see [CREDITS.md](CREDITS.md).
