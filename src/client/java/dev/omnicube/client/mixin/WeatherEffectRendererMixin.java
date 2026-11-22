package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vehicle for the "Hide rain and snow" visual toggle.
 *
 * <p>The rain and snow pass is extracted into its own render state, so cancelling
 * that extraction removes the weather without touching the rest of the world pass.
 * The handler is zero-argument on purpose: it stays valid if Mojang changes the
 * method signature, and {@code require = 0} keeps the game starting either way.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$hideWeather(CallbackInfo callback) {
		try {
			if (OmniCubeClient.hideWeather()) {
				callback.cancel();
			}
		} catch (Throwable throwable) {
			// Never let a visual toggle take the world render down.
			dev.omnicube.client.diag.Diagnostics.exception("render.weather", throwable);
		}
	}
}
