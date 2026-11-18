package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.module.impl.EntityCullingModule;
import dev.omnicube.client.perf.PerfMonitor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderDispatcher.class, priority = 1500)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$cullHiddenMob(Entity entity, Frustum frustum, double x, double y, double z,
			CallbackInfoReturnable<Boolean> callback) {
		long started = PerfMonitor.start();
		try {
			EntityCullingModule module = OmniCubeClient.entityCulling();
			if (module != null && module.shouldCull(entity)) {
				callback.setReturnValue(false);
			}
		} catch (Throwable throwable) {
			// A failed optional hook must never crash rendering.
			EntityCullingModule module = OmniCubeClient.entityCulling();
			if (module != null) {
				module.fail(throwable);
			}
		} finally {
			PerfMonitor.record("render.entity_culling", started);
		}
	}
}
