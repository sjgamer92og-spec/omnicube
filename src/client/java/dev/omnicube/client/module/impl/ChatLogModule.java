package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.OmniCubeClient;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatLogModule extends Module {
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_QUEUED_LINES = 256;
	private static final BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>(MAX_QUEUED_LINES);
	private static final AtomicBoolean DROP_WARNED = new AtomicBoolean();
	private static final AtomicBoolean WRITE_FAILED = new AtomicBoolean();

	static {
		Thread writer = new Thread(ChatLogModule::writeLoop, "OmniCube-ChatLog");
		writer.setDaemon(true);
		writer.start();
	}

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
		if (message == null || message.isEmpty() || WRITE_FAILED.get()) {
			return;
		}
		String line = (timestamps.get() ? "[" + LocalDateTime.now().format(TIMESTAMP) + "] " : "")
				+ "[" + channel + "] " + message.strip() + System.lineSeparator();
		if (!QUEUE.offer(line) && DROP_WARNED.compareAndSet(false, true)) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Chat log queue is full; dropping new lines until the writer catches up.");
		}
	}

	private static void writeLoop() {
		Path path;
		try {
			path = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("omnicube-chat.log");
		} catch (Throwable throwable) {
			WRITE_FAILED.set(true);
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not resolve the chat log path; chat logging is disabled.", throwable);
			return;
		}

		while (true) {
			String line;
			try {
				line = QUEUE.take();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			}

			try {
				Files.createDirectories(path.getParent());
				Files.writeString(path, line, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (Throwable throwable) {
				WRITE_FAILED.set(true);
				OmniCubeMod.LOGGER.warn("[OmniCube] Chat logging failed; disabling the chat log module.", throwable);
				ChatLogModule module = OmniCubeClient.chatLog();
				if (module != null) {
					module.setEnabled(false);
				}
				return;
			}
		}
	}

	public static OmniCubeConfig config() {
		return OmniCubeClient.config();
	}
}
