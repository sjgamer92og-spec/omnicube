package dev.omnicube.client.optimization;

/**
 * Coarse hardware classes used to pick a quality preset. Deliberately coarse: the
 * point is to keep the most aggressive preset away from desktops and the untuned
 * preset on strong machines, not to benchmark the device.
 */
public enum HardwareTier {
	VERY_LOW("Very low"),
	LOW("Low"),
	MID("Mid"),
	HIGH("High");

	private final String displayName;

	HardwareTier(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public static HardwareTier detect(HardwareProfile profile) {
		if (profile == null) {
			return MID;
		}
		long ram = profile.effectiveMemoryMb();
		String renderer = profile.renderer() == null ? "" : profile.renderer().toLowerCase(java.util.Locale.ROOT);
		boolean translated = profile.gl4es() || profile.mobileGpu() || profile.ltw();
		if (translated) {
			if (ram <= 3200L || profile.maxMemoryMb() <= 1400L
					|| renderer.contains("powervr") || renderer.contains("mali-4")
					|| renderer.contains("adreno (tm) 3") || renderer.contains("swiftshader")) {
				return VERY_LOW;
			}
			return LOW;
		}
		if (profile.weakGpu() || ram < 5000L || profile.processors() <= 4) {
			return LOW;
		}
		if (ram < 10000L || profile.processors() <= 8) {
			return MID;
		}
		return HIGH;
	}
}
