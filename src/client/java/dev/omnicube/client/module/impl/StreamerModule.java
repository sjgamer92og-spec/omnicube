package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/**
 * Best-effort local privacy mode. It is deliberately not presented as a
 * security boundary: other overlays, screenshots and mods can still expose
 * information.
 */
public final class StreamerModule extends Module {
	private static final Pattern IPV4 = Pattern.compile("(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?(?!\\d)");
	private static final Pattern COORDS = Pattern.compile("-?\\d{1,7}(?:\\.\\d+)?(?:[ ,]+-?\\d{1,7}(?:\\.\\d+)?){2}");

	private final Setting.Bool hideChat = add(new Setting.Bool("hide_chat", "Redact chat", true));
	private final Setting.Bool hideCoordinates = add(new Setting.Bool("hide_coordinates", "Hide coordinates", true));
	private final Setting.Bool hideServer = add(new Setting.Bool("hide_server", "Hide server address", true));

	public StreamerModule() {
		super("streamer_mode", "Streamer Mode", "Hides your coordinates, server address and addresses in chat locally.", ModuleCategory.MISC, false);
	}

	@Override
	protected void onEnable() {
		OmniCubeText.send("Streamer Mode on: redaction is local and best-effort.");
	}

	public boolean hideChat() {
		return isEnabled() && hideChat.get();
	}

	public boolean hideCoordinates() {
		return isEnabled() && hideCoordinates.get();
	}

	public boolean hideServer() {
		return isEnabled() && hideServer.get();
	}

	public Component redact(Component message) {
		if (message == null || !hideChat()) {
			return message;
		}
		String original = message.getString();
		String redacted = redactString(original);
		return redacted.equals(original) ? message : Component.literal(redacted);
	}

	public String redactString(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String result = text;
		try {
			result = IPV4.matcher(result).replaceAll("[address hidden]");
			result = COORDS.matcher(result).replaceAll("[coords hidden]");
			if (hideServer()) {
				String server = serverAddress();
				if (!server.isEmpty()) {
					result = result.replace(server, "[server hidden]");
				}
			}
		} catch (Throwable throwable) {
			// Redaction must never drop a chat message.
			Diagnostics.exception("streamer.redact", throwable);
		}
		return result;
	}

	public String serverAddress() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
				return client.getCurrentServer().ip;
			}
		} catch (Throwable throwable) {
			// Not connected or the server list is unavailable.
			Diagnostics.exception("streamer.server", throwable);
		}
		return "";
	}
}
