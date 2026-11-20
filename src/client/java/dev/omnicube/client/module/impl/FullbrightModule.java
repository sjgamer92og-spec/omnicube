package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends Module {
	// The lightmap uses gamma without clamping it to the slider range, so a high
	// value lifts night and cave darkness instead of only softening it.
	private final Setting.Num brightness = add(new Setting.Num("brightness", "Brightness", 16.0D, 1.0D, 16.0D, 0.5D));
	private Double originalGamma;

	public FullbrightModule() {
		super("fullbright", "Fullbright", "Removes night and cave darkness by raising the gamma option while enabled.", ModuleCategory.RENDER, false);
	}

	@Override
	protected void onEnable() {
		// Enabling happens while the config is read, which can be before the client is
		// fully constructed; never let that turn into a startup failure.
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null) {
				return;
			}
			if (originalGamma == null) {
				originalGamma = client.options.gamma().get();
			}
			apply(client);
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.enable", throwable);
		}
	}

	@Override
	protected void onDisable() {
		restore();
	}

	@Override
	public void tick() {
		apply(Minecraft.getInstance());
	}

	private void apply(Minecraft client) {
		try {
			double target = brightness.get();
			if (Math.abs(client.options.gamma().get() - target) > 0.001D) {
				client.options.gamma().set(target);
			}
		} catch (Throwable throwable) {
			// Options can be unavailable during very early startup; the next tick retries.
			Diagnostics.exception("fullbright.apply", throwable);
		}
	}

	public void restore() {
		if (originalGamma == null) {
			return;
		}
		try {
			Minecraft.getInstance().options.gamma().set(originalGamma);
		} catch (Throwable throwable) {
			// Nothing sensible to do if options are already torn down.
			Diagnostics.exception("fullbright.restore", throwable);
		} finally {
			originalGamma = null;
		}
	}
}
