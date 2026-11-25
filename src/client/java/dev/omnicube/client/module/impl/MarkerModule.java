package dev.omnicube.client.module.impl;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.render.WorldTextRenderer;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import dev.omnicube.client.waypoint.Waypoint;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MarkerModule extends Module {
	private final Setting.Bool tntMarkers = add(new Setting.Bool("tnt_markers", "TNT fuse markers", true));
	private final Setting.Bool waypointBeacon = add(new Setting.Bool("waypoint_beacon", "Waypoint beacon", true));
	private final Setting.Num maxDistance = add(new Setting.Num("max_distance", "Maximum distance", 128.0D, 16.0D, 512.0D, 16.0D));

	private volatile List<TntMarker> tntSnapshot = List.of();
	private int scanTicks;

	public MarkerModule() {
		super("world_markers", "World Markers", "Draws TNT fuse labels and a beacon at your active waypoint in the world.", ModuleCategory.WORLD, false);
	}

	@Override
	protected void onDisable() {
		// Start a fresh scan on the next enable instead of waiting out the old half second.
		scanTicks = 0;
		tntSnapshot = List.of();
	}

	@Override
	public void tick() {
		if (!isEnabled()) {
			tntSnapshot = List.of();
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			tntSnapshot = List.of();
			return;
		}
		if (++scanTicks < 10) {
			return;
		}
		scanTicks = 0;
		List<TntMarker> found = new ArrayList<>();
		double max = maxDistance.get();
		long now = System.currentTimeMillis();
		for (var entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof PrimedTnt tnt)) {
				continue;
			}
			double distance = tnt.distanceToSqr(player.getX(), player.getY(), player.getZ());
			if (distance <= max * max) {
				found.add(new TntMarker(tnt.getX(), tnt.getY(), tnt.getZ(), tnt.getFuse(), distance, now));
			}
		}
		found.sort(Comparator.comparingDouble(TntMarker::distance));
		if (found.size() > 24) {
			found = new ArrayList<>(found.subList(0, 24));
		}
		tntSnapshot = List.copyOf(found);
	}

	public void render(WorldRenderContext context) {
		if (!isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null || client.options.hideGui || client.screen != null) {
			return;
		}
		Vec3 camera = context.worldState().cameraRenderState.pos;

		if (tntMarkers.get()) {
			for (TntMarker marker : tntSnapshot) {
				try {
					Vec3 position = new Vec3(marker.x(), marker.y() + 1.0D, marker.z());
					if (position.distanceTo(camera) > maxDistance.get()) {
						continue;
					}
					// The snapshot is up to half a second old, so subtract the elapsed ticks;
					// otherwise the displayed fuse froze and then jumped backwards.
					long elapsedTicks = (System.currentTimeMillis() - marker.scannedAt()) / 50L;
					double seconds = Math.max(0.0D, marker.fuse() - elapsedTicks) / 20.0D;
					WorldTextRenderer.beam(context, new Vec3(marker.x(), marker.y(), marker.z()), 0xFFFF5533, 2.0D);
					WorldTextRenderer.draw(context, position,
							Component.literal(String.format(java.util.Locale.ROOT, "TNT %.1fs", seconds))
									.withStyle(ChatFormatting.RED),
							0xFFFFFFFF, 1.0F);
				} catch (Throwable throwable) {
					// Keep rendering the remaining markers.
					Diagnostics.exception("world_markers.tnt", throwable);
				}
			}
		}

		if (waypointBeacon.get()) {
			try {
				var waypointModule = OmniCubeClient.waypoints();
				if (waypointModule == null || !waypointModule.isEnabled()) {
					return;
				}
				Optional<Waypoint> active = waypointModule.active();
				if (active.isEmpty()) {
					return;
				}
				Waypoint waypoint = active.get();
				String dimension = client.level.dimension().identifier().getPath();
				if (waypoint.dimension != null && !waypoint.dimension.isEmpty() && !waypoint.dimension.equals(dimension)) {
					return;
				}
				Vec3 position = new Vec3(waypoint.x, waypoint.y + 2.0D, waypoint.z);
				if (position.distanceTo(camera) > maxDistance.get()) {
					return;
				}
				WorldTextRenderer.beam(context, new Vec3(waypoint.x, waypoint.y, waypoint.z), 0xFF7ED957, 4.0D);
				WorldTextRenderer.draw(context, position,
						Component.literal(waypoint.name).withStyle(ChatFormatting.GREEN),
						0xFFFFFFFF, 1.2F);
			} catch (Throwable throwable) {
				// A bad waypoint should not break the world render pass.
				Diagnostics.exception("world_markers.waypoint", throwable);
			}
		}
	}

	private record TntMarker(double x, double y, double z, int fuse, double distance, long scannedAt) {
	}
}
