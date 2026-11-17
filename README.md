# OmniCube

![OmniCube icon](src/main/resources/assets/omnicube/icon.png)

**OmniCube** is an all-in-one, client-side Fabric toolkit for **Minecraft 1.21.11**.

**Repository:** [sjgamer92og-spec/omnicube](https://github.com/sjgamer92og-spec/omnicube)


It brings a configurable HUD, useful quality-of-life modules, waypoints, macros, chat logging, better tooltips and an adaptive performance module together in one clean, toggleable mod. Every feature can be switched off, and every setting is adjustable in-game — no config-file editing required (although `config/omnicube.json` is human-readable if you prefer).

The name reflects the goal: one compact cube of client-side utilities covering many parts of the game. It is not affiliated with, endorsed by, or derived from Cactus Mod.

## Feature highlights

### HUD (draggable, toggleable)

Open the **HUD Editor** (`H` by default, or `/omnicube hud`) to drag elements anywhere or right-click them to show/hide. Positions are stored as screen fractions, so they survive resolution and GUI-scale changes.

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

### Modules

| Category | Feature | What it does |
| --- | --- | --- |
| Render | Fullbright | Raises gamma with a configurable brightness and restores your exact original value when disabled or on quit |
| Render | Zoom | Hold a key to narrow FOV, with an adjustable zoom factor |
| Player | Auto Sprint | Sprints while you hold forward (skips sneaking, item use and riding) |
| Player | Fly Speed | Sets creative flight speed (server permissions still apply) |
| Chat | Chat Log | Saves chat and system messages to `logs/omnicube-chat.log` with timestamps |
| Misc | Better Tooltips | Container/shulker contents, durability, food values and optional item IDs |
| World | Waypoints | Saved positions, death markers, HUD compass, active-waypoint tracking |
| Misc | Macros | Bind a chat/slash command to a physical key, with a manager screen |
| Misc | Coordinates Copy | Copy exact position and dimension to the clipboard |
| HUD | HUD Overlay | Master switch, element background, layout editor |
| Performance | Performance | Adaptive particles, render distance, clouds and entity distance based on FPS, with full restoration |
| HUD | 20 individual HUD elements | Each is its own toggle in the config screen |

### Commands

All commands are client-side and start with `/omnicube`:

| Command | Purpose |
| --- | --- |
| `/omnicube config` | Open the config screen |
| `/omnicube hud` | Open the HUD editor |
| `/omnicube list` | List enabled features |
| `/omnicube toggle <feature>` | Toggle a feature by id |
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

All of these can be changed in **Options → Controls → Key Binds → OmniCube**.

| Key | Action |
| --- | --- |
| Right Shift | Open the OmniCube config screen |
| C (hold) | Zoom |
| H | HUD editor |
| B | Add a waypoint at your position |
| F6 | Copy coordinates |

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader 0.18+** for Minecraft **1.21.11**.
3. Put **Fabric API 0.141.6+1.21.11** (or newer for 1.21.11) in your `mods` folder.
4. Put `omnicube-0.1.0+1.21.11.jar` from [`releases/`](releases/) into the same `mods` folder.
5. Launch the Fabric 1.21.11 profile.

OmniCube is **client-side only**. Do not install it on a server; it is not required there and will not load.

## Building from source

```bash
./gradlew build
```

- Gradle **9.8.2** and **JDK 25** are needed to run the build (Fabric Loom 1.19 requires a Java 25 JVM).
- The mod itself compiles and runs on **Java 21**, which is what Minecraft 1.21.11 uses.
- The remapped mod jar is copied to `releases/` automatically.

Project layout:

```
src/main/java/dev/omnicube/          common entrypoint (no client imports)
src/main/resources/               fabric.mod.json, icon, language file
src/client/java/dev/omnicube/client/
  compat/                         all Minecraft-version-specific calls
  config/                         safe atomic JSON config
  feature/  module/  setting/     feature + module + setting framework
  hud/                            HUD elements and layout persistence
  gui/                            config, settings, HUD editor and macro screens
  command/  keybind/  macro/      client commands, keybinds, macros
  optimization/                   performance backend SPI + built-in backend
```

When porting to 1.21–26.3, keep version-specific changes inside `client/compat` first. The rest of the feature code intentionally avoids renderer internals.

## Compatibility

- **No mixins.** OmniCube v0.1.0 hooks only into Fabric's public client APIs (HUD, tooltips, chat, ticks, commands, keybinds). It does not transform Minecraft classes, so it can't conflict with another mod's injection points.
- Per-feature callbacks are individually wrapped; a failure disables that one feature instead of crashing the client.
- Malformed config files are backed up and replaced with defaults instead of preventing startup.
- The performance module only changes vanilla video options, so it is safe alongside Sodium/Iris and similar renderers.
- Some modules (Fly Speed, Auto Sprint) send ordinary client-side state that a server may reject or consider against its rules. Use them responsibly and follow each server's rules.

## What is not in v0.1.0 (roadmap)

OmniCube is an independent reimplementation of the "all-in-one toolkit" idea, not a copy of Cactus Mod (which is **All Rights Reserved**; no code or assets were taken from it). The following Cactus-style features are planned but not implemented yet:

- Free look / detached third-person camera
- Client-side name tags and skin display
- Custom chat rendering (in-chat timestamps, emoji, condensation) — chat is currently logged with timestamps instead
- Shulker/map previews rendered as item grids (currently contents are listed as tooltip text)
- World-space TNT timers, fluid markers and BUD-switch highlighting
- Auto repeater, auto top, auto craft
- Inventory tabs and anti-force-pack
- Account switcher, local server hosting, content packs and userscripts
- NBT editor and armor-stand editor
- Discord RPC, capes and cosmetics

Those require deeper renderer, auth or server-interaction hooks; they will be ported carefully once the 1.21.11 base is proven.



```java
```

where the backend implements `dev.omnicube.client.optimization.OptimizationBackend`. See `CREDITS.md` for the attribution plan once that code is supplied.

## License and credits

OmniCube is released under the **MIT License**. See [LICENSE](LICENSE) and [CREDITS.md](CREDITS.md).
