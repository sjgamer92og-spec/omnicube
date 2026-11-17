package dev.omnicube.client.module;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base module. Lifecycle callbacks are wrapped so a failure in one feature can
 * never take down the client or another mod's tick handler.
 */
public abstract class Module implements Feature {
	private final String id;
	private final String name;
	private final String description;
	private final ModuleCategory category;
	private final boolean defaultEnabled;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;

	protected Module(String id, String name, String description, ModuleCategory category, boolean defaultEnabled) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.defaultEnabled = defaultEnabled;
	}

	protected <T extends Setting<?>> T add(T setting) {
		this.settings.add(setting);
		return setting;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public ModuleCategory category() {
		return category;
	}

	@Override
	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public boolean defaultEnabled() {
		return defaultEnabled;
	}

	@Override
	public void setEnabled(boolean newState) {
		if (newState == enabled) {
			return;
		}

		this.enabled = newState;
		try {
			if (newState) {
				onEnable();
			} else {
				onDisable();
			}
		} catch (Throwable throwable) {
			this.enabled = false;
			OmniCubeMod.LOGGER.error("[OmniCube] Module '{}' failed while being {}; it has been disabled.", id,
					newState ? "enabled" : "disabled", throwable);
			try {
				onDisable();
			} catch (Throwable suppress) {
				OmniCubeMod.LOGGER.warn("[OmniCube] Module '{}' also failed during cleanup.", id, suppress);
			}
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/**
	 * Called once per client tick while enabled. Implementations should be cheap
	 * and must not assume a world or player is present.
	 */
	public void tick() {
	}
}
