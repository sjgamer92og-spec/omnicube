package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class AutoReconnectModule extends Module {
	private final Setting.Num delay = add(new Setting.Num("delay", "Delay", 5.0D, 1.0D, 30.0D, 1.0D));

	private boolean pending;
	private int ticks;
	private ServerData server;
	private String address = "";
	private boolean announced;
	private boolean multiplayerSession;

	public AutoReconnectModule() {
		super("auto_reconnect", "Auto Reconnect", "Retries the last multiplayer server after an unexpected disconnect.", ModuleCategory.MISC, false);
	}

	public void rememberServer(Minecraft client) {
		try {
			if (client == null) {
				return;
			}
			if (client.hasSingleplayerServer() || client.getCurrentServer() == null) {
				// Never carry a multiplayer target into a singleplayer session.
				server = null;
				address = "";
				multiplayerSession = false;
				return;
			}
			if (client.getCurrentServer().ip != null && !client.getCurrentServer().ip.isBlank()) {
				server = client.getCurrentServer();
				address = server.ip;
				multiplayerSession = true;
			}
		} catch (Throwable ignored) {
			// The next join will try again.
		}
	}

	public void onDisconnect(Minecraft client) {
					try {
			if (!isEnabled() || client == null || !multiplayerSession) {
				return;
			}
			if (client.hasSingleplayerServer()) {
				return;
			}
			if (client.screen instanceof TitleScreen) {
				// The player chose "Disconnect"; do not drag them back in.
				return;
			}
			if (client.getCurrentServer() != null) {
				server = client.getCurrentServer();
				address = server.ip == null ? "" : server.ip;
			} else if (server != null && server.ip != null) {
				address = server.ip;
			}
			if (server == null || address.isBlank()) {
				return;
			}
			if (address.isBlank() || !ServerAddress.isValidAddress(address)) {
				return;
			}
			ticks = Math.max(1, delay.intValue() * 20);
			pending = true;
			announced = false;
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Auto Reconnect could not remember the server.", throwable);
			reset();
		}
	}

	public void tick(Minecraft client) {
		if (!pending || client == null) {
			return;
		}
		try {
			if (!isEnabled()) {
				reset();
				return;
			}
			if (client.player != null || client.level != null) {
				reset();
				return;
			}
			if (client.screen instanceof TitleScreen) {
				reset();
				return;
			}
			if (client.screen != null && !(client.screen instanceof DisconnectedScreen)) {
				// The player navigated to the server list/options; respect that.
				reset();
				return;
			}
			if (ticks > 0 && --ticks > 0) {
				if (!announced && client.screen instanceof DisconnectedScreen) {
					announced = true;
					OmniCubeText.send("Auto Reconnect: retrying " + address + " in " + Math.max(1, ticks / 20) + "s.");
				}
				return;
			}
			pending = false;
			ServerData target = server;
			String targetAddress = address;
			if (target == null || targetAddress.isBlank()) {
				return;
			}
			OmniCubeText.send("Auto Reconnect: connecting to " + targetAddress + "...");
			ConnectScreen.startConnecting(null, client, ServerAddress.parseString(targetAddress), target, false, null);
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Auto Reconnect failed; staying on the disconnect screen.", throwable);
			reset();
		}
	}

	public void reset() {
		pending = false;
		ticks = 0;
		server = null;
		address = "";
		announced = false;
		multiplayerSession = false;
	}

	@Override
	protected void onDisable() {
		reset();
	}
}
