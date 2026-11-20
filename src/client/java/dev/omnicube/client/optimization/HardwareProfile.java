package dev.omnicube.client.optimization;

import java.util.Locale;

/**
 * Immutable result of the first-launch hardware probe. {@code maxMemoryMb} is the
 * JVM heap limit; {@code physicalMemoryMb} is the device RAM when it can be read
 * (0 when unknown, for example on Windows).
 */
public record HardwareProfile(
		long maxMemoryMb,
		long physicalMemoryMb,
		int processors,
		String renderer,
		String vendor,
		boolean mobileGpu,
		boolean gl4es,
		boolean weakGpu,
		boolean ltw
) {
	public String summary() {
		String ram = this.physicalMemoryMb > 0L
				? (this.physicalMemoryMb / 1024L) + " GB device RAM, " + this.maxMemoryMb + " MB heap"
				: this.maxMemoryMb + " MB heap (device RAM unknown)";
		return ram + ", " + this.processors + " CPU threads, GPU: "
				+ (this.renderer == null || this.renderer.isBlank() ? "unknown" : this.renderer);
	}

	public long effectiveMemoryMb() {
		return this.physicalMemoryMb > 0L ? this.physicalMemoryMb : this.maxMemoryMb;
	}

	public HardwareTier tier() {
		return HardwareTier.detect(this);
	}

	/** Best-effort renderer family, used for logging and preset advice. */
	public String rendererFamily() {
		String name = this.renderer == null ? "" : this.renderer.toLowerCase(Locale.ROOT);
		String vendor = this.vendor == null ? "" : this.vendor.toLowerCase(Locale.ROOT);
		String combined = name + " " + vendor;
		if (this.ltw || combined.contains("ltw") || combined.contains("artdev")
				|| combined.contains("serpentspirale") || combined.contains("cadindie")) {
			return "LTW";
		}
		if (combined.contains("mobileglues")) {
			return "MobileGlues";
		}
		if (combined.contains("gl4es")) {
			return "GL4ES";
		}
		if (combined.contains("angle")) {
			return "ANGLE";
		}
		if (combined.contains("zink")) {
			return "Zink";
		}
		if (combined.contains("virgl")) {
			return "VirGL";
		}
		if (combined.contains("llvmpipe") || combined.contains("softpipe") || combined.contains("software")) {
			return "Software";
		}
		if (combined.contains("powervr") || combined.contains("adreno") || combined.contains("mali")) {
			return "Mobile native";
		}
		return combined.isBlank() ? "Unknown" : "Native";
	}

	/** Optional launcher hint from properties/environment; never a path. */
	public String launcherHint() {
		String[] properties = {"pojav.version", "pojavlauncher.version", "zalith.version", "boardwalk.version"};
		for (String property : properties) {
			String value = System.getProperty(property);
			if (value != null && !value.isBlank()) {
				return property.substring(0, property.indexOf('.')) + " " + value;
			}
		}
		String[] env = {"POJAV_LAUNCHER", "ZALITH_LAUNCHER", "AMETHYST_LAUNCHER"};
		for (String name : env) {
			String value = System.getenv(name);
			if (value != null && !value.isBlank()) {
				return name.toLowerCase(Locale.ROOT).replace('_', ' ') + " " + value;
			}
		}
		String hints = (System.getProperty("java.class.path", "") + ' '
				+ System.getProperty("sun.java.command", "")).toLowerCase(Locale.ROOT);
		if (hints.contains("mjlaunch") || hints.contains("git.artdeell")) {
			return "Zalith/MJLauncher";
		}
		if (hints.contains("pojavlauncher") || hints.contains("pojav")) {
			return "PojavLauncher";
		}
		if (hints.contains("boardwalk")) {
			return "Boardwalk";
		}
		if (hints.contains("foldcraftlauncher")) {
			return "Fold Craft Launcher";
		}
		String os = System.getProperty("os.name", "");
		return os.toLowerCase(Locale.ROOT).contains("android") ? "Android launcher" : "desktop";
	}

	/**
	 * Deliberately excludes the heap size: changing the heap should not re-apply a
	 * preset over the user's own choices. A different renderer or device does.
	 */
	public String fingerprint() {
		String gpu = this.renderer == null ? "" : this.renderer.strip().toLowerCase(Locale.ROOT);
		String glVendor = this.vendor == null ? "" : this.vendor.strip().toLowerCase(Locale.ROOT);
		return gpu + "|" + glVendor + "|" + this.processors + "|" + this.physicalMemoryMb
				+ "|ltw=" + this.ltw;
	}
}
