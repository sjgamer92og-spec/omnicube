# OmniCube roadmap

## Released — v0.2.5 (Minecraft 1.21.11)

- Asynchronous, debounced, atomic config saves on a background thread; shutdown flush and logged failures
- Fullbright **Keep shading** option and Sodium/Embeddium/Iris detection with a logged status note
- Config screen version read from mod metadata instead of a hard-coded number
- Chat Filters (keyword/regex, hide/highlight, 50 cap, compile-once)
- RAM Warning (per-second sampling, once-a-minute warnings)
- Visual Toggles (particles, rain/snow, view bobbing, damage tilt) with option restore
- Settings Profiles (Default, Low-end, Streaming, PvP plus saved files; off-render-thread I/O)
- Death Coordinates with a five-entry history
- Low Durability Warning with per-item re-arming
- Item Counter HUD element with a cached recount
- Tool Swap Helper keybind using the crosshair pick

## Released — v0.2.4 (Minecraft 1.21.11)

- TRUE fullbright: the light coordinate for every block and entity vertex is overridden from a
  `LightTexture.pack` `@Inject` hook, so light level 0 areas are lit instead of merely softened
- Three modes — Full, Gamma (soft), Night vision style — with a brightness slider, cached hot-path
  values, no gamma option write and no per-frame allocations
- Fullbright status line in the Diagnostics tab and `/omnicube diag`; a missing hook is logged and
  falls back to a session-only gamma raise
- Bug audit: guarded entity-count scan, null-safe HUD measuring, version-free splash fallback

## Released — v0.2.3 (Minecraft 1.21.11)

- Free Look limited to a configurable arc, kept in first person, with a smooth return and
  sensitivity-aware mouse movement
- Frame-interpolated zoom with adjustable factor and animation time; the FOV option is untouched
- Custom Splash module: OmniCube loading screen with progress bar and key hints
- Fullbright default raised to maximum gamma so night and cave darkness are gone
- Armor HUD names the worn material, colours durability green/amber/red and hides itself when empty;
  the potion panel hides when there are no effects
- TNT Fuse element no longer crashes when TNT is lit (snapshot scan, per-entity guards)
- Self-check reports mixin state correctly (runtime handler renaming)

## Released — v0.2.2 (Minecraft 1.21.11)

- PolyForm Shield License 1.0.0, with the required notice line and the identifier declared in
  `fabric.mod.json`
- Performance module reworked into a hardware-aware backend: one-time hardware tier, matching quality
  preset, then a per-second quality ladder driven by hardware-aware minimum and comfort frame rates
- New **Hardware tiers and presets** setting; safe fallback to the built-in backend on failure
- Adaptive backend reported in the diagnostics feature table and performance section; Lithium added
  to the performance compatibility list
- README restructured with a complete feature list and this roadmap

## Released — v0.2.1 (Minecraft 1.21.11, bug-fix round)

- Diagnostics log (`logs/omnicube-diagnostics.log`), Diagnostics tab and `/omnicube diag`
- Startup self-check for features, keybinds, screens and mixins
- Fixed startup registration aborts, the clipped feature list, over-broad compatibility rules and
  FPS Safety being enabled by default
- Every caught exception is now logged; feature states carry a specific reason

## Released — v0.2.0 (Minecraft 1.21.11)

- HUD fixes and normalized layout persistence
- Config migration from an earlier config file, with missing/empty/corrupt protection
- Free look
- Shulker/container tooltip preview (text + optional item grid)
- Client-side name tags
- Chat timestamps, copy and search
- Chat spam collapse (`hi (2)`, `hi (3)`, ...)
- World-space TNT and waypoint markers
- Inventory search and vanilla quick-move
- Streamer Mode
- Crosshair shape/color/size customizer
- Auto Reconnect
- Conservative entity culling
- Compatibility registry, auto-disable, Compatibility screen and `/omnicube compat`
- Low-end preset, FPS Safety and opt-in `/omnicube perf` profiler

## Next — v0.2.5 candidates (light features, each with a toggle)

- Chat Filters (precompiled keyword/regex, hide or highlight, max 50, bad regex rejected)
- RAM Warning (roughly once per second, default 85%, at most once per minute)
- Animation and particle toggles (block break, rain, smoke, hand swing, screen shake)
- Settings Profiles (Default, Low-end, Streaming, PvP; file I/O off the render thread)
- Death coordinates in chat with a short recent-deaths list
- Low durability warning for held tool and armour (configurable %, throttled)
- Item counter HUD for a chosen item, refreshed only when the inventory changes
- Hotbar/tool swap helper keybind (off by default, no per-tick scanning)

## Earlier v0.2.x candidates

- True server-safe inventory sorting (review click simulation, container sizes and anticheat rules first)
- More world overlays: fluids, BUD switches, spawn-proofing
- Better automated config/migration tests

## Deliberately out of scope

- Sound control
- Thumbnail Studio, pose tools or dummy-player features
- Minimap
- Local server hosting
- Replay system
- Shaders
- Cosmetics or capes

These are not placeholders for half-finished code. They stay out until there is a safe, maintainable Fabric 1.21.11 design and a clear compatibility story.

## Compatibility policy

Every overlapping feature must name the mod that wins in `CompatRegistry`. When another mod is detected:

- OmniCube's copy is disabled before it can run.
- The config row is greyed out and cannot be enabled.
- The user sees the exact reason and is told to remove the other mod first.
- Mods that own video/render settings always win over OmniCube's Performance module.
- No optional mod is a hard dependency; detection is by Fabric loader ID and integrations use optional entrypoints/reflection only.

## Performance policy

- Heavy features default off.
- Cache and cap every scan; no per-block scans and no file/network work on the render path.
- Low-end preset disables optional heavy work in one click.
- FPS Safety trips only after a sustained low-FPS window and tells the user what changed.
- `/omnicube perf` measures client callback time per feature; GPU/frame-time numbers require real hardware and a normal client, which the headless build environment cannot provide.
