package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.module.impl.FreeLookModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup", at = @At("RETURN"))
	private void omnicube$applyFreeLook(Level level, Entity entity, boolean detached, boolean thirdPersonReverse,
			float partialTick, CallbackInfo callbackInfo) {
		try {
			FreeLookModule module = OmniCubeClient.freeLook();
			if (module != null && module.isLooking()) {
				setRotation(module.cameraYaw(), module.cameraPitch());
			}
		} catch (Throwable ignored) {
			// A broken camera hook must leave the vanilla camera untouched.
		}
	}
}
