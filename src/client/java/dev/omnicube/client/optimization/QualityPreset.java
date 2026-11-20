package dev.omnicube.client.optimization;

/**
 * Quality presets reduced to the vanilla video options OmniCube can safely change.
 * Zero means "leave the user's value alone".
 */
public enum QualityPreset {
	MINIMUM("minimum", "Minimum", "Very low hardware: small view and simulation distance.",
			4, 5, 0.5D, 0, 0),
	LOW("low", "Low", "Older laptops and low-end phones.",
			6, 6, 0.65D, 2, 1),
	BALANCED("balanced", "Balanced", "The default: most of the gain with a vanilla-looking result.",
			8, 8, 0.85D, 2, 2),
	QUALITY("quality", "Quality", "Only scaling after a real drop; no preset changes.",
			0, 0, 1.0D, -1, -1);

	private final String key;
	private final String displayName;
	private final String description;
	private final int renderDistance;
	private final int simulationDistance;
	private final double entityDistance;
	private final int biomeBlend;
	private final int particleLevel;

	QualityPreset(String key, String displayName, String description, int renderDistance,
			int simulationDistance, double entityDistance, int biomeBlend, int particleLevel) {
		this.key = key;
		this.displayName = displayName;
		this.description = description;
		this.renderDistance = renderDistance;
		this.simulationDistance = simulationDistance;
		this.entityDistance = entityDistance;
		this.biomeBlend = biomeBlend;
		this.particleLevel = particleLevel;
	}

	public String key() {
		return key;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	/** Target render distance, or 0 to keep the user's value. */
	public int renderDistance() {
		return renderDistance;
	}

	/** Target simulation distance, or 0 to keep the user's value. */
	public int simulationDistance() {
		return simulationDistance;
	}

	/** Entity distance scaling (0.5 - 1.0). */
	public double entityDistance() {
		return entityDistance;
	}

	/** Biome blend radius, or -1 to keep the user's value. */
	public int biomeBlend() {
		return biomeBlend;
	}

	/** Particle level: 0 = minimal, 1 = decreased, 2 = all, -1 = keep. */
	public int particleLevel() {
		return particleLevel;
	}

	public static QualityPreset fromTier(HardwareTier tier) {
		if (tier == null) {
			return BALANCED;
		}
		return switch (tier) {
			case VERY_LOW -> MINIMUM;
			case LOW -> LOW;
			case MID -> BALANCED;
			case HIGH -> QUALITY;
		};
	}
}
