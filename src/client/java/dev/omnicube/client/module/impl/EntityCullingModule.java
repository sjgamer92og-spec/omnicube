package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Conservative visibility check for mobs. Results are cached between render
 * frames and each tick has a hard raycast budget, so this never becomes a
 * per-frame world scan.
 */
public final class EntityCullingModule extends Module {
	private final Setting.Num maxDistance = add(new Setting.Num("max_distance", "Max distance", 64.0D, 16.0D, 128.0D, 8.0D));
	private final Setting.Num interval = add(new Setting.Num("interval", "Recheck ticks", 5.0D, 2.0D, 20.0D, 1.0D));

	private final Map<Integer, CacheEntry> cache = new HashMap<>();
	private long ticks;
	private int checksThisTick;
	private Level lastLevel;
	private boolean failureLogged;

	private record CacheEntry(boolean cull, long tick) {
	}

	public EntityCullingModule() {
		super("entity_culling", "Entity Culling", "Skips mobs whose center is hidden behind a solid block. Conservative and cached.", ModuleCategory.RENDER, false);
	}

	@Override
	public void tick() {
		tick(Minecraft.getInstance());
	}

	public void tick(Minecraft client) {
		ticks++;
		checksThisTick = 0;
		try {
			if (client.level != lastLevel) {
				lastLevel = client.level;
				cache.clear();
			}
			if (cache.size() > 512) {
				cache.clear();
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("entity_culling.tick", throwable);
			cache.clear();
		}
	}

	public boolean shouldCull(Entity entity) {
		if (!isEnabled() || entity == null) {
			return false;
		}
		try {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.level == null || entity == client.player) {
				return false;
			}
			if (!(entity instanceof Mob) || entity.isCurrentlyGlowing() || entity.isSpectator()) {
				return false;
			}
			double max = maxDistance.get();
			if (client.player.distanceToSqr(entity) > max * max) {
				return false;
			}
			CacheEntry cached = cache.get(entity.getId());
			if (cached != null && ticks - cached.tick < interval.intValue()) {
				return cached.cull;
			}
			if (checksThisTick >= 24) {
				return false;
			}
			checksThisTick++;
			Vec3 start = client.player.getEyePosition();
			Vec3 end = entity.getBoundingBox().getCenter();
			if (start.distanceToSqr(end) < 4.0D) {
				return false;
			}
			HitResult hit = client.level.clip(new ClipContext(start, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
			boolean cull = hit.getType() == HitResult.Type.BLOCK;
			cache.put(entity.getId(), new CacheEntry(cull, ticks));
			return cull;
		} catch (Throwable throwable) {
			Diagnostics.exception("entity_culling.check", throwable);
			if (!failureLogged) {
				failureLogged = true;
				OmniCubeMod.LOGGER.warn("[OmniCube] Entity culling check failed; skipping culling for this session.", throwable);
			}
			markFailed("Failed: " + Diagnostics.describe(throwable) + " in EntityCullingModule.shouldCull() (see stack trace)");
			return false;
		}
	}

	public void fail(Throwable throwable) {
		Diagnostics.exception("entity_culling.hook", throwable);
		if (!failureLogged) {
			failureLogged = true;
			OmniCubeMod.LOGGER.error("[OmniCube] Entity culling failed; disabling it for this session.", throwable);
		}
		markFailed("Failed: " + Diagnostics.describe(throwable) + " in entity culling hook (see stack trace)");
	}

	@Override
	protected void onDisable() {
		cache.clear();
	}
}
