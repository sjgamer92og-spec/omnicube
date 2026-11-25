# OmniCube roadmap

## Released — v0.2.7 (Minecraft 1.21.11)

- Fixes and polish for existing features, no new modules: Streamer Mode coordinate switch, TNT HUD
  interpolation, vanilla-accurate name tags, scrollable Chat Search with match counts, Inventory
  Tools sort state and refresh, Tool Swap keybind conflict reporting and correct-tool preference,
  offhand Low Durability warnings, and a two-sample RAM Warning

## Released — v0.2.6 (Minecraft 1.21.11)

- Full bug-audit pass with no new features: config-screen tooltips and overlapping footers, waypoint
  compass direction, duplicate death waypoints, Streamer Mode redaction, chat-log timestamps and
  unredacted server lines, chat-log writer restart, performance particle/adaptive-toggle/restore
  bugs, preset particle inflation, Visual Toggles option leaks, cleanup on failed or blocked
  modules, thread-safe entity culling, Auto Sprint keybind, TNT fuse interpolation, Fullbright log
  flooding, duplicate waypoint/macro names, Default profile HUD reset and stale profile selection
- Redesigned loading screen: animated wireframe cube, glow, grid backdrop, glowing wordmark,
  segmented progress bar with shimmer and rotating tips; an animation toggle and Low-end value

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

## Next — v0.2.8 candidates

Still queued from the 0.2.4 plan: a resource-pack quick toggle and a better server list, plus a
hand-swing visual toggle once a safe client-side hook exists in 1.21.11 (cancelling the first-person
hand render breaks rendering, so it stays out until then).

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
