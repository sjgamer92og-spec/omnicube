package dev.omnicube.client.hud;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * A draggable HUD element. Elements render plain text by default; a few
 * (keystrokes) override {@link #render} to draw simple rectangles.
 */
public abstract class HudElement implements Feature {
	private static final int PADDING = 3;
	private static final int LINE_HEIGHT = 10;
	public static boolean background = true;
	/** Mirrors the HUD Overlay master switch so reasons can explain hidden elements. */
	public static boolean masterEnabled = true;

	private final String id;
	private final String name;
	private final String description;
	private double defaultX;
	private double defaultY;
	private double x;
	private double y;
	private boolean defaultEnabled = true;
	private boolean enabled;
	private String unavailableReason = "";
	private String reason = "";
	private boolean autoDisabled;
	private int measuredWidth = -1;
	private int measuredHeight = -1;

	protected HudElement(String id, String name, String description, double defaultX, double defaultY) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
		this.enabled = true;
		this.reason = "Enabled (default)";
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public ModuleCategory category() {
		return ModuleCategory.HUD;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isAvailable() {
		return unavailableReason.isEmpty();
	}

	@Override
	public String unavailableReason() {
		return unavailableReason;
	}

	@Override
	public String stateReason() {
		if (!isAvailable()) {
			return unavailableReason;
		}
		if (enabled && !masterEnabled) {
			return "Hidden: HUD Overlay master switch is off";
		}
		return reason;
	}

	public boolean autoDisabled() {
		return autoDisabled;
	}

	public void blockForCompatibility(String reason) {
		this.unavailableReason = reason == null ? "" : reason;
		this.reason = this.unavailableReason;
		this.autoDisabled = true;
		this.enabled = false;
		recordState();
	}

	@Override
	public void setEnabled(boolean enabled) {
		applyState(enabled, enabled ? "Enabled" : "Disabled by user", false);
	}

	/** Changes state with an explicit reason and keeps it out of the saved config. */
	public void setEnabledAutomatically(boolean enabled, String why) {
		applyState(enabled, why == null || why.isBlank() ? "Disabled by OmniCube" : why, true);
	}

	/** Changes state with an explicit reason and saves it like a user choice. */
	public void setEnabledWithReason(boolean enabled, String why) {
		applyState(enabled, why == null || why.isBlank() ? "Changed by OmniCube" : why, false);
	}

	@Override
	public void markFailed(String reason) {
		this.enabled = false;
		this.autoDisabled = true;
		this.reason = reason == null || reason.isBlank() ? "Failed (see stack trace)" : reason;
		recordState();
	}

	private void applyState(boolean newState, String why, boolean automatic) {
		if (newState && !isAvailable()) {
			this.reason = unavailableReason;
			recordState();
			return;
		}
		boolean changed = newState != enabled;
		this.enabled = newState;
		this.reason = why;
		this.autoDisabled = automatic;
		recordState();
		if (changed) {
			Diagnostics.event("hud", id + " -> " + (enabled ? "ENABLED" : "DISABLED") + " (" + this.reason + ")");
		}
	}

	public void recordState() {
		Diagnostics.status(id, name, "hud", state(), stateReason());
	}

	private String state() {
		if (!isAvailable()) {
			return "BLOCKED";
		}
		if (reason != null && reason.startsWith("Failed")) {
			return "FAILED";
		}
		return enabled ? "ENABLED" : "DISABLED";
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public void setPosition(double newX, double newY) {
		this.x = HudPos.clamp(newX);
		this.y = HudPos.clamp(newY);
	}

	public void resetPosition() {
		this.x = defaultX;
		this.y = defaultY;
	}

	public void setDefaultPosition(double x, double y) {
		this.defaultX = HudPos.clamp(x);
		this.defaultY = HudPos.clamp(y);
	}

	public boolean defaultEnabled() {
		return defaultEnabled;
	}

	public void setDefaultEnabled(boolean enabled) {
		this.defaultEnabled = enabled;
		// A compatibility block must survive layout resets and config reloads.
		this.enabled = enabled && isAvailable();
	}

	public int screenX(Minecraft client, int guiWidth) {
		ensureMeasured(client);
		return (int) Math.round(x * Math.max(0, guiWidth - measuredWidth));
	}

	public int screenY(Minecraft client, int guiHeight) {
		ensureMeasured(client);
		return (int) Math.round(y * Math.max(0, guiHeight - measuredHeight));
	}

	/** Measures once before the first frame so elements do not jump after being drawn. */
	public void measure(Minecraft client) {
		setMeasuredSize(width(client), height(client));
	}

	private void ensureMeasured(Minecraft client) {
		if (measuredWidth <= 0 || measuredHeight <= 0) {
			measure(client);
		}
	}

	protected final void setMeasuredSize(int width, int height) {
		this.measuredWidth = Math.max(1, width);
		this.measuredHeight = Math.max(1, height);
	}

	public int measuredWidth() {
		return measuredWidth;
	}

	public int measuredHeight() {
		return measuredHeight;
	}

	/** Lines shown by the default text renderer. */
	public abstract List<String> lines(Minecraft client);

	protected int accentColor() {
		return 0xFF7ED957;
	}

	/** Colour for one rendered line; elements can override it for status colours. */
	protected int lineColor(int index, String line) {
		return index == 0 ? accentColor() : 0xFFE6E6E6;
	}

	public void render(GuiGraphics graphics, Minecraft client, int screenX, int screenY) {
		List<String> lines = lines(client);
		if (lines == null || lines.isEmpty()) {
			return;
		}

		int available = Math.max(32, graphics.guiWidth() - Math.max(0, screenX) - PADDING * 2);
		int width = 0;
		for (String raw : lines) {
			String line = raw == null ? "" : raw;
			width = Math.max(width, client.font.width(line));
		}
		width = Math.min(width, available);
		int height = lines.size() * LINE_HEIGHT;
		setMeasuredSize(width + PADDING * 2, height + PADDING * 2);

		if (background) {
			graphics.fill(screenX - PADDING, screenY - PADDING, screenX + width + PADDING, screenY + height + PADDING, 0x66000000);
		}

		int y = screenY;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i) == null ? "" : lines.get(i);
			if (client.font.width(line) > width) {
				line = client.font.plainSubstrByWidth(line, Math.max(1, width - client.font.width("..."))) + "...";
			}
			graphics.drawString(client.font, line, screenX, y, lineColor(i, line), true);
			y += LINE_HEIGHT;
		}
	}

	public int width(Minecraft client) {
		int width = 0;
		for (String line : lines(client)) {
			width = Math.max(width, client.font.width(line == null ? "" : line));
		}
		return width + PADDING * 2;
	}

	public int height(Minecraft client) {
		List<String> lines = lines(client);
		return Math.max(1, lines == null ? 0 : lines.size()) * LINE_HEIGHT + PADDING * 2;
	}
}
