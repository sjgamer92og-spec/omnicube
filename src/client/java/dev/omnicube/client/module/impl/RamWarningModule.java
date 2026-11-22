package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;

/**
 * Warns in chat when the Java heap stays near its maximum.
 *
 * <p>The heap is sampled once per second and a warning is shown at most once a
 * minute, so the check can never spam the chat or cost a frame.
 */
public final class RamWarningModule extends Module {
	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final long WARNING_COOLDOWN_MILLIS = 60_000L;

	private final Setting.Num threshold = add(new Setting.Num("threshold", "Warn above (%)", 85.0D, 50.0D, 97.0D, 5.0D));
	private int tickCounter;
	private long lastWarning;
	private int lastPercent = -1;

	public RamWarningModule() {
		super("ram_warning", "RAM Warning",
				"Shows one chat warning when Java heap use stays above the chosen percentage. "
						+ "Checked about once a second, shown at most once a minute.",
				ModuleCategory.PERFORMANCE, false);
	}

	@Override
	protected void onEnable() {
		tickCounter = 0;
		lastWarning = 0L;
		lastPercent = -1;
		publishStatus();
	}

	@Override
	protected void onDisable() {
		publishStatus();
	}

	@Override
	public void tick() {
		if (++tickCounter < CHECK_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;
		try {
			Runtime runtime = Runtime.getRuntime();
			long max = runtime.maxMemory();
			if (max <= 0L) {
				return;
			}
			long used = runtime.totalMemory() - runtime.freeMemory();
			int percent = (int) Math.round(used * 100.0D / max);
			if (percent != lastPercent) {
				lastPercent = percent;
				Diagnostics.note("ram_warning", statusLine());
			}
			long now = System.currentTimeMillis();
			if (percent >= threshold.intValue() && now - lastWarning >= WARNING_COOLDOWN_MILLIS) {
				lastWarning = now;
				OmniCubeText.sendError("RAM warning: " + percent + "% of the " + (max / (1024L * 1024L))
						+ " MB heap is in use. Close other apps or restart Minecraft if it stutters.");
				Diagnostics.event("ram_warning", "warned at " + percent + "% (" + used / (1024L * 1024L) + " MB used)");
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("ram_warning.tick", throwable);
		}
	}

	public String statusLine() {
		return "RAM Warning: " + (isEnabled() ? "ON" : "OFF") + " | warns at " + threshold.intValue()
				+ "% | heap " + (lastPercent < 0 ? "?" : lastPercent) + "%";
	}

	private void publishStatus() {
		try {
			Diagnostics.note("ram_warning", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("ram_warning.status", throwable);
		}
	}
}
