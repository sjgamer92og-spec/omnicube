package dev.omnicube.client.perf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opt-in, allocation-free when disabled. Enable it with /omnicube perf, then
 * inspect the same command after playing. It intentionally measures client-side
 * callback time, not GPU time.
 */
public final class PerfMonitor {
	private static final Map<String, Sample> SAMPLES = new ConcurrentHashMap<>();
	private static volatile boolean enabled;

	private PerfMonitor() {
	}

	public static void enable() {
		enabled = true;
	}

	public static boolean enabled() {
		return enabled;
	}

	public static long start() {
		return enabled ? System.nanoTime() : 0L;
	}

	public static void record(String id, long startNanos) {
		if (!enabled || startNanos == 0L || id == null) {
			return;
		}
		try {
			SAMPLES.computeIfAbsent(id, ignored -> new Sample()).add(System.nanoTime() - startNanos);
		} catch (Throwable ignored) {
			// Profiling must never affect gameplay.
		}
	}

	public static void reset() {
		SAMPLES.clear();
		enabled = false;
	}

	public static List<String> summary() {
		List<Map.Entry<String, Sample>> entries = new ArrayList<>(SAMPLES.entrySet());
		entries.sort(Comparator.comparing(Map.Entry::getKey));
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Sample> entry : entries) {
			Sample sample = entry.getValue();
			long count = sample.count;
			if (count <= 0L) {
				continue;
			}
			double averageMicros = sample.totalNanos / (double) count / 1_000.0D;
			lines.add(entry.getKey() + ": " + String.format(java.util.Locale.ROOT, "%.2f us avg", averageMicros)
					+ " (" + count + " calls)");
		}
		return lines;
	}

	private static final class Sample {
		private long totalNanos;
		private long count;

		synchronized void add(long nanos) {
			totalNanos += Math.max(0L, nanos);
			count++;
		}
	}
}
