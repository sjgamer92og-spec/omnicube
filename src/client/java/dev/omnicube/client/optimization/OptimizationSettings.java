package dev.omnicube.client.optimization;

public record OptimizationSettings(
		int targetFps,
		int minRenderDistance,
		boolean adaptiveParticles,
		boolean adaptiveRenderDistance,
		boolean adaptiveClouds,
		boolean adaptiveEntityDistance
) {
}
