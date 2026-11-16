




## Feature highlights

### HUD (draggable, toggleable)


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
| Misc | Better Tooltips | Container/shulker contents, durability, food values and optional item IDs |
| World | Waypoints | Saved positions, death markers, HUD compass, active-waypoint tracking |
| Misc | Macros | Bind a chat/slash command to a physical key, with a manager screen |
| Misc | Coordinates Copy | Copy exact position and dimension to the clipboard |
| HUD | HUD Overlay | Master switch, element background, layout editor |
| Performance | Performance | Adaptive particles, render distance, clouds and entity distance based on FPS, with full restoration |
| HUD | 20 individual HUD elements | Each is its own toggle in the config screen |

### Commands


| Command | Purpose |
| --- | --- |

### Default keybinds


| Key | Action |
| --- | --- |
| C (hold) | Zoom |
| H | HUD editor |
| B | Add a waypoint at your position |
| F6 | Copy coordinates |

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader 0.18+** for Minecraft **1.21.11**.
3. Put **Fabric API 0.141.6+1.21.11** (or newer for 1.21.11) in your `mods` folder.
5. Launch the Fabric 1.21.11 profile.


## Building from source

```bash
./gradlew build
```

- Gradle **9.8.2** and **JDK 25** are needed to run the build (Fabric Loom 1.19 requires a Java 25 JVM).
- The mod itself compiles and runs on **Java 21**, which is what Minecraft 1.21.11 uses.
- The remapped mod jar is copied to `releases/` automatically.

Project layout:

```
src/main/resources/               fabric.mod.json, icon, language file
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

- Per-feature callbacks are individually wrapped; a failure disables that one feature instead of crashing the client.
- Malformed config files are backed up and replaced with defaults instead of preventing startup.
- The performance module only changes vanilla video options, so it is safe alongside Sodium/Iris and similar renderers.
- Some modules (Fly Speed, Auto Sprint) send ordinary client-side state that a server may reject or consider against its rules. Use them responsibly and follow each server's rules.

## What is not in v0.1.0 (roadmap)


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


## License and credits

