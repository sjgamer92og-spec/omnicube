package dev.omnicube.client.module.impl;

import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

public final class ZoomModule extends Module {
	private final Setting.Num factor = add(new Setting.Num("factor", "Zoom factor", 4.0D, 2.0D, 12.0D, 0.5D));
	private Integer originalFov;

	public ZoomModule() {
		super("zoom", "Zoom", "Hold the zoom key to narrow the field of view. Restores your FOV on release.", ModuleCategory.RENDER, false);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client.options == null) {
			return;
		}

		boolean held = OmniCubeKeybinds.zoom != null && OmniCubeKeybinds.zoom.isDown() && client.screen == null;
		if (held) {
			if (originalFov == null) {
				originalFov = client.options.fov().get();
			}
			int target = Math.max(1, (int) Math.round(originalFov / factor.get()));
			if (client.options.fov().get() != target) {
				client.options.fov().set(target);
			}
		} else {
			restore();
		}
	}

	@Override
	protected void onDisable() {
		restore();
	}

	public void restore() {
		if (originalFov == null) {
			return;
		}
		try {
			Minecraft.getInstance().options.fov().set(originalFov);
		} catch (Throwable ignored) {
			// Safe to ignore; options are still the vanilla defaults at worst.
		} finally {
			originalFov = null;
		}
	}
}
