package dev.omnicube.client.module.impl;

import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

/**
 * Smooth zoom. The field of view is adjusted for every rendered frame from
 * {@code GameRenderer#getFov} instead of being written to the FOV option, so the
 * transition eases in and out and the player's saved FOV is never touched.
 */
public final class ZoomModule extends Module {
	private final Setting.Num factor = add(new Setting.Num("factor", "Zoom factor", 4.0D, 2.0D, 16.0D, 0.5D));
	private final Setting.Bool smooth = add(new Setting.Bool("smooth", "Smooth animation", true));
	private final Setting.Num smoothing = add(new Setting.Num("smoothing", "Animation time", 0.12D, 0.02D, 0.50D, 0.02D));

	private boolean zooming;
	/** Interpolated divisor applied to the field of view; 1.0 means no zoom. */
	private double current = 1.0D;
	private long lastFrame = System.nanoTime();

	public ZoomModule() {
		super("zoom", "Zoom", "Hold the zoom key to narrow the field of view with a smooth animation.",
				ModuleCategory.RENDER, false);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		zooming = isEnabled() && client.player != null && client.screen == null
				&& OmniCubeKeybinds.zoom != null && OmniCubeKeybinds.zoom.isDown();
	}

	/**
	 * Called from the renderer once per frame.
	 *
	 * @return multiplier for the computed field of view (1.0 when not zooming)
	 */
	public float fovMultiplier() {
		if (!isEnabled()) {
			current = 1.0D;
			return 1.0F;
		}
		long now = System.nanoTime();
		double delta = Math.min(0.1D, Math.max(0.0D, (now - lastFrame) / 1_000_000_000.0D));
		lastFrame = now;

		double target = zooming ? factor.get() : 1.0D;
		if (smooth.get()) {
			double alpha = 1.0D - Math.exp(-delta / Math.max(0.02D, smoothing.get()));
			current += (target - current) * alpha;
		} else {
			current = target;
		}
		if (Math.abs(current - 1.0D) < 0.004D) {
			current = 1.0D;
		}
		if (current <= 1.0001D) {
			return 1.0F;
		}
		return (float) (1.0D / current);
	}

	/** Kept for callers that used to restore the FOV option; zoom no longer edits it. */
	public void restore() {
		zooming = false;
		current = 1.0D;
	}

	@Override
	protected void onDisable() {
		restore();
	}
}
