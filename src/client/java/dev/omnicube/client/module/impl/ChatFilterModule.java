package dev.omnicube.client.module.impl;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.chat.ChatFilter;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Hides or highlights chat lines that match a saved keyword or regex.
 *
 * <p>Patterns are compiled once and reused; the compiled list is rebuilt only when
 * the saved filter list changes. Invalid regexes are rejected when they are added,
 * never while a message is being received.
 */
public final class ChatFilterModule extends Module {
	public static final int MAX_FILTERS = 50;

	private final Setting.Choice action = add(new Setting.Choice("action", "Action", List.of("Hide", "Highlight"), "Hide"));
	private final List<Pattern> compiled = new ArrayList<>();
	private int compiledSignature;
	private boolean compiledReady;

	public ChatFilterModule() {
		super("chat_filter", "Chat Filters",
				"Hides or highlights chat lines that contain a saved keyword or match a saved regex. "
						+ "Add patterns with /omnicube filter add <pattern> [regex].",
				ModuleCategory.CHAT, false);
	}

	@Override
	protected void onEnable() {
		publishStatus();
	}

	@Override
	protected void onDisable() {
		publishStatus();
	}

	@Override
	public void tick() {
		// Nothing runs per tick; this only keeps the status line fresh after editing.
	}

	public boolean hideMode() {
		return "Hide".equals(action.get());
	}

	/**
	 * Returns the component to show, or null to hide the message. When the module
	 * is off or nothing matches, the exact original component is returned.
	 */
	public Component apply(Component message) {
		if (!isEnabled() || message == null) {
			return message;
		}
		try {
			if (!matches(message.getString())) {
				return message;
			}
			if (hideMode()) {
				return null;
			}
			return message.copy().withStyle(style -> style.withColor(ChatFormatting.YELLOW));
		} catch (Throwable throwable) {
			Diagnostics.exception("chat_filter.apply", throwable);
			return message;
		}
	}

	/** True when a message should be removed before it reaches the chat. */
	public boolean hides(Component message) {
		if (!isEnabled() || !hideMode() || message == null) {
			return false;
		}
		try {
			return matches(message.getString());
		} catch (Throwable throwable) {
			Diagnostics.exception("chat_filter.hides", throwable);
			return false;
		}
	}

	private boolean matches(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		ensureCompiled();
		for (Pattern pattern : compiled) {
			try {
				if (pattern.matcher(text).find()) {
					return true;
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("chat_filter.match", throwable);
			}
		}
		return false;
	}

	private synchronized void ensureCompiled() {
		List<ChatFilter> filters = OmniCubeClient.config().chatFilters;
		int signature = signatureOf(filters);
		if (compiledReady && signature == compiledSignature) {
			return;
		}
		compiled.clear();
		for (ChatFilter filter : filters) {
			try {
				compiled.add(Pattern.compile(filter.pattern, filter.regex ? 0 : Pattern.LITERAL));
			} catch (PatternSyntaxException exception) {
				// A pattern saved by an older build or edited by hand must not break chat.
				Diagnostics.exception("chat_filter.compile", exception);
			}
		}
		compiledSignature = signature;
		compiledReady = true;
	}

	private static int signatureOf(List<ChatFilter> filters) {
		int signature = filters.size();
		for (ChatFilter filter : filters) {
			signature = signature * 31 + filter.signature().hashCode();
		}
		return signature;
	}

	/** Adds a filter. Returns null on success, otherwise a message to show the user. */
	public String add(String pattern, boolean regex) {
		List<ChatFilter> filters = OmniCubeClient.config().chatFilters;
		if (filters.size() >= MAX_FILTERS) {
			Diagnostics.event("chat_filter", "add rejected: limit of " + MAX_FILTERS + " reached");
			return "Chat Filters: the limit of " + MAX_FILTERS + " patterns is reached. Remove one first.";
		}
		ChatFilter filter = new ChatFilter(pattern, regex);
		filter.sanitize();
		if (filter.pattern.isEmpty()) {
			return "Chat Filters: the pattern is empty.";
		}
		if (regex && !validRegex(filter.pattern)) {
			return "Chat Filters: invalid regex, it was not saved.";
		}
		filters.add(filter);
		compiledReady = false;
		publishStatus();
		Diagnostics.event("chat_filter", "added " + (regex ? "regex " : "keyword ") + filter.pattern);
		OmniCubeClient.saveConfig();
		return null;
	}

	private boolean validRegex(String pattern) {
		try {
			Pattern.compile(pattern);
			return true;
		} catch (PatternSyntaxException exception) {
			Diagnostics.exception("chat_filter.add.regex", exception);
			Diagnostics.event("chat_filter", "rejected invalid regex: " + exception.getDescription());
			return false;
		}
	}

	public boolean remove(int index) {
		List<ChatFilter> filters = OmniCubeClient.config().chatFilters;
		if (index < 0 || index >= filters.size()) {
			return false;
		}
		ChatFilter removed = filters.remove(index);
		compiledReady = false;
		publishStatus();
		Diagnostics.event("chat_filter", "removed " + removed.describe());
		OmniCubeClient.saveConfig();
		return true;
	}

	public boolean remove(String pattern) {
		List<ChatFilter> filters = OmniCubeClient.config().chatFilters;
		for (int i = 0; i < filters.size(); i++) {
			if (filters.get(i).pattern.equals(pattern)) {
				return remove(i);
			}
		}
		return false;
	}

	public int clear() {
		List<ChatFilter> filters = OmniCubeClient.config().chatFilters;
		int count = filters.size();
		filters.clear();
		compiledReady = false;
		publishStatus();
		Diagnostics.event("chat_filter", "cleared " + count + " filter(s)");
		OmniCubeClient.saveConfig();
		return count;
	}

	public List<ChatFilter> filters() {
		return List.copyOf(OmniCubeClient.config().chatFilters);
	}

	public String statusLine() {
		return "Chat Filters: " + (isEnabled() ? "ON" : "OFF") + " | " + action.get().toLowerCase(java.util.Locale.ROOT)
				+ " | " + OmniCubeClient.config().chatFilters.size() + "/" + MAX_FILTERS + " pattern(s)";
	}

	private void publishStatus() {
		try {
			Diagnostics.note("chat_filter", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("chat_filter.status", throwable);
		}
	}
}
