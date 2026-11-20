package dev.omnicube.client.optimization;

import java.util.Locale;

/**
 * Pure quality-step state machine, independent of Minecraft so it can be reasoned
 * about (and unit-tested) on its own.
 *
 * <p>An earlier iteration compared every frame against a fixed 60 FPS target. On a
 * device whose comfortable ceiling is about 26 FPS that target is unreachable, so
 * the ladder walked to the floor and the step-up condition could never become true.
 * This policy compares against hardware-aware minimum and comfort thresholds
 * instead:</p>
 *
 * <ul>
 *   <li>step DOWN only while FPS is below the minimum for N seconds, never when
 *       FPS is already at or above it;</li>
 *   <li>step UP when FPS holds at or above the comfort level (1.25x the minimum),
 *       or when frame time has been stable for a while and the last down-step is
 *       old enough;</li>
 *   <li>cooldowns and the gap between the down and up thresholds prevent
 *       oscillation.</li>
 * </ul>
 *
 * <p>A world join or dimension change pauses decisions for at least 10 s, then
 * until frame time has been stable for 5 s, with a hard 40 s ceiling.</p>
 */
public final class QualityPolicy {
	public static final double[] SCALES = {0.35D, 0.45D, 0.55D, 0.65D, 0.78D, 0.88D, 0.95D, 1.00D};
	public static final int MAX_TIER = SCALES.length - 1;

	public enum Step { NONE, DOWN, UP }

	public record Decision(Step step, int tier, String reason) {
		public boolean changed() {
			return step != Step.NONE;
		}
	}

	public record Limits(double minFps, double comfortFps) {
		public double minFrameMs() {
			return 1000.0D / Math.max(1.0D, minFps);
		}

		public double comfortFrameMs() {
			return 1000.0D / Math.max(1.0D, comfortFps);
		}
	}

	public record Tuning(int downSeconds, int upSeconds, long cooldownMs, long stableAfterDownMs,
			int stableUpSeconds, long joinMinMs, long joinMaxMs, int joinStableSeconds) {
		public static Tuning defaults() {
			return new Tuning(4, 8, 5_000L, 30_000L, 12, 10_000L, 40_000L, 5);
		}
	}

	/** Mutable per-session state. Not thread-safe; it only runs on the client thread. */
	public static final class Engine {
		private final Limits limits;
		private final Tuning tuning;
		private int tier = MAX_TIER;
		private int maxTier = MAX_TIER;
		private int lowSeconds;
		private int highSeconds;
		private int stableSeconds;
		private int joinStableSeconds;
		private long lastChangeMs;
		private long lastDownMs;
		private long joinStartMs = -1L;
		private boolean joinPaused;
		private double lastSecondFrameMs;
		private volatile String pauseReason = "";
		private long pauseUntilMs;

		public Engine(Limits limits, Tuning tuning) {
			this.limits = limits;
			this.tuning = tuning == null ? Tuning.defaults() : tuning;
		}

		public int tier() {
			return tier;
		}

		public int maxTier() {
			return maxTier;
		}

		public double scale() {
			return SCALES[Math.max(0, Math.min(MAX_TIER, tier))];
		}

		public String pauseText(long now) {
			if (joinPaused) {
				long since = now - joinStartMs;
				return "join " + (since / 1000L) + "s stable " + joinStableSeconds + "/"
						+ tuning.joinStableSeconds;
			}
			long remaining = pauseUntilMs - now;
			return remaining > 0L ? (remaining / 1000L + 1L) + "s " + pauseReason : "none";
		}

		public boolean joinPaused() {
			return joinPaused;
		}

		public void reset() {
			tier = MAX_TIER;
			lowSeconds = 0;
			highSeconds = 0;
			stableSeconds = 0;
			joinStableSeconds = 0;
			lastChangeMs = 0L;
			lastDownMs = 0L;
			joinStartMs = -1L;
			joinPaused = false;
			lastSecondFrameMs = 0.0D;
			pauseUntilMs = 0L;
			pauseReason = "";
		}

		public void noteWorldJoin(long now) {
			reset();
			joinPaused = true;
			joinStartMs = now;
			pauseReason = "world join/dimension change";
		}

		public void pause(long now, long millis, String reason) {
			resetWindow();
			pauseUntilMs = now + Math.max(0L, millis);
			pauseReason = reason == null ? "" : reason;
		}

		public Decision tick(long now, double fps, double frameMs, boolean eligible) {
			if (!eligible) {
				resetWindow();
				return new Decision(Step.NONE, tier, "ineligible (screen/loading)");
			}
			if (frameMs <= 0.0D && fps > 0.0D) {
				frameMs = 1000.0D / fps;
			}
			if (fps <= 0.0D || frameMs <= 0.0D) {
				return new Decision(Step.NONE, tier, "no measurement");
			}
			if (joinPaused) {
				long joinedFor = now - joinStartMs;
				if (joinedFor < tuning.joinMinMs()) {
					lastSecondFrameMs = frameMs;
					return new Decision(Step.NONE, tier, "post-join pause");
				}
				if (isStable(frameMs, lastSecondFrameMs)) {
					joinStableSeconds++;
				} else {
					joinStableSeconds = 0;
				}
				lastSecondFrameMs = frameMs;
				if (joinStableSeconds >= tuning.joinStableSeconds() || joinedFor >= tuning.joinMaxMs()) {
					joinPaused = false;
					pauseUntilMs = now;
					pauseReason = "join settled";
					lastChangeMs = now;
					return new Decision(Step.NONE, tier, "post-join pause ended after " + (joinedFor / 1000L)
							+ " s (stable " + joinStableSeconds + " s)");
				}
				return new Decision(Step.NONE, tier, "post-join pause");
			}
			if (now < pauseUntilMs) {
				resetWindow();
				return new Decision(Step.NONE, tier, "paused: " + pauseReason);
			}

			boolean stableFrame = isStable(frameMs, lastSecondFrameMs);
			if (stableFrame) {
				stableSeconds++;
			} else {
				stableSeconds = 0;
			}
			lastSecondFrameMs = frameMs;

			double minFps = limits.minFps();
			double comfortFps = limits.comfortFps();
			boolean belowMinimum = fps < minFps && frameMs > limits.minFrameMs() * 1.02D;
			boolean atComfort = fps >= comfortFps && frameMs <= limits.comfortFrameMs();

			if (belowMinimum) {
				highSeconds = 0;
				lowSeconds++;
				if (tier > 0 && lowSeconds >= tuning.downSeconds() && now - lastChangeMs >= tuning.cooldownMs()) {
					int before = tier;
					tier--;
					lastChangeMs = now;
					lastDownMs = now;
					lowSeconds = 0;
					stableSeconds = 0;
					return new Decision(Step.DOWN, tier,
							"FPS " + one(fps) + " below minimum " + one(minFps) + " for "
									+ tuning.downSeconds() + " s (scale " + two(SCALES[before]) + " -> "
									+ two(SCALES[tier]) + ")");
				}
				return new Decision(Step.NONE, tier, "below minimum, collecting");
			}

			lowSeconds = 0;
			if (!atComfort) {
				highSeconds = 0;
				return new Decision(Step.NONE, tier, "between minimum and comfort; holding");
			}
			highSeconds++;
			if (tier >= maxTier) {
				return new Decision(Step.NONE, tier, "already at maximum quality");
			}
			boolean stableRecovery = stableSeconds >= tuning.stableUpSeconds()
					&& now - lastDownMs >= tuning.stableAfterDownMs();
			// Capture the cause before the counters are cleared for the step; reading
			// highSeconds after the reset always produced the stable branch's
			// explanation, even when the comfort path caused the step.
			boolean sustainedComfort = highSeconds >= tuning.upSeconds();
			if ((sustainedComfort || stableRecovery)
					&& now - lastChangeMs >= tuning.cooldownMs()) {
				int before = tier;
				tier++;
				lastChangeMs = now;
				highSeconds = 0;
				stableSeconds = 0;
				String reason = sustainedComfort
						? "FPS " + one(fps) + " at or above comfort " + one(comfortFps) + " for "
						+ tuning.upSeconds() + " s"
						: "frame time stable for " + tuning.stableUpSeconds()
						+ " s and last down-step older than " + (tuning.stableAfterDownMs() / 1000L)
						+ " s (FPS " + one(fps) + ")";
				return new Decision(Step.UP, tier, reason + " (scale " + two(SCALES[before]) + " -> "
						+ two(SCALES[tier]) + ")");
			}
			return new Decision(Step.NONE, tier, "above comfort, collecting");
		}

		private void resetWindow() {
			lowSeconds = 0;
			highSeconds = 0;
			stableSeconds = 0;
		}

		private boolean isStable(double frameMs, double previous) {
			if (previous <= 0.0D) {
				return false;
			}
			double tolerance = Math.max(1.0D, previous * 0.08D);
			return Math.abs(frameMs - previous) <= tolerance;
		}
	}

	private QualityPolicy() {
	}

	/** Minimum acceptable FPS for a tier; an explicit user value wins. */
	public static double minFps(int configured, HardwareTier tier) {
		if (configured > 0) {
			return configured;
		}
		if (tier == null) {
			return 45.0D;
		}
		return switch (tier) {
			case VERY_LOW, LOW -> 30.0D;
			case MID -> 45.0D;
			case HIGH -> 60.0D;
		};
	}

	/** Comfortable FPS: an explicit user value wins, otherwise 1.25x the minimum. */
	public static double comfortFps(int configured, double minFps) {
		if (configured > 0) {
			return Math.max(configured, minFps + 1.0D);
		}
		return Math.round(minFps * 1.25D * 10.0D) / 10.0D;
	}

	private static String one(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String two(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
