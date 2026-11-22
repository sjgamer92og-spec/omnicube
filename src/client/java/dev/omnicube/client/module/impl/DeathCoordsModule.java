package dev.omnicube.client.module.impl;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Locale;

/**
 * Announces where you died and keeps the last few death positions.
 *
 * <p>Detection is one health comparison per client tick, which is free compared to
 * anything that scans the world. The list is capped and stored in the config.
 */
public final class DeathCoordsModule extends Module {
	public static final int MAX_DEATHS = 5;

	private final Setting.Bool chatMessage = add(new Setting.Bool("chat_message", "Show in chat", true));
	private boolean wasDead;
	private int lastReportedHealth = -1;

	public DeathCoordsModule() {
		super("death_coords", "Death Coordinates",
				"Shows your coordinates in chat when you die and keeps the last five death positions "
						+ "for /omnicube deaths.",
				ModuleCategory.WORLD, true);
	}

	@Override
	protected void onEnable() {
		wasDead = false;
		publishStatus();
	}

	@Override
	protected void onDisable() {
		publishStatus();
	}

	@Override
	public void tick() {
		try {
			Minecraft client = Minecraft.getInstance();
			LocalPlayer player = client.player;
			if (player == null) {
				wasDead = false;
				lastReportedHealth = -1;
				return;
			}
			// The death screen is checked as well: a paused single-player death screen
			// still shows it before the health value is reset on respawn.
			boolean dead = player.isDeadOrDying() || player.getHealth() <= 0.0F
					|| client.screen instanceof DeathScreen;
			if (dead && !wasDead) {
				wasDead = true;
				record(player, client);
			} else if (!dead) {
				wasDead = false;
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("death_coords.tick", throwable);
		}
	}

	private void record(LocalPlayer player, Minecraft client) {
		String dimension = "?";
		try {
			dimension = client.level == null ? "?" : client.level.dimension().identifier().getPath();
		} catch (Throwable throwable) {
			Diagnostics.exception("death_coords.dimension", throwable);
		}
		String entry = String.format(Locale.ROOT, "%.1f %.1f %.1f (%s)",
				player.getX(), player.getY(), player.getZ(), dimension);
		List<String> deaths = OmniCubeClient.config().deaths;
		deaths.add(entry);
		while (deaths.size() > MAX_DEATHS) {
			deaths.removeFirst();
		}
		if (chatMessage.get()) {
			OmniCubeText.sendError("You died at " + entry + ".");
		}
		Diagnostics.event("death_coords", "died at " + entry);
		Diagnostics.note("death_coords", statusLine());
		OmniCubeClient.saveConfig();
	}

	public List<String> deaths() {
		return List.copyOf(OmniCubeClient.config().deaths);
	}

	public String statusLine() {
		return "Death coordinates: " + (isEnabled() ? "ON" : "OFF") + " | "
				+ OmniCubeClient.config().deaths.size() + "/" + MAX_DEATHS + " remembered";
	}

	private void publishStatus() {
		try {
			Diagnostics.note("death_coords", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("death_coords.status", throwable);
		}
	}
}
