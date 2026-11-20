package dev.omnicube.client.feature;

import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;

import java.util.List;

/**
 * Anything the config screen can show and toggle. Both gameplay modules and HUD
 * elements implement this, which keeps the GUI simple and consistent.
 */
public interface Feature {
	String id();

	String name();

	String description();

	ModuleCategory category();

	boolean isEnabled();

	void setEnabled(boolean enabled);

	/**
	 * False when another mod already provides this feature. The config screen
	 * shows such features greyed out and refuses to enable them.
	 */
	default boolean isAvailable() {
		return true;
	}

	default String unavailableReason() {
		return "";
	}

	/**
	 * Specific, human readable explanation for the current state. It is shown as a
	 * tooltip in the GUI, printed by /omnicube diag and written to the diagnostics log.
	 */
	default String stateReason() {
		return "";
	}

	/**
	 * Marks the feature FAILED. Implementations disable it and remember why.
	 */
	default void markFailed(String reason) {
		setEnabled(false);
	}

	default List<Setting<?>> settings() {
		return List.of();
	}
}
