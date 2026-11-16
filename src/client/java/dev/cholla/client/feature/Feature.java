

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

	default List<Setting<?>> settings() {
		return List.of();
	}
}
