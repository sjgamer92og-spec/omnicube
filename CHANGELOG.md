# Changelog

## 0.2.6 — bug-fix round and a redesigned loading screen (Minecraft 1.21.11)

No new features. This release is a full audit pass over the client code, fixing behaviour that was
wrong or fragile in 0.2.5.

- **Every feature description was hidden by the config screen.** Hovering any row replaced its
description with the state reason, which is always `Enabled (default)` for a normal feature. The
description is shown again; only a blocked, hidden or failed state replaces it.
- **The config screen's footer buttons overlapped on the Diagnostics tab.** The feature buttons
(HUD, Macros, Compat, Profiles, Reload) and the diagnostics buttons were drawn in the same place;
the feature buttons now hide while Diagnostics is open.
- **The waypoint compass pointed the wrong way.** A positive yaw difference is to the right in
Minecraft, but the HUD labelled it "left" and vice versa.
- **Death waypoints piled up.** One `Death` waypoint was added per death, forever. There is now a
single death marker, moved to the latest death.
- **Streamer Mode redacted diagnostics while it was off.** The redactor ran regardless of the module
switch, so coordinates and addresses disappeared from the log even with Streamer Mode disabled.
Redaction now follows the module.
- **The chat log could store secrets and duplicate timestamps.** Server messages were written to
`logs/omnicube-chat.log` unredacted, and displayed chat lines were logged with the chat timestamp on
top of the log timestamp. Both now log the redacted, undecorated text.
- **Chat logging stayed dead after one failed write.** A write failure disabled the module and left
the writer thread gone; re-enabling the module now restarts the writer and clears the failure flag.
- **Performance recovery forced particles back to `ALL`.** The built-in backend now restores the
player's original particle setting instead of raising a deliberately reduced one.
- **The adaptive backend ignored its own switches.** "Reduce render distance under load" and
"Reduce entity distance under load" were not consulted by the adaptive backend; both are now
honoured by the scaling step.
- **The adaptive backend used stale FPS thresholds and a stale preset.** The hardware preset was
chosen before the GL renderer was known, so a machine that turned out weaker kept the wrong preset
and the policy kept its old minimum/comfort FPS. The tier is now re-evaluated once the renderer is
known, the preset is re-applied when the tier drops, and the decision engine is rebuilt.
- **The fallback backend's video changes were never restored.** After an adaptive failure the fallback
captured and changed options, but disabling the module only restored the adaptive layer. Both layers
are restored now, and the fallback can be retried on the next enable.
- **The Balanced preset raised a reduced particle setting.** Preset level 2 forced `ALL` particles;
it now leaves the player's value alone, matching the "presets never raise a value" rule.
- **Visual Toggles leaked its video changes into `options.txt`.** Leaving the world or quitting
restored Performance but not the particle, view-bob and damage-tilt values, so OmniCube's temporary
values could be saved as the player's own. They are restored on disconnect and shutdown, and the
module captures the restored values again on its next tick.
- **Failed and compatibility-blocked modules never unloaded their state.** `markFailed` and
`blockForCompatibility` now run the same cleanup as a normal disable; a blocked Fullbright, for
example, no longer keeps overriding light coordinates.
- **The entity-culling cache was written from two threads.** Render-thread lookups and client-thread
`clear()` calls used a plain `HashMap`; it is now a `ConcurrentHashMap` with volatile tick counters.
- **Auto Sprint ignored a remapped forward key.** It now follows the vanilla forward keybind instead
of the physical W key.
- **The TNT fuse counter froze between scans.** Markers are sampled twice a second, and the label
showed that stale fuse; the elapsed ticks are now subtracted so the countdown stays continuous.
- **Fullbright could flood the diagnostics log.** A failure in the per-vertex light override logged
for every vertex of every frame; it now logs once and switches the override off.
- **Re-using a waypoint or macro name duplicated it.** Adding an existing name now moves or updates
that entry, so `/omnicube waypoint add`, the quick-waypoint key and the macro screen cannot create
ambiguous duplicates.
- **The Default profile left the HUD layout alone.** It now resets element positions as well as
module settings.
- **Saving a profile read the live config from the background thread.** The module and HUD sections
are now deep-copied on the client thread before the IO job is queued, so a tick or screen saving at
the same time can no longer race the profile snapshot.
- **The Low-end preset skipped its safe values on a running Visual Toggles module.** The safe
particle/view-bob/damage-tilt values were only applied while the module was off; they are now applied
whether it is already enabled or not.
- **A deleted profile could stay selected** in the Profiles screen; the selection now follows the
list.
- **The loading screen is redesigned.** A slowly rotating 3D wireframe cube with a mint glow, a
drifting grid backdrop, a glowing wordmark, a segmented progress bar with a shine sweep and rotating
tips replace the flat gradient-and-bar splash. It is still plain rectangle/text drawing with no
textures, and a new **Animate cube and shimmer** toggle (also applied by the Low-end preset) turns
the animation off.

## 0.2.5 — async saves, fullbright polish and eight light features (Minecraft 1.21.11)

- **Config saves no longer touch the render thread.** The JSON snapshot is built in memory and written
  by one background daemon thread, atomically (temp file + rename), with a 300 ms debounce so dragging
  a slider schedules one write instead of dozens. Repeated writes collapse by generation, the file is
  flushed on shutdown, and every failure is logged and shown in the Diagnostics tab.
- **Fullbright polish.** New **Keep shading** option: Full mode lifts every light coordinate to 11–15
  instead of forcing 15/15, so caves are bright but block faces and particles keep their relative
  shading. The module now detects Sodium, Embeddium and Iris at startup and logs a status note, so a
  bypassed lightmap hook is explained instead of looking like a broken feature; a missing hook still
  falls back to a session-only gamma raise.
- **Bug: the config screen showed a hard-coded `0.2.1`.** It now reads the version from the loaded mod
  metadata and shows `dev` if that is unavailable.
- **Chat Filters** (`chat_filter`, off by default): hide or highlight lines by keyword or regex.
  Precompiled patterns, 50-pattern cap, invalid regex rejected and logged at add time, cached compile
  invalidation only when the saved list changes. Manage with `/omnicube filter add|addregex|remove|list|clear`.
- **RAM Warning** (`ram_warning`): heap sampled about once a second, warning at a configurable
  percentage (85% default), at most once a minute.
- **Visual Toggles** (`visual_toggles`): separate particle, rain/snow, view-bobbing and damage-tilt
  switches. Everything maps to an existing vanilla option, previous values are captured on enable and
  restored on disable, and rain/snow is skipped through a `WeatherEffectRenderer` `@Inject` hook with
  `require = 0`.
- **Settings Profiles** (`settings_profiles`): save, load, delete and rename named profiles plus the
  built-in **Default**, **Low-end**, **Streaming** and **PvP** profiles. Files live in
  `config/omnicube/profiles/`, are read and written off the render thread, and a damaged profile logs
  and falls back to your current settings. Available from the new **Profiles** button and
  `/omnicube profile save|load|delete|rename|list`.
- **Death Coordinates** (`death_coords`): announces where you died and keeps the last five positions
  in the config for `/omnicube deaths`.
- **Low Durability Warning** (`durability_warning`): one warning per item below a configurable
  percentage, re-armed when the item is repaired or replaced.
- **Item Counter HUD** (`hud_item_count`): how many of the held item you carry, recounted only when the
  held item changes or the cache is 500 ms old.
- **Tool Swap Helper** (`tool_swap`): an unbound keybind selects the best hotbar tool for the block you
  look at, using the existing crosshair pick and nine slot comparisons.
- Diagnostics: every new feature publishes a status note to the Diagnostics tab and `/omnicube diag`;
  `SelfCheck` now verifies the new modules, HUD element, keybind, screen and weather mixin. The Low-end
  preset disables the new heavy or noisy extras and switches on RAM Warning and low-end Visual Toggles.

## 0.2.4 — real fullbright and a bug-audit round (Minecraft 1.21.11)

- **Fullbright actually makes the world bright now.** The 0.2.x implementation only raised the
  `gamma` option, which cannot work in 1.21.11: `lightmap.fsh` computes
  `BrightnessFactor = max(0, gamma - darknessScale)`, gamma is capped at 1.0, and a lightmap texel
  with light level 0 stays 0 at any gamma. The module now overrides the light coordinate that every
  block and entity vertex is built from (`LightTexture.pack`) through a `@Inject` hook, so caves, night
  and dark rooms really are lit. It never writes the saved gamma option, and disabling it restores
  normal lighting immediately with no state to unwind.
- **Three Fullbright modes:** **Full** (light coordinates forced to 15/15), **Gamma (soft)** (dark
  areas lifted to a mid level so shading is kept) and **Night vision style** (near-maximum brightness
  with a little sky tint, no potion flash or fade), plus a **Brightness** slider for the two soft
  modes. The mode and lift are cached as plain ints, so the per-vertex path allocates nothing.
- **Fullbright reports itself.** A status line (on/off, mode, light floor, whether the lightmap hook
  applied) is shown in the Diagnostics tab and `/omnicube diag`. If the hook does not apply, that is
  logged and the module falls back to a session-only gamma raise instead of doing nothing.
- **Compatibility:** the existing Gamma Utils / Fullbright rules still disable OmniCube's module when
  another mod owns fullbright; the normal blocked message is shown.
- **Bug fixes:** the entity-count HUD no longer throws when the render entity view changes during its
  scan; the HUD text measurer accepts an element with no lines instead of failing on a null list; the
  splash version fallback no longer names a release.

## 0.2.3 — comfort and polish (Minecraft 1.21.11)

- **Free Look behaves like a head turn, not an orbit.** The camera stays in first person, the yaw is
  clamped to a configurable arc (110° by default, 30–180°) instead of spinning a full 360°, and the
  view eases back to your aim when the key is released instead of snapping. Mouse scaling now follows
  your sensitivity and invert options, so it feels the same as normal looking. Settings: **Yaw range**,
  **Sensitivity**, **Smooth return**, **Stay in first person**.
- **Zoom is smooth.** The field of view is interpolated for every rendered frame from the game's FOV
  calculation, with adjustable **Zoom factor**, **Smooth animation** and **Animation time**. Your saved
  FOV option is never written, so disabling the module or leaving the world cannot change your settings.
- **Custom Splash.** A new module replaces the Mojang studios loading screen with an OmniCube screen:
  dark gradient, wordmark, version, loading progress bar and a key hint. It hands the screen back to
  vanilla as soon as the overlay starts fading out. Settings: **Show loading progress**, **Show key hints**.
- **Fullbright now really removes darkness.** The default brightness is 16 (the maximum), which the
  lightmap applies on top of night and cave light instead of merely softening it.
- **Armor HUD shows what you wear.** Each piece is named by material ("Iron", "Diamond", "Leather")
  with its remaining durability, coloured green at 90 %+, amber at 45 % or less and a normal red at
  20 % or less. With no armor equipped the panel is hidden entirely, and the potion panel disappears
  when you have no active effects.
- **TNT fuse crash fixed.** Lighting TNT while the TNT Fuse element was on could take the frame down:
  the element cached a live entity across frames and iterated the live entity list. It now copies the
  list, guards each entity and stores only the fuse and distance, so a removed or exploding TNT can no
  longer crash the HUD.
- **Startup self-check no longer reports working mixins as failed.** Mixin renames handler methods at
  runtime, so the exact-name reflection check marked every applied mixin as "NOT APPLIED". The check
  now matches the renamed handler, so the diagnostics log reports the real state of free look, zoom,
  tooltips, name tags, entity culling and chat collapse.

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
