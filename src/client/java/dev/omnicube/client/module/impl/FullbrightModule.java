package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends Module {
	private final Setting.Num brightness = add(new Setting.Num("brightness", "Brightness", 5.0D, 1.0D, 16.0D, 0.5D));
	private Double originalGamma;

	public FullbrightModule() {
		super("fullbright", "Fullbright", "Lights up the world by raising the gamma option while enabled.", ModuleCategory.RENDER, false);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		if (originalGamma == null) {
			originalGamma = client.options.gamma().get();
		}
		apply(client);
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
		} catch (Throwable ignored) {
			// Options can be unavailable during very early startup; the next tick retries.
		}
	}

	public void restore() {
		if (originalGamma == null) {
			return;
		}
		try {
			Minecraft.getInstance().options.gamma().set(originalGamma);
		} catch (Throwable ignored) {
			// Nothing sensible to do if options are already torn down.
		} finally {
			originalGamma = null;
		}
	}
}
