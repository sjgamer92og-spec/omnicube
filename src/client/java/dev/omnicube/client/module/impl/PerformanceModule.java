package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.optimization.AdaptiveOptimizationBackend;
import dev.omnicube.client.optimization.OptimizationBackend;
import dev.omnicube.client.optimization.OptimizationSettings;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

public final class PerformanceModule extends Module {
	private static AdaptiveOptimizationBackend adaptive = new AdaptiveOptimizationBackend();
	private static OptimizationBackend backend = adaptive;

	private final Setting.Num targetFps = add(new Setting.Num("target_fps", "Target FPS", 60.0D, 30.0D, 240.0D, 10.0D));
	private final Setting.Num minRenderDistance = add(new Setting.Num("min_render_distance", "Minimum render distance", 4.0D, 2.0D, 16.0D, 1.0D));
	private final Setting.Bool adaptiveParticles = add(new Setting.Bool("adaptive_particles", "Reduce particles under load", true));
	private final Setting.Bool adaptiveRenderDistance = add(new Setting.Bool("adaptive_render_distance", "Reduce render distance under load", true));
	private final Setting.Bool adaptiveClouds = add(new Setting.Bool("adaptive_clouds", "Reduce clouds under load", true));
	private final Setting.Bool adaptiveEntityDistance = add(new Setting.Bool("adaptive_entity_distance", "Reduce entity distance under load", true));
	private final Setting.Bool hardwareTiers = add(new Setting.Bool("hardware_tiers", "Hardware tiers and presets", true));

	public PerformanceModule() {
		super("performance", "Performance", "Hardware-aware adaptive quality: picks a preset for your machine, then steps render distance, simulation distance and entity distance with your frame rate. Changes only vanilla video options and restores them when disabled.", ModuleCategory.PERFORMANCE, false);
	}

	/**
	 * Allows an alternative backend to be installed at startup.
	 */
	public static void setBackend(OptimizationBackend newBackend) {
		if (newBackend != null) {
			backend = newBackend;
		}
	}

	/** Current backend id, shown in diagnostics. */
	public static String backendId() {
		return backend.id();
	}

	/** True while the adaptive backend has handed control to the built-in fallback. */
	public static boolean backendUsingFallback() {
		return adaptive.usingFallback();
	}

	/** Tells the adaptive backend that a world was joined so scaling waits to settle. */
	public void onWorldJoin() {
		try {
			adaptive.noteWorldJoin();
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.join", throwable);
		}
	}

	public static String backendName() {
		return backend.id();
	}

	@Override
	protected void onEnable() {
		try {
			backend.capture(Minecraft.getInstance());
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.error("[OmniCube] Performance backend '{}' failed to capture settings.", backend.id(), throwable);
		}
	}

	@Override
	protected void onDisable() {
		try {
			backend.restore(Minecraft.getInstance());
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.error("[OmniCube] Performance backend '{}' failed to restore settings.", backend.id(), throwable);
		}
	}

	public void restoreForSession() {
		try {
			backend.restore(Minecraft.getInstance());
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not restore performance options.", throwable);
		}
	}

	public void captureForSession() {
		try {
			backend.capture(Minecraft.getInstance());
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not capture performance options.", throwable);
		}
	}

	@Override
	public void tick() {
		try {
			backend.tick(Minecraft.getInstance(), new OptimizationSettings(
					targetFps.intValue(),
					minRenderDistance.intValue(),
					adaptiveParticles.get(),
					adaptiveRenderDistance.get(),
					adaptiveClouds.get(),
					adaptiveEntityDistance.get(),
					hardwareTiers.get()
			));
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.tick", throwable);
			OmniCubeMod.LOGGER.debug("[OmniCube] Performance tick failed; disabling the module.", throwable);
			markFailed("Failed: " + Diagnostics.describe(throwable) + " in PerformanceModule.tick() (see stack trace)");
		}
	}
}
