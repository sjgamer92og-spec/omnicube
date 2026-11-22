package dev.omnicube.client.chat;

/** One saved chat filter: a keyword or a regular expression. */
public final class ChatFilter {
	private static final int MAX_LENGTH = 120;

	public String pattern = "";
	public boolean regex;

	public ChatFilter() {
	}

	public ChatFilter(String pattern, boolean regex) {
		this.pattern = pattern;
		this.regex = regex;
	}

	public void sanitize() {
		if (pattern == null) {
			pattern = "";
		}
		pattern = pattern.strip();
		if (pattern.length() > MAX_LENGTH) {
			pattern = pattern.substring(0, MAX_LENGTH);
		}
	}

	public String describe() {
		return regex ? "/" + pattern + "/" : pattern;
	}

	public String signature() {
		return (regex ? "r" : "l") + pattern;
	}
}
