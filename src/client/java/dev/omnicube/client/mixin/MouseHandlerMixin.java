package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.impl.FreeLookModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 1500)
public abstract class MouseHandlerMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$freeLook(double value, CallbackInfo callbackInfo) {
		try {
			FreeLookModule module = OmniCubeClient.freeLook();
			if (module != null && module.handleMouse(accumulatedDX, accumulatedDY)) {
				accumulatedDX = 0.0D;
				accumulatedDY = 0.0D;
				callbackInfo.cancel();
			}
		} catch (Throwable throwable) {
			// If anything goes wrong, let vanilla handle the mouse normally.
			Diagnostics.exception("free_look.mouse_hook", throwable);
			FreeLookModule module = OmniCubeClient.freeLook();
			if (module != null) {
				module.fail(throwable);
			}
		}
	}
}
