package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ChatModule extends Module {
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm");

	private final Setting.Bool timestamps = add(new Setting.Bool("timestamps", "Show timestamps", true));
	private volatile String lastMessage = "";

	public ChatModule() {
		super("better_chat", "Better Chat", "Adds timestamps to chat and lets you copy the last chat message.", ModuleCategory.CHAT, true);
	}

	public boolean timestampEnabled() {
		return isEnabled() && timestamps.get();
	}

	public void remember(Component message) {
		if (isEnabled() && message != null) {
			lastMessage = message.getString();
		}
	}

	public MutableComponent decorate(Component message) {
		String prefix = "[" + LocalTime.now().format(TIMESTAMP) + "] ";
		return Component.literal(prefix).withStyle(ChatFormatting.DARK_GRAY).append(message.copy());
	}

	public void copyLast() {
		if (!isEnabled()) {
			return;
		}
		String message = lastMessage;
		if (message == null || message.isBlank()) {
			OmniCubeText.sendError("No chat message has been received yet.");
			return;
		}
		OmniCubeText.copyToClipboard(message);
		OmniCubeText.send("Copied the last chat message.");
	}
}
