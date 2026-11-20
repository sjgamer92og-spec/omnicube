package dev.omnicube.client.optimization;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

import java.util.Locale;

/**
 * Hardware-aware adaptive backend.
 *
 * <p>Two layers, both from the optimisation work this repository is built on and
 * rewritten for OmniCube's backend contract:</p>
 *
 * <ol>
 *   <li><b>Preset</b> — the first probe classifies the machine into a
 *       {@link HardwareTier} and applies the matching {@link QualityPreset} once.
 *       A preset only ever lowers a value; it never raises one above what the
 *       player already had.</li>
 *   <li><b>Scaling</b> — once per second a {@link QualityPolicy} step machine lowers
 *       or raises render distance, simulation distance and entity distance around
 *       the preset baseline, using hardware-aware minimum and comfort frame rates
 *       with hysteresis, cooldowns and a post-join settle window.</li>
 * </ol>
 *
 * <p>Only vanilla {@link Options} are touched, so there is nothing for another
 * renderer mod to fight over, and every original value is captured before the
 * first change and put back when the module is disabled, when the toggle is turned
 * off, and on quit. Nothing here reads or writes files, and nothing scans the world:
 * the only per-tick work is one FPS read plus a comparison, with the decision
 * machine running once per second.</p>
 */
public final class AdaptiveOptimizationBackend implements OptimizationBackend {
	public static final String ID = "adaptive";
	private static final int MIN_DISTANCE = 2;

	private final BuiltinOptimizationBackend fallback = new BuiltinOptimizationBackend();

	private boolean captured;
	private boolean failed;
	private boolean usingFallback;
	private boolean rendererQueried;

	private int originalRenderDistance = -1;
	private int originalSimulationDistance = -1;
	private int originalBiomeBlend = -1;
	private ParticleStatus originalParticles;
	private CloudStatus originalClouds;
	private double originalEntityDistance = -1.0D;

	private int baselineRenderDistance = -1;
	private int baselineSimulationDistance = -1;
	private double baselineEntityDistance = -1.0D;

	private HardwareProfile hardware;
	private HardwareTier tier = HardwareTier.MID;
	private QualityPreset preset = QualityPreset.BALANCED;
	private QualityPolicy.Engine engine;
	private long lastDecisionMs;
	private int appliedTier = -1;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void capture(Minecraft client) {
		if (captured || client == null || client.options == null) {
			return;
		}
		Options options = client.options;
		try {
			originalRenderDistance = options.renderDistance().get();
			originalSimulationDistance = options.simulationDistance().get();
			originalBiomeBlend = options.biomeBlendRadius().get();
			originalParticles = options.particles().get();
			originalClouds = options.cloudStatus().get();
			originalEntityDistance = options.entityDistanceScaling().get();
			captured = true;
			appliedTier = -1;
			lastDecisionMs = 0L;
			engine = new QualityPolicy.Engine(limits(), QualityPolicy.Tuning.defaults());
			engine.reset();

			hardware = HardwareProbe.current();
			tier = hardware.tier();
			preset = QualityPreset.fromTier(tier);
			applyPreset(options);

			baselineRenderDistance = options.renderDistance().get();
			baselineSimulationDistance = options.simulationDistance().get();
			baselineEntityDistance = options.entityDistanceScaling().get();

			OmniCubeMod.LOGGER.info(
					"[OmniCube] Adaptive performance: tier {} ({}), preset {} -> render {}, simulation {}, entity {}",
					tier.displayName(), hardware.summary(), preset.displayName(),
					baselineRenderDistance, baselineSimulationDistance, baselineEntityDistance);
			Diagnostics.performance("adaptive preset " + preset.key() + " for tier " + tier.displayName()
					+ " (render " + baselineRenderDistance + ", simulation " + baselineSimulationDistance
					+ ", entity " + two(baselineEntityDistance) + ")");
			Diagnostics.status("performance_backend", "Adaptive quality (hardware tiers)", "backend", "ENABLED",
					"Tier " + tier.displayName() + ", preset " + preset.displayName() + ", min "
							+ one(limits().minFps()) + " FPS");
		} catch (Throwable throwable) {
			fail(client, throwable);
		}
	}

	@Override
	public void restore(Minecraft client) {
		if (!captured || client == null || client.options == null) {
			captured = false;
			return;
		}
		restoreOptions(client.options);
		captured = false;
		appliedTier = -1;
		engine = null;
		Diagnostics.performance("adaptive disabled; original video settings restored");
		Diagnostics.status("performance_backend", "Adaptive quality (hardware tiers)", "backend", "DISABLED",
				"Disabled by user; original video settings restored");
	}

	@Override
	public void tick(Minecraft client, OptimizationSettings settings) {
		if (failed) {
			if (usingFallback) {
				fallback.tick(client, settings);
			}
			return;
		}
		try {
			if (!settings.hardwareTiers()) {
				if (captured) {
					restore(client);
				}
				return;
			}
			if (!captured) {
				capture(client);
			}
			if (!captured || client == null || client.options == null) {
				return;
			}
			if (!rendererQueried) {
				// The GL renderer string is only readable on the render thread, so the
				// probe runs here rather than during config load.
				rendererQueried = true;
				HardwareProbe.queryRenderer();
				hardware = HardwareProbe.current();
				HardwareTier detected = hardware.tier();
				if (detected != tier) {
					tier = detected;
					Diagnostics.performance("adaptive tier changed to " + tier.displayName()
							+ " (" + hardware.summary() + ")");
				}
			}

			long now = System.currentTimeMillis();
			if (now - lastDecisionMs < 1000L) {
				return;
			}
			lastDecisionMs = now;

			boolean eligible = client.player != null && client.level != null && client.screen == null;
			int fps = 0;
			try {
				fps = client.getFps();
			} catch (Throwable throwable) {
				Diagnostics.exception("performance.adaptive.fps", throwable);
				return;
			}
			double frameMs = fps > 0 ? 1000.0D / fps : 0.0D;
			QualityPolicy.Decision decision = engine.tick(now, fps, frameMs, eligible);
			if (decision.changed()) {
				applyScale(client.options, decision.tier());
				Diagnostics.performance("adaptive step " + decision.step() + " to " + decision.tier() + "/"
						+ QualityPolicy.MAX_TIER + ": " + decision.reason());
				Diagnostics.status("performance_backend", "Adaptive quality (hardware tiers)", "backend", "ENABLED",
						"Tier " + tier.displayName() + ", step " + decision.tier() + "/"
								+ QualityPolicy.MAX_TIER + ", scale " + two(engine.scale()));
			}
		} catch (Throwable throwable) {
			fail(client, throwable);
		}
	}

	/** Called on a world join so the policy waits for a settled frame time. */
	public void noteWorldJoin() {
		try {
			if (engine != null) {
				engine.noteWorldJoin(System.currentTimeMillis());
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.adaptive.join", throwable);
		}
	}

	public HardwareTier tier() {
		return tier;
	}

	public QualityPreset preset() {
		return preset;
	}

	public boolean usingFallback() {
		return usingFallback;
	}

	private void applyPreset(Options options) {
		if (preset.renderDistance() > 0 && originalRenderDistance > preset.renderDistance()) {
			options.renderDistance().set(preset.renderDistance());
		}
		if (preset.simulationDistance() > 0 && originalSimulationDistance > preset.simulationDistance()) {
			options.simulationDistance().set(preset.simulationDistance());
		}
		if (preset.entityDistance() < originalEntityDistance) {
			options.entityDistanceScaling().set(preset.entityDistance());
		}
		if (preset.biomeBlend() >= 0 && originalBiomeBlend > preset.biomeBlend()) {
			options.biomeBlendRadius().set(preset.biomeBlend());
		}
		switch (preset.particleLevel()) {
			case 0 -> options.particles().set(ParticleStatus.MINIMAL);
			case 1 -> options.particles().set(ParticleStatus.DECREASED);
			case 2 -> {
				if (originalParticles != ParticleStatus.ALL) {
					options.particles().set(ParticleStatus.ALL);
				}
			}
			default -> {
				// Keep the player's value.
			}
		}
		if (preset == QualityPreset.MINIMUM) {
			options.cloudStatus().set(CloudStatus.OFF);
		} else if (preset == QualityPreset.LOW || preset == QualityPreset.BALANCED) {
			if (originalClouds == CloudStatus.FANCY) {
				options.cloudStatus().set(CloudStatus.FAST);
			}
		}
	}

	/**
	 * Applies one quality step around the preset baseline. Values never exceed the
	 * baseline and never drop below {@link #MIN_DISTANCE}.
	 */
	private void applyScale(Options options, int stepTier) {
		if (stepTier == appliedTier || baselineRenderDistance <= 0) {
			return;
		}
		appliedTier = stepTier;
		double scale = QualityPolicy.SCALES[Math.max(0, Math.min(QualityPolicy.MAX_TIER, stepTier))];
		int render = Math.max(MIN_DISTANCE, (int) Math.round(baselineRenderDistance * scale));
		int simulation = Math.max(MIN_DISTANCE, (int) Math.round(baselineSimulationDistance * scale));
		double entity = Math.max(0.5D, Math.min(baselineEntityDistance, baselineEntityDistance * scale));
		if (render != options.renderDistance().get()) {
			options.renderDistance().set(render);
		}
		if (simulation != options.simulationDistance().get()) {
			options.simulationDistance().set(simulation);
		}
		if (Math.abs(entity - options.entityDistanceScaling().get()) > 0.001D) {
			options.entityDistanceScaling().set(entity);
		}
	}

	private void restoreOptions(Options options) {
		try {
			if (originalRenderDistance > 0) {
				options.renderDistance().set(originalRenderDistance);
			}
			if (originalSimulationDistance > 0) {
				options.simulationDistance().set(originalSimulationDistance);
			}
			if (originalBiomeBlend >= 0) {
				options.biomeBlendRadius().set(originalBiomeBlend);
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
			Diagnostics.exception("performance.adaptive.restore", throwable);
			OmniCubeMod.LOGGER.warn("[OmniCube] Adaptive performance: could not restore every video setting.", throwable);
		}
	}

	/**
	 * Any failure restores the original values once and hands the rest of the session
	 * to the conservative built-in backend, so the module keeps working without
	 * repeated exceptions.
	 */
	private void fail(Minecraft client, Throwable throwable) {
		if (failed) {
			return;
		}
		failed = true;
		Diagnostics.exception("performance.adaptive", throwable);
		OmniCubeMod.LOGGER.warn("[OmniCube] Adaptive performance failed; restoring settings and falling back "
				+ "to the built-in backend.", throwable);
		try {
			if (client != null && client.options != null && captured) {
				restoreOptions(client.options);
			}
		} catch (Throwable restoreFailure) {
			Diagnostics.exception("performance.adaptive.fail_restore", restoreFailure);
		}
		captured = false;
		engine = null;
		try {
			usingFallback = true;
			if (client != null) {
				fallback.capture(client);
			}
		} catch (Throwable fallbackFailure) {
			Diagnostics.exception("performance.adaptive.fallback", fallbackFailure);
		}
		Diagnostics.status("performance_backend", "Adaptive quality (hardware tiers)", "backend", "FAILED",
				"Failed: " + Diagnostics.describe(throwable) + "; built-in backend taking over");
	}

	private QualityPolicy.Limits limits() {
		HardwareTier current = hardware == null ? tier : hardware.tier();
		double min = QualityPolicy.minFps(0, current);
		double comfort = QualityPolicy.comfortFps(0, min);
		return new QualityPolicy.Limits(min, comfort);
	}

	private static String one(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String two(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
