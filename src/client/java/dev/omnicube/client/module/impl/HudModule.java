package dev.omnicube.client.module.impl;

import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;

public final class HudModule extends Module {
	private final Setting.Bool background = add(new Setting.Bool("background", "Element background", true));

	public HudModule() {
		super("hud", "HUD Overlay", "Master switch for OmniCube's HUD elements and their background panels.", ModuleCategory.HUD, true);
	}

	@Override
	protected void onEnable() {
		HudElement.background = background.get();
	}

	@Override
	protected void onDisable() {
		// Hiding is handled by HudManager; nothing to restore here.
	}

	@Override
	public void tick() {
		HudElement.background = background.get();
	}

	public boolean backgroundEnabled() {
		return background.get();
	}
}
