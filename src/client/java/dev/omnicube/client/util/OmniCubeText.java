package dev.omnicube.client.util;

import dev.omnicube.client.diag.Diagnostics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class OmniCubeText {
	private OmniCubeText() {
	}

	public static Component prefix() {
		return Component.literal("[OmniCube] ").withStyle(ChatFormatting.GREEN);
	}

	public static void send(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		client.player.displayClientMessage(prefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY)), false);
	}

	public static void sendError(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		client.player.displayClientMessage(prefix().copy().append(Component.literal(message).withStyle(ChatFormatting.RED)), false);
	}

	public static void copyToClipboard(String text) {
		Minecraft client = Minecraft.getInstance();
		try {
			org.lwjgl.glfw.GLFW.glfwSetClipboardString(client.getWindow().handle(), text);
		} catch (Throwable throwable) {
			Diagnostics.exception("clipboard", throwable);
			sendError("Could not access the clipboard.");
		}
	}

	public static String formatDuration(long millis) {
		long totalSeconds = Math.max(0L, millis / 1000L);
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0) {
			return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds);
	}

	public static String formatDistance(double blocks) {
		return String.format(java.util.Locale.ROOT, "%.0f", blocks);
	}
}
