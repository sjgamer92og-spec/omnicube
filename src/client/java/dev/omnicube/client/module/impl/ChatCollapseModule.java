package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import net.minecraft.network.chat.Component;

/**
 * Counts consecutive duplicate chat lines. The actual line replacement happens
 * in ChatComponentMixin so no extra chat lines are added.
 */
public final class ChatCollapseModule extends Module {
	private static final java.util.regex.Pattern TIMESTAMP =
			java.util.regex.Pattern.compile("^\\[\\d{2}:\\d{2}\\]\\s*");

	private String lastKey = "";
	private int count;
	private boolean failureLogged;

	public ChatCollapseModule() {
		super("chat_collapse", "Chat Spam Collapse", "Collapses repeated consecutive chat lines into hi (2), hi (3), ...", ModuleCategory.CHAT, false);
	}

	public synchronized int collapseCount(Component message) {
		if (!isEnabled() || message == null) {
			return 0;
		}
		String key;
		try {
			key = TIMESTAMP.matcher(message.getString()).replaceFirst("").strip();
		} catch (Throwable throwable) {
			Diagnostics.exception("chat_collapse", throwable);
			return 0;
		}
		if (key.isEmpty()) {
			return 0;
		}
		if (key.equals(lastKey)) {
			if (count < Integer.MAX_VALUE) {
				count++;
			}
		} else {
			lastKey = key;
			count = 1;
		}
		return count;
	}

	public synchronized void reset() {
		lastKey = "";
		count = 0;
	}

	public synchronized void fail(String reason, Throwable throwable) {
		Diagnostics.exception("chat_collapse.hook", throwable);
		if (!failureLogged) {
			failureLogged = true;
			OmniCubeMod.LOGGER.error("[OmniCube] Chat collapse hook failed ({}); disabling it for this session.", reason, throwable);
		}
		markFailed("Failed: " + Diagnostics.describe(throwable) + " in chat collapse hook (see stack trace)");
	}

	@Override
	protected void onDisable() {
		reset();
	}
}
