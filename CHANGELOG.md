# Changelog

## 0.2.2 — license change (Minecraft 1.21.11)

No new features.

- OmniCube is now released under the **PolyForm Shield License 1.0.0** instead of MIT. The mod stays
  source-available: the code may be used, modified and shared, but not used to provide a product that
  competes with OmniCube. `LICENSE` carries the full text plus the line
  `Required Notice: Copyright (c) 2026 sjgamer92og (OmniCube)`.
- `fabric.mod.json` now declares `PolyForm-Shield-1.0.0`; README, CREDITS and this changelog describe the
  new terms. Third-party components keep their own licenses (see `CREDITS.md`).
- **The Performance module now has a hardware-aware backend.** It probes the machine once
  (`HardwareProbe`), classifies it into a `HardwareTier`, applies a matching `QualityPreset` and
  then steps quality with a `QualityPolicy` state machine that uses hardware-aware minimum and
  comfort frame rates instead of one fixed target. New setting: **Hardware tiers and presets**
  (on by default, config screen). Only vanilla video options are changed, every original value is
  restored on disable, toggle-off and quit, and any failure restores the settings and falls back to
  the built-in backend. The backend appears in the diagnostics feature table and in the
  performance section of the log, and it is disabled automatically when Sodium, Sodium Extra,
  VulkanMod, Embeddium, Rubidium, Dynamic FPS, OptiFine or Lithium is present.

## 0.2.1 — bug-fix round (Minecraft 1.21.11)

No new features. Everything below fixes behaviour that was broken or misleading in 0.2.0.

### Missing features / blank screens

- **Startup no longer aborts silently.** Every registration step (modules, HUD elements, keybinds,
  commands, tooltip/chat/world/HUD callbacks, join/leave handlers) now runs in isolation. In 0.2.0 a
  single failure — most easily the HUD overlay attach or the command registration — threw out of
  `onInitializeClient`, so the HUD was never attached **and** the client-tick handler that opens all
  screens was never registered. The HUD attach also has a second registration path now.
- **The config list can no longer be wiped by one bad row.** The feature list enabled a scissor
  region and drew rows without a `try/finally`; one row throwing left the scissor enabled, which
  clipped the whole list and every widget drawn afterwards. Rows are now individually guarded and the
  scissor is always released.
- **Compatibility no longer greys out features another mod does not provide.** AppleSkin and Jade were
  blocking the armor/durability HUD, Inspecio was blocking it too, Entity Culling was blocking the
  Performance module, and Iris/Lithium/FerriteCore/ImmediatelyFast/ModernFix/Indium were blocking
  Performance even though they do not manage the vanilla options OmniCube changes. Chat Heads was
  blocking Better Chat although it only adds chat heads.
- **FPS Safety is off by default** (it was on with a 30 FPS floor). It also no longer judges the first
  ten seconds of a world, and its automatic disables are no longer written to the config, so a slow
  world load cannot permanently remove features.
- **HUD elements can explain why they are hidden**: master switch off, blocked by another mod,
  disabled by the user, disabled by FPS Safety/low-end preset, or failed.
- Fixed the default state being re-saved as "automatic", which omitted every untouched module from the
  first config file.

### Diagnostics (new, required by this round)

- `logs/omnicube-diagnostics.log` is written on every launch, with the previous run kept as
  `logs/omnicube-diagnostics-prev.log`. It contains environment, the full loaded-mod list, a feature
  status table, collisions/conflicts, a mixin report, exceptions with full stack traces and repeat
  counts, config load/migration results, FPS samples, callback timings and runtime events.
- All file writes happen on a background daemon thread with a bounded queue; nothing is written on the
  render thread. The log is flushed on disconnect and on client stop/crash.
- Feature states are written immediately when they change, not only at shutdown.
- **Diagnostics tab** in the config screen: feature status table plus **Open log folder** and
  **Copy report** buttons.
- **`/omnicube diag`** prints the summary and the log path.
- Streamer Mode also redacts the diagnostics log (server address and IP patterns).
- A startup self-check verifies every expected module/HUD id, every keybind, every screen, the HUD and
  crosshair hooks and all six mixins (by looking for the merged handler method on the target class).
  Failures are logged as `FAILED` with a reason and shown once in chat:
  `OmniCube: X features failed to load. See logs/omnicube-diagnostics.log`.

### Other bug fixes

- Every `catch` block now reports to the diagnostics log; nothing is swallowed silently any more. A
  HUD element or module that throws is logged with its full stack trace every time and disabled on its
  own, without taking the others down.
- HUD element and module failures are attributed to the feature that failed instead of one global
  "first error only" flag.
- Config saves no longer drop entries for compatibility-blocked or automatically disabled features, so
  their previous user state is restored when the cause disappears.
- Keybind conflict lines now name the owning mod (from the keybind category namespace) as well as the
  key and action.
- Footer buttons on the config screen no longer overlap the Done button on narrow GUIs; the
  Diagnostics tab has its own action row.
- Chat timestamps, chat collapse, tooltip hook, world label and crosshair failures now log and disable
  only the affected feature, with a specific reason.
- Version bumped to `0.2.1`.

## 0.2.0 — expanded release

- Streamer Mode, crosshair customizer, chat search and spam collapse, auto-reconnect, conservative
  entity culling, compatibility registry with auto-disable, Low-end preset, FPS safety, opt-in
  `/omnicube perf` profiler.
- HUD fixes, normalized layout persistence, config migration from an earlier config file, free look, container
  tooltip preview, client-side name tags, chat timestamps/copy/search, world markers, inventory tools,
  waypoints and macros.

## 0.1.0 — first release

- Initial OmniCube client toolkit for Minecraft 1.21.11.
