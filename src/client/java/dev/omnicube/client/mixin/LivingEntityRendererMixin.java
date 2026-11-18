package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.module.impl.NameTagModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$hideVanillaPlayerNameTag(LivingEntity entity, double distance,
			CallbackInfoReturnable<Boolean> callbackInfo) {
		try {
			NameTagModule module = OmniCubeClient.nameTags();
			if (module != null && entity instanceof Player player && module.shouldReplace(player)) {
				callbackInfo.setReturnValue(false);
			}
		} catch (Throwable throwable) {
			// Leave vanilla name tags visible if the module state is unavailable.
			NameTagModule module = OmniCubeClient.nameTags();
			if (module != null) {
				module.fail(throwable);
			}
		}
	}
}
