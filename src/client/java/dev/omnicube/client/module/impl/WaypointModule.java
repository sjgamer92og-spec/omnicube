package dev.omnicube.client.module.impl;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import dev.omnicube.client.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class WaypointModule extends Module {
	private final Setting.Num hudRange = add(new Setting.Num("hud_range", "HUD range (blocks)", 8192.0D, 256.0D, 65536.0D, 256.0D));
	private final Setting.Bool deathMarkers = add(new Setting.Bool("death_markers", "Death markers", true));
	private boolean deathRecorded;

	public WaypointModule() {
		super("waypoints", "Waypoints", "Save coordinates, show a compass to the active waypoint and mark where you died.", ModuleCategory.WORLD, true);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			deathRecorded = false;
			return;
		}

		boolean dead = player.getHealth() <= 0.0F;
		if (dead && !deathRecorded && deathMarkers.get()) {
			deathRecorded = true;
			add("Death", player.getX(), player.getY(), player.getZ());
			OmniCubeText.send("Death waypoint saved in " + dimensionName(client));
		} else if (!dead) {
			deathRecorded = false;
		}
	}

	public String dimensionName(Minecraft client) {
		try {
			return client.level.dimension().identifier().getPath();
		} catch (Throwable throwable) {
			Diagnostics.exception("waypoints.dimension", throwable);
			return "unknown";
		}
	}

	public String currentDimension() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? "" : dimensionName(client);
	}

	public Waypoint add(String name, double x, double y, double z) {
		return add(name, x, y, z, currentDimension());
	}

	public Waypoint add(String name, double x, double y, double z, String dimension) {
		String cleanName = name == null || name.isBlank() ? "Waypoint" : name.strip();
		Waypoint waypoint = new Waypoint(cleanName, x, y, z, dimension == null ? "" : dimension,
				OmniCubeClient.config().accentArgb());
		OmniCubeClient.config().waypoints.add(waypoint);
		OmniCubeClient.saveConfig();
		return waypoint;
	}

	public boolean remove(String name) {
		boolean removed = OmniCubeClient.config().waypoints.removeIf(waypoint ->
				waypoint.name != null && waypoint.name.equalsIgnoreCase(name));
		if (removed) {
			OmniCubeClient.saveConfig();
		}
		return removed;
	}

	public void clear() {
		OmniCubeClient.config().waypoints.clear();
		OmniCubeClient.config().activeWaypoint = "";
		OmniCubeClient.saveConfig();
	}

	public List<Waypoint> all() {
		return OmniCubeClient.config().waypoints;
	}

	public Optional<Waypoint> active() {
		String configured = OmniCubeClient.config().activeWaypoint;
		List<Waypoint> waypoints = OmniCubeClient.config().waypoints;
		if (configured != null && !configured.isBlank()) {
			String dimension = currentDimension();
			for (Waypoint waypoint : waypoints) {
				if (waypoint.name != null && waypoint.name.equalsIgnoreCase(configured)) {
					// Never point at a waypoint that lives in a different dimension.
					if (waypoint.dimension == null || waypoint.dimension.isEmpty() || waypoint.dimension.equals(dimension)) {
						return Optional.of(waypoint);
					}
					break;
				}
			}
		}
		return nearest();
	}

	public Optional<Waypoint> nearest() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return Optional.empty();
		}
		String dimension = currentDimension();
		double maxRange = hudRange.get();
		Waypoint best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Waypoint waypoint : OmniCubeClient.config().waypoints) {
			if (waypoint.dimension != null && !waypoint.dimension.isEmpty() && !waypoint.dimension.equals(dimension)) {
				continue;
			}
			double distance = player.distanceToSqr(waypoint.x, waypoint.y, waypoint.z);
			if (distance <= maxRange * maxRange && distance < bestDistance) {
				bestDistance = distance;
				best = waypoint;
			}
		}
		return Optional.ofNullable(best);
	}

	public String describe(Waypoint waypoint) {
		return String.format(Locale.ROOT, "%s (%.0f, %.0f, %.0f)", waypoint.name, waypoint.x, waypoint.y, waypoint.z);
	}

	public BlockPos playerBlockPos() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? BlockPos.ZERO : player.blockPosition();
	}
}
