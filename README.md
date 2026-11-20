# OmniCube

![OmniCube icon](src/main/resources/assets/omnicube/icon.png)

**OmniCube** is an all-in-one, client-side Fabric toolkit for **Minecraft 1.21.11**. It combines a draggable HUD, quality-of-life tools, chat features, a custom crosshair and performance options in one mod, and every feature has its own toggle.

## Features

### HUD
Drag and drop every element in the HUD editor (`H`).

FPS and frame time, coordinates, chunk, biome and dimension, facing, ping, speed, nearby entities, server address, memory, session time, clock, stopwatch, TNT fuse, waypoint compass, potion effects, armor durability, held item durability, experience, keystrokes and CPS.

### Modules

| Category | Feature | Description |
| --- | --- | --- |
| Render | Fullbright | Adjustable brightness, restored on disable |
| Render | Zoom | Hold a key to zoom, adjustable factor |
| Render | Free Look | Look around without turning your player |
| Render | Crosshair | Cross, dot, square or circle, with color and size |
| Render | Entity Culling | Skips mobs hidden behind blocks (off by default) |
| Player | Auto Sprint | Sprints while moving forward |
| Player | Fly Speed | Sets creative flight speed |
| Chat | Better Chat | Timestamps, copy last message, chat search |
| Chat | Chat Spam Collapse | Repeated messages become `hi (2)`, `hi (3)` |
| Chat | Chat Log | Saves chat to `logs/omnicube-chat.log` |
| Misc | Better Tooltips | Container contents, durability, food values, item IDs |
| Misc | Streamer Mode | Hides coordinates and server address in HUD and chat |
| Misc | Auto Reconnect | Rejoins your last server after a disconnect |
| Misc | Inventory Tools | Searchable inventory with quick-move |
| Misc | Macros | Bind commands to keys |
| World | Waypoints | Saved positions, death markers, compass and beacon |
| World | World Markers | TNT fuse labels and waypoint beacon |
| Performance | Performance | Adaptive particles, render distance, clouds and entity distance |
| Performance | FPS Safety | Turns off heavy features if FPS stays too low |

### Built for low-end devices
- Heavy features are **off by default**.
- A one-click **Low-end preset** turns off heavy features.
- The built-in profiler (`/omnicube perf`) shows how much time each feature takes.

## Compatibility

OmniCube is built to work next to other mods.

- If another mod already provides a feature, OmniCube turns its own version off and greys it out. Trying to enable it shows: *"This feature is already provided by `<ModName>`. Remove that mod first to enable it in OmniCube."*
- Performance options switch off automatically when Sodium, Sodium Extra, VulkanMod, Embeddium, Rubidium, Dynamic FPS or OptiFine is installed.
- No hard dependencies on any other mod.
- Mixins are optional, use `@Inject` only and never `@Overwrite`.
- Every feature is isolated, so one failing feature disables itself and nothing else.
- See the **Compat** screen or `/omnicube compat` for the full list.

## Diagnostics

Each launch writes `logs/omnicube-diagnostics.log`, with the previous run saved as `omnicube-diagnostics-prev.log`. It lists the environment, loaded mods, the status of every feature with the reason it is on or off, mod conflicts, mixin results, exceptions, config results and performance data.

You can also view it in the config screen's **Diagnostics** tab or with `/omnicube diag`. If something isn't working, include this file when you report an issue.

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader 0.18+** for Minecraft **1.21.11**.
3. Put **Fabric API 0.141.6+1.21.11** (or newer) in your `mods` folder.
4. Put the OmniCube jar from [`releases/`](releases/) in the same folder.
5. Launch the Fabric 1.21.11 profile.

OmniCube is **client-side only**. Don't install it on a server.

## Controls

All keys can be changed in **Options > Controls > Key Binds > OmniCube**.

| Key | Action |
| --- | --- |
| Right Shift | Open config |
| H | HUD editor |
| V (hold) | Zoom |
| Right Alt (hold) | Free look |
| B | Add waypoint |
| F6 | Copy coordinates |
| F7 | Copy last chat message |
| F8 | Inventory tools |
| F9 | Chat search |

## Commands

| Command | Description |
| --- | --- |
| `/omnicube config` | Open config |
| `/omnicube hud` | Open HUD editor |
| `/omnicube list` | List enabled features |
| `/omnicube toggle <feature>` | Toggle a feature |
| `/omnicube compat` | Show compatibility report |
| `/omnicube diag` | Show diagnostics summary |
| `/omnicube perf` | Show feature timings |
| `/omnicube lowend` | Apply the Low-end preset |
| `/omnicube reload` | Reload the config |
| `/omnicube waypoint add/list/remove/set/clear` | Manage waypoints |
| `/omnicube macro add/list/remove/run` | Manage macros |
| `/omnicube timer start/stop/reset` | Control the stopwatch |

## Building from source

```bash
./gradlew clean build
```

Requires **JDK 25** to run Gradle (Fabric Loom 1.19). The mod itself targets **Java 21**. The jar is copied to `releases/`.

## Roadmap

Planned for later versions:
- Server-side inventory sorting
- More world markers (fluids, BUD switches)
- Auto repeater, auto top, auto craft
- NBT and armor-stand editors
- Support for more Minecraft versions (1.21 to 26.3)

## License

OmniCube is **source-available** under the [PolyForm Shield License 1.0.0](LICENSE).
You may use and modify the code, but you may not use it to make a product that competes with OmniCube.

Third-party code is credited in [CREDITS.md](CREDITS.md). OmniCube is not affiliated with Cactus Mod, and no Cactus code or assets are used
