package dev.omnicube.client.optimization;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Adaptive quality backend built only on vanilla {@link Options}. It never
 * touches rendering internals, so it cannot conflict with Sodium, Iris or other
 * renderer mods: it simply turns the same knobs the video settings screen does.
 */
public final class BuiltinOptimizationBackend implements OptimizationBackend {
	private boolean captured;
	private int originalRenderDistance = -1;
	private ParticleStatus originalParticles;
	private CloudStatus originalClouds;
	private double originalEntityDistance = -1.0D;

	private double smoothedFps = 60.0D;
	private int ticks;
	private int lastRenderDistance = -1;
	private long lastChangeAt;

	@Override
	public String id() {
		return "builtin";
	}

	@Override
	public void capture(Minecraft client) {
		if (captured || client == null || client.options == null) {
			return;
		}
		Options options = client.options;
		try {
			originalRenderDistance = options.renderDistance().get();
			originalParticles = options.particles().get();
			originalClouds = options.cloudStatus().get();
			originalEntityDistance = options.entityDistanceScaling().get();
			captured = true;
			OmniCubeMod.LOGGER.info("[OmniCube] Performance: captured video settings (rd={}, particles={}, clouds={}, entityDistance={})",
					originalRenderDistance, originalParticles, originalClouds, originalEntityDistance);
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.capture", throwable);
			OmniCubeMod.LOGGER.warn("[OmniCube] Performance: could not capture video settings.", throwable);
		}
	}

	@Override
	public void restore(Minecraft client) {
		if (!captured || client == null || client.options == null) {
			return;
		}
		Options options = client.options;
		try {
			if (originalRenderDistance > 0) {
				options.renderDistance().set(originalRenderDistance);
			}
			if (originalParticles != null) {
				options.particles().set(originalParticles);
			}
			if (originalClouds != null) {
				options.cloudStatus().set(originalClouds);
			}
			if (originalEntityDistance > 0.0D) {
				options.entityDistanceScaling().set(originalEntityDistance);
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.restore", throwable);
			OmniCubeMod.LOGGER.warn("[OmniCube] Performance: could not restore video settings.", throwable);
		} finally {
			captured = false;
			lastRenderDistance = -1;
		}
	}

	@Override
	public void tick(Minecraft client, OptimizationSettings settings) {
		if (!captured || client.player == null || client.level == null || client.screen != null) {
			return;
		}

		Options options = client.options;
		int fps;
		try {
			fps = client.getFps();
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.fps", throwable);
			return;
		}
		if (fps <= 0) {
			return;
		}

		smoothedFps = smoothedFps * 0.85D + fps * 0.15D;
		ticks++;
		if (ticks % 40 != 0) {
			return;
		}

		double target = settings.targetFps();
		boolean low = smoothedFps < target * 0.80D;
		boolean veryLow = smoothedFps < target * 0.60D;
		boolean recovered = smoothedFps > target * 1.05D;
		long now = System.currentTimeMillis();

		if (settings.adaptiveParticles()) {
			try {
				// Recovery restores what the player had, not always ALL: the old code raised
				// a saved MINIMAL/DECREASED particle setting to ALL once FPS recovered.
				ParticleStatus desired = veryLow ? ParticleStatus.MINIMAL
						: low ? ParticleStatus.DECREASED
						: recovered && originalParticles != null ? originalParticles
						: options.particles().get();
				if (desired != options.particles().get()) {
					options.particles().set(desired);
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("performance.particles", throwable);
				OmniCubeMod.LOGGER.debug("[OmniCube] Performance: particle adjustment failed.", throwable);
			}
		}

		if (settings.adaptiveClouds()) {
			try {
				CloudStatus desired = low ? CloudStatus.OFF : recovered ? originalClouds : options.cloudStatus().get();
				if (desired != null && desired != options.cloudStatus().get()) {
					options.cloudStatus().set(desired);
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("performance.clouds", throwable);
				OmniCubeMod.LOGGER.debug("[OmniCube] Performance: cloud adjustment failed.", throwable);
			}
		}

		if (settings.adaptiveEntityDistance()) {
			try {
				double desired = veryLow ? 0.5D : recovered ? originalEntityDistance : options.entityDistanceScaling().get();
				if (desired > 0.0D && Math.abs(desired - options.entityDistanceScaling().get()) > 0.001D) {
					options.entityDistanceScaling().set(desired);
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("performance.entity_distance", throwable);
				OmniCubeMod.LOGGER.debug("[OmniCube] Performance: entity distance adjustment failed.", throwable);
			}
		}

		if (settings.adaptiveRenderDistance()) {
			try {
				int current = options.renderDistance().get();
				if (lastRenderDistance < 0) {
					lastRenderDistance = current;
				}
				int floor = Math.min(settings.minRenderDistance(), Math.max(2, originalRenderDistance));
				if (low && current > floor && now - lastChangeAt > 4000L) {
					options.renderDistance().set(Math.max(floor, current - 2));
					lastChangeAt = now;
					lastRenderDistance = options.renderDistance().get();
				} else if (recovered && originalRenderDistance > 0 && current < originalRenderDistance && now - lastChangeAt > 8000L) {
					options.renderDistance().set(Math.min(originalRenderDistance, current + 1));
					lastChangeAt = now;
					lastRenderDistance = options.renderDistance().get();
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("performance.render_distance", throwable);
				OmniCubeMod.LOGGER.debug("[OmniCube] Performance: render-distance adjustment failed.", throwable);
			}
		}
	}
}
