package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Full brightness, done the only way 1.21.11 allows it.
 *
 * <p>{@code lightmap.fsh} derives every texel from its own light level, so a level-0
 * texel stays black whatever the gamma option says. This hook replaces the packed
 * light coordinate that every block and entity vertex is built with, which is what
 * turns a dark cave into a lit one. The module decides the value; when it is off the
 * call is left exactly as vanilla made it.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@Inject(method = "pack", at = @At("HEAD"), cancellable = true, require = 0)
	private static void omnicube$fullbrightLight(int blockLight, int skyLight, CallbackInfoReturnable<Integer> callback) {
		try {
			int override = OmniCubeClient.fullbrightLight(blockLight, skyLight);
			if (override >= 0) {
				callback.setReturnValue(override);
			}
		} catch (Throwable throwable) {
			// Never let full brightness take a frame down; vanilla lighting continues.
			Diagnostics.exception("fullbright.lightmap", throwable);
		}
	}
}
