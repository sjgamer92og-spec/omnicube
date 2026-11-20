package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;

public final class HudModule extends Module {
	private final Setting.Bool background = add(new Setting.Bool("background", "Element background", true));

	public HudModule() {
		super("hud", "HUD Overlay", "Master switch for OmniCube's HUD elements and their background panels.", ModuleCategory.HUD, true);
	}

	public HudModule(boolean defaultEnabled) {
		super("hud", "HUD Overlay", "Master switch for OmniCube's HUD elements and their background panels.", ModuleCategory.HUD, defaultEnabled);
	}

	@Override
	protected void onEnable() {
		HudElement.masterEnabled = true;
		HudElement.background = background.get();
		Diagnostics.event("hud", "master switch ON");
	}

	@Override
	protected void onDisable() {
		// Hiding is handled by HudManager; the flag explains why elements are hidden.
		HudElement.masterEnabled = false;
		Diagnostics.event("hud", "master switch OFF -> every HUD element is hidden");
	}

	@Override
	public void tick() {
		HudElement.masterEnabled = isEnabled();
		HudElement.background = background.get();
	}

	public boolean backgroundEnabled() {
		return background.get();
	}
}
