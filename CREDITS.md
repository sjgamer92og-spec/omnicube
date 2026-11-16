# Credits


Original code, artwork and design: **sjgamer92og-spec**, licensed under the MIT License.

No third-party mod source code or assets are included in this repository. The features were written from scratch against the public Minecraft and Fabric API documentation.

## Inspiration

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



2. Keep its original license header and add the license text here.
3. Implement `OptimizationBackend` and register it during client init.

## License headers

