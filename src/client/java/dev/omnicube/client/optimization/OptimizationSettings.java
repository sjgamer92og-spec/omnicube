package dev.omnicube.client.optimization;

public record OptimizationSettings(
		int targetFps,
		int minRenderDistance,
		boolean adaptiveParticles,
		boolean adaptiveRenderDistance,
		boolean adaptiveClouds,
		boolean adaptiveEntityDistance,
		boolean hardwareTiers
) {
	/** Settings without hardware tier presets, for callers that only adapt to load. */
	public OptimizationSettings(int targetFps, int minRenderDistance, boolean adaptiveParticles,
			boolean adaptiveRenderDistance, boolean adaptiveClouds, boolean adaptiveEntityDistance) {
		this(targetFps, minRenderDistance, adaptiveParticles, adaptiveRenderDistance, adaptiveClouds,
				adaptiveEntityDistance, false);
	}
}
