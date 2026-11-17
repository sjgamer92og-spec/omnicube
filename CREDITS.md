# Credits

## OmniCube

Original code, artwork and design: **sjgamer92og-spec**, licensed under the MIT License.

No third-party mod source code or assets are included in this repository. The features were written from scratch against the public Minecraft and Fabric API documentation.

## Inspiration

- **Cactus Mod** by Dwarslooper and contributors — inspiration for the "all-in-one client toolkit" feature set. Cactus Mod is **All Rights Reserved**, so no Cactus code, assets, names or config files were copied. OmniCube is an independent implementation.
- The Minecraft modding community for documenting Fabric APIs and client-side patterns.

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

The Minecraft/Fabric dependencies are not redistributed in source form. The built jar expects the standard Fabric runtime and Fabric API.



The module boundary is intentionally prepared:

- `dev.omnicube.client.optimization.OptimizationBackend` — backend contract
- `dev.omnicube.client.optimization.BuiltinOptimizationBackend` — current, vanilla-options-only backend


2. Keep its original license header and add the license text here.
3. Implement `OptimizationBackend` and register it during client init.
4. Add the author's copyright line and any required notice to this file and to the jar's `LICENSE_omnicube`-adjacent notices.

## License headers

All OmniCube source files are original work released under the repository's MIT License. No copied files were used, so no third-party source headers are present. If code is adapted from another project in the future, the original header must be preserved and the project listed above with its license.
