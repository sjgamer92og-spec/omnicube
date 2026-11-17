package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.config.OmniCubeConfig;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ChatLogModule extends Module {
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Object LOCK = new Object();

	private final Setting.Bool logChat = add(new Setting.Bool("log_chat", "Log player chat", true));
	private final Setting.Bool logSystem = add(new Setting.Bool("log_system", "Log server messages", true));
	private final Setting.Bool timestamps = add(new Setting.Bool("timestamps", "Timestamps in log file", true));

	public ChatLogModule() {
		super("chat_log", "Chat Log", "Saves received chat and server messages to logs/omnicube-chat.log.", ModuleCategory.CHAT, false);
	}

	public void recordChat(String text) {
		if (isEnabled() && logChat.get()) {
			append("CHAT", text);
		}
	}

	public void recordSystem(String text) {
		if (isEnabled() && logSystem.get()) {
			append("SYSTEM", text);
		}
	}

	private void append(String channel, String message) {
		if (message == null || message.isEmpty()) {
			return;
		}
		String line = (timestamps.get() ? "[" + LocalDateTime.now().format(TIMESTAMP) + "] " : "")
				+ "[" + channel + "] " + message.strip() + System.lineSeparator();
		Path path = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("omnicube-chat.log");

		synchronized (LOCK) {
			try {
				Files.createDirectories(path.getParent());
				Files.writeString(path, line, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (Exception exception) {
				// Disable logging rather than spamming the log every chat message.
				OmniCubeMod.LOGGER.warn("[OmniCube] Chat logging failed; disabling the chat log module.", exception);
				setEnabled(false);
			}
		}
	}

	public static OmniCubeConfig config() {
		return dev.omnicube.client.OmniCubeClient.config();
	}
}
