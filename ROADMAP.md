# OmniCube roadmap

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

## Next — v0.3.x candidates

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
