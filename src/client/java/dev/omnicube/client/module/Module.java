package dev.omnicube.client.module;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
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
	private String unavailableReason = "";
	private String reason = "";
	private boolean autoDisabled;

	protected Module(String id, String name, String description, ModuleCategory category, boolean defaultEnabled) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.defaultEnabled = defaultEnabled;
		this.reason = defaultEnabled ? "Enabled (default)" : "Disabled by user (default)";
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
	public boolean isAvailable() {
		return unavailableReason.isEmpty();
	}

	@Override
	public String unavailableReason() {
		return unavailableReason;
	}

	@Override
	public String stateReason() {
		if (!isAvailable()) {
			return unavailableReason;
		}
		return reason;
	}

	/** True when the state was changed by OmniCube (compat, low-end, FPS safety, failure). */
	public boolean autoDisabled() {
		return autoDisabled;
	}

	public void blockForCompatibility(String reason) {
		boolean wasEnabled = this.enabled;
		this.unavailableReason = reason == null ? "" : reason;
		this.reason = this.unavailableReason;
		this.autoDisabled = true;
		this.enabled = false;
		if (wasEnabled) {
			// A module that was already running owns live state or changed options;
			// blocking it must run the same cleanup as a normal disable.
			try {
				onDisable();
			} catch (Throwable throwable) {
				Diagnostics.exception(id + ".compat_cleanup", throwable);
				OmniCubeMod.LOGGER.warn("[OmniCube] Module '{}' failed during compatibility cleanup.", id, throwable);
			}
		}
		recordState();
	}

	@Override
	public void setEnabled(boolean newState) {
		applyState(newState, newState ? "Enabled" : "Disabled by user", false);
	}

	/** Changes state with an explicit reason and keeps it out of the saved config. */
	public void setEnabledAutomatically(boolean newState, String why) {
		applyState(newState, why == null || why.isBlank() ? "Disabled by OmniCube" : why, true);
	}

	/** Changes state with an explicit reason and saves it like a user choice. */
	public void setEnabledWithReason(boolean newState, String why) {
		applyState(newState, why == null || why.isBlank() ? "Changed by OmniCube" : why, false);
	}

	@Override
	public void markFailed(String reason) {
		boolean wasEnabled = this.enabled;
		this.enabled = false;
		this.autoDisabled = true;
		this.reason = reason == null || reason.isBlank() ? "Failed (see stack trace)" : reason;
		if (wasEnabled) {
			// A module that failed mid-session still owns whatever it changed (video
			// options, captured state), so run its cleanup exactly like a normal disable.
			try {
				onDisable();
			} catch (Throwable throwable) {
				Diagnostics.exception(id + ".cleanup", throwable);
				OmniCubeMod.LOGGER.warn("[OmniCube] Module '{}' also failed during cleanup.", id, throwable);
			}
		}
		recordState();
	}

	private void applyState(boolean newState, String why, boolean automatic) {
		if (newState && !isAvailable()) {
			this.reason = unavailableReason;
			recordState();
			return;
		}
		boolean changed = newState != enabled;
		this.enabled = newState;
		this.reason = why;
		this.autoDisabled = automatic;
		if (changed) {
			try {
				if (newState) {
					onEnable();
				} else {
					onDisable();
				}
			} catch (Throwable throwable) {
				this.enabled = false;
				this.autoDisabled = true;
				this.reason = "Failed: " + Diagnostics.describe(throwable) + " in "
						+ (newState ? "onEnable()" : "onDisable()") + " (see stack trace)";
				Diagnostics.exception(id, throwable);
				OmniCubeMod.LOGGER.error("[OmniCube] Module '{}' failed while being {}; it has been disabled.", id,
						newState ? "enabled" : "disabled", throwable);
				try {
					onDisable();
				} catch (Throwable suppress) {
					Diagnostics.exception(id + ".cleanup", suppress);
					OmniCubeMod.LOGGER.warn("[OmniCube] Module '{}' also failed during cleanup.", id, suppress);
				}
			}
		}
		recordState();
		Diagnostics.event("feature", id + " -> " + (enabled ? "ENABLED" : "DISABLED") + " (" + this.reason + ")");
	}

	/** Writes the current state into the diagnostics feature table without spamming an event. */
	public void recordState() {
		Diagnostics.status(id, name, "module", state(), stateReason());
	}

	private String state() {
		if (!isAvailable()) {
			return "BLOCKED";
		}
		if (reason != null && reason.startsWith("Failed")) {
			return "FAILED";
		}
		return enabled ? "ENABLED" : "DISABLED";
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
