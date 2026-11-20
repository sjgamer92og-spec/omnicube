package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;

/**
 * Replaces the Mojang loading screen with OmniCube's own branded splash while
 * the game starts. Everything is drawn with plain rectangles and text, so it
 * needs no extra textures and costs nothing once the world is loaded.
 */
public final class SplashModule extends Module {
	private final Setting.Bool progress = add(new Setting.Bool("progress", "Show loading progress", true));
	private final Setting.Bool tips = add(new Setting.Bool("tips", "Show key hints", true));

	public SplashModule() {
		super("splash", "Custom Splash",
				"Draws an OmniCube loading screen instead of the Mojang studios logo while the game starts.",
				ModuleCategory.RENDER, true);
	}

	public boolean showProgress() {
		return progress.get();
	}

	public boolean showTips() {
		return tips.get();
	}
}
