package dev.omnicube.client.optimization;

import dev.omnicube.client.diag.Diagnostics;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/**
 * One-time hardware probe. It reads only system properties, the JVM heap limit and
 * (when called on the client thread with a GL context) the GL renderer string. The
 * result is cached because the probe is not free and must never run per tick.
 */
public final class HardwareProbe {
	private static HardwareProfile cached;
	private static boolean glQueried;
	private static String glRenderer = "";
	private static String glVendor = "";

	private HardwareProbe() {
	}

	/** Safe from any thread: reports what is known without touching OpenGL. */
	public static synchronized HardwareProfile current() {
		if (cached == null) {
			cached = build();
		}
		return cached;
	}

	/** Fills in the renderer string the first time it is called on the render thread. */
	public static synchronized void queryRenderer() {
		if (glQueried) {
			return;
		}
		glQueried = true;
		try {
			glRenderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
			glVendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
			if (glRenderer == null) {
				glRenderer = "";
			}
			if (glVendor == null) {
				glVendor = "";
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.hardware.gl", throwable);
		}
		cached = null;
	}

	public static synchronized void reset() {
		cached = null;
		glQueried = false;
		glRenderer = "";
		glVendor = "";
	}

	private static HardwareProfile build() {
		long maxMemoryMb = 0L;
		int processors = 1;
		try {
			maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
			processors = Math.max(1, Runtime.getRuntime().availableProcessors());
		} catch (Throwable throwable) {
			Diagnostics.exception("performance.hardware.runtime", throwable);
		}
		long physicalMemoryMb = 0L;
		try {
			java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
			if (bean instanceof com.sun.management.OperatingSystemMXBean extended) {
				long total = extended.getTotalMemorySize();
				if (total > 0L) {
					physicalMemoryMb = total / (1024L * 1024L);
				}
			}
		} catch (Throwable throwable) {
			// Not every JVM exposes the extended bean; the heap limit is still usable.
			Diagnostics.exception("performance.hardware.memory", throwable);
		}

		String renderer = glRenderer == null ? "" : glRenderer;
		String vendor = glVendor == null ? "" : glVendor;
		String combined = (renderer + ' ' + vendor).toLowerCase(Locale.ROOT);
		boolean mobileGpu = combined.contains("powervr") || combined.contains("adreno")
				|| combined.contains("mali") || combined.contains("videocore");
		boolean gl4es = combined.contains("gl4es") || combined.contains("mobileglues")
				|| combined.contains("virgl") || combined.contains("swiftshader");
		boolean weakGpu = combined.contains("intel(r) hd") || combined.contains("intel hd graphics")
				|| combined.contains("gma") || combined.contains("llvmpipe") || combined.contains("softpipe")
				|| combined.contains("software rasterizer");
		boolean ltw = combined.contains("ltw") || combined.contains("artdev") || combined.contains("mobileglues");
		return new HardwareProfile(maxMemoryMb, physicalMemoryMb, processors, renderer, vendor,
				mobileGpu, gl4es, weakGpu, ltw);
	}
}
