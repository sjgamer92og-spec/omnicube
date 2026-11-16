
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * adding a feature never invalidates an existing config file.
 */
	public static final int CURRENT_VERSION = 1;

	public int version = CURRENT_VERSION;
	public String accent = "#7ED957";
	public boolean hudBackground = true;
	public boolean chatLog = false;
	public boolean chatLogTimestamps = true;
	public boolean copyCoordsIncludesDimension = true;
	public String activeWaypoint = "";
	public boolean deathWaypoint = true;

	public JsonObject modules = new JsonObject();
	public JsonObject hud = new JsonObject();
	public List<Macro> macros = new ArrayList<>();
	public List<Waypoint> waypoints = new ArrayList<>();

	public void sanitize() {
		if (version < 1) {
			version = CURRENT_VERSION;
		}
		if (accent == null || !accent.matches("#[0-9a-fA-F]{6}")) {
			accent = "#7ED957";
		}
		if (modules == null) {
			modules = new JsonObject();
		}
		if (hud == null) {
			hud = new JsonObject();
		}
		if (macros == null) {
			macros = new ArrayList<>();
		}
		if (waypoints == null) {
			waypoints = new ArrayList<>();
		}
		if (activeWaypoint == null) {
			activeWaypoint = "";
		}
		macros.removeIf(java.util.Objects::isNull);
		waypoints.removeIf(java.util.Objects::isNull);
	}

	public int accentArgb() {
		try {
			return 0xFF000000 | Integer.parseInt(accent.substring(1), 16);
		} catch (RuntimeException ignored) {
			return 0xFF7ED957;
		}
	}
}
