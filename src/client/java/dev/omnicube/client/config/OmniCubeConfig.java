package dev.omnicube.client.config;

import com.google.gson.JsonObject;
import dev.omnicube.client.macro.Macro;
import dev.omnicube.client.waypoint.Waypoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Root of config/omnicube.json. Module settings live in their own JSON object so
 * adding a feature never invalidates an existing config file.
 */
public final class OmniCubeConfig {
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
		for (Macro macro : macros) {
			macro.sanitize();
		}
		for (Waypoint waypoint : waypoints) {
			waypoint.sanitize();
		}
	}

	/** Parses "#RRGGBB" into an opaque ARGB int, falling back to OmniCube green. */
	public int accentArgb() {
		try {
			return 0xFF000000 | Integer.parseInt(accent.substring(1), 16);
		} catch (RuntimeException ignored) {
			return 0xFF7ED957;
		}
	}
}
