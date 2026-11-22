package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;

/**
 * Toggle for the Profiles screen. The profiles themselves are files, so this module
 * has no per-tick work; turning it off simply hides the entry point.
 */
public final class SettingsProfilesModule extends Module {
	public SettingsProfilesModule() {
		super("settings_profiles", "Settings Profiles",
				"Save, load, delete and rename named setting profiles, and apply the built-in "
						+ "Default, Low-end, Streaming and PvP profiles. Open it with the Profiles button.",
				ModuleCategory.MISC, true);
	}

	@Override
	protected void onEnable() {
		publishStatus();
	}

	@Override
	protected void onDisable() {
		publishStatus();
	}

	public String statusLine() {
		return "Settings profiles: " + (isEnabled() ? "ON" : "OFF");
	}

	private void publishStatus() {
		try {
			Diagnostics.note("settings_profiles", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("settings_profiles.status", throwable);
		}
	}
}
