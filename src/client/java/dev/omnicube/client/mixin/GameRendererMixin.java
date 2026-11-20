package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.impl.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the animated zoom to the field of view that is actually used for
 * rendering, so the easing is per frame and the FOV option stays untouched.
 */
@Mixin(value = GameRenderer.class, priority = 1500)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
	private void omnicube$applyZoom(Camera camera, float partialTick, boolean useFovSetting,
			CallbackInfoReturnable<Float> callbackInfo) {
		try {
			ZoomModule module = OmniCubeClient.zoom();
			if (module == null || !module.isEnabled()) {
				return;
			}
			float multiplier = module.fovMultiplier();
			if (multiplier < 0.999F) {
				callbackInfo.setReturnValue(callbackInfo.getReturnValue().floatValue() * multiplier);
			}
		} catch (Throwable throwable) {
			// A failed zoom leaves the vanilla field of view in place.
			Diagnostics.exception("zoom.fov", throwable);
			ZoomModule module = OmniCubeClient.zoom();
			if (module != null) {
				module.restore();
			}
		}
	}
}
