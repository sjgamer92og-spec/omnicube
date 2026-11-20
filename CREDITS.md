# Credits

## OmniCube

Original code, artwork and design: **sjgamer92og**, licensed under the **PolyForm Shield License 1.0.0**.

Required Notice: Copyright (c) 2026 sjgamer92og (OmniCube)

No third-party mod source code or assets are included in this repository. v0.2.0's free look, container tooltip grid, name tags, chat timestamps/search/collapse, streamer redaction, crosshair renderer, auto-reconnect, conservative entity culling, world markers, compatibility registry and inventory tools were written from scratch against the public Minecraft/Fabric API and the decompiled mapping signatures. No code was copied from Cactus Mod or any other All Rights Reserved project.

The optional client mixins in `dev.omnicube.client.mixin` are small `@Inject` hooks written for OmniCube itself; they do not copy or redistribute another mod's mixin code. They exist only where the Fabric API has no public client hook (mouse/camera decoupling, tooltip image data, vanilla name-tag suppression, replacing the newest chat line, and a conservative entity-visibility check). The chat refresh method is reached through a generated Mixin accessor; no vanilla method is overwritten.

The new v0.2.0 logo is original geometric artwork generated for this repository; it does not use third-party logo assets or fonts.

## Inspiration

- **Cactus Mod** by Dwarslooper and contributors — inspiration for the "all-in-one client toolkit" feature set. Cactus Mod is **All Rights Reserved**, so no Cactus code, assets, names or config files were copied. OmniCube is an independent implementation.
- The Minecraft modding community for documenting Fabric APIs and client-side patterns.

## Compatibility research

The compatibility table is a list of loader IDs and display names. No third-party mod code, metadata or assets are bundled. The following projects were checked as overlapping providers during development:

- HUD: Essential, Raised, BetterHUD
- Tooltip/durability: Jade, AppleSkin, Inspecio, Shulker Box Tooltip
- Zoom: Logical Zoom, Ok Zoomer, Zoomify, Just Zoom
- Fullbright: Gamma Utils, Fullbright
- Free look: Freelook, Perspective Mod Redux
- Chat: Chat Heads, Chat Patches, Chat Plus
- Reconnect: Auto Reconnect
- Culling: Entity Culling
- Performance: Sodium, Lithium, FerriteCore, ImmediatelyFast, Iris, Indium, ModernFix, Dynamic FPS, Embeddium, Rubidium

These lists are loader-ID hypotheses, not bundled dependencies. A mod is only disabled when Fabric Loader reports its ID; no optional class is required. See `CompatRegistry.java` for the single source of truth.

## Runtime and build dependencies (not bundled beyond normal mod requirements)

| Project | License | Use |
| --- | --- | --- |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader) | Apache-2.0 | Mod loader |
| [Fabric API](https://github.com/FabricMC/fabric-api) | Apache-2.0 | Public client APIs (HUD, tooltips, chat, lifecycle, commands, keybinds) |
| [Minecraft](https://www.minecraft.net/) | Mojang EULA | Game runtime |
| [Gradle](https://gradle.org/) | Apache-2.0 | Build tool |
| [LWJGL](https://www.lwjgl.org/) | BSD-3-Clause | Input and window access (provided by Minecraft) |
| [Gson](https://github.com/google/gson) | Apache-2.0 | Config JSON (provided by Minecraft) |
| [SLF4J](https://www.slf4j.org/) | MIT | Logging (provided by Minecraft) |
| [Mixin](https://github.com/SpongePowered/Mixin) | MIT | Optional client hooks (provided by Fabric Loader) |

The Minecraft/Fabric dependencies are not redistributed in source form. The built jar expects the standard Fabric runtime and Fabric API.

## Performance backend

The adaptive performance code in `dev.omnicube.client.optimization` — the `OptimizationBackend` contract and `BuiltinOptimizationBackend` — is original OmniCube work written against the public Minecraft `Options` API, and is released under this repository's PolyForm Shield License 1.0.0 like the rest of the mod. It contains no third-party code.

The Performance module disables itself automatically when Sodium, Sodium Extra, VulkanMod, Embeddium, Rubidium, Dynamic FPS or OptiFine is installed, because those mods own the same video settings.

## License headers

All OmniCube source files are original work released under the repository's **PolyForm Shield License 1.0.0** (`Required Notice: Copyright (c) 2026 sjgamer92og (OmniCube)`). No copied files were used, so no third-party source headers are present. If code is adapted from another project in the future, the original header must be preserved and the project listed above with its license; such code stays under its own license and is not relicensed.
