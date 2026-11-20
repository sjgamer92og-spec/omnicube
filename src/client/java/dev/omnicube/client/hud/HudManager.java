package dev.omnicube.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.perf.PerfMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudManager {
	private final List<HudElement> elements = new ArrayList<>();
	private final Map<String, HudElement> byId = new LinkedHashMap<>();

	public void register(HudElement element) {
		if (byId.containsKey(element.id())) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Duplicate HUD element id '{}' ignored.", element.id());
			Diagnostics.event("registry", "duplicate HUD id ignored: " + element.id());
			return;
		}
		elements.add(element);
		byId.put(element.id(), element);
	}

	public List<HudElement> elements() {
		return Collections.unmodifiableList(elements);
	}

	public HudElement byId(String id) {
		return byId.get(id);
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		if (client.player == null || client.level == null || client.options.hideGui || client.screen != null) {
			return;
		}

		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();
		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}
			long started = PerfMonitor.start();
			try {
				element.measure(client);
				int x = element.screenX(client, guiWidth);
				int y = element.screenY(client, guiHeight);
				element.render(graphics, client, x, y);
			} catch (Throwable throwable) {
				// Log every element failure with a full stack trace and disable only that
				// element; other elements and vanilla HUD keep working.
				String reason = "Failed: " + Diagnostics.describe(throwable) + " in " + element.id()
						+ ".render() (see stack trace)";
				element.markFailed(reason);
				Diagnostics.exception(element.id(), throwable);
				OmniCubeMod.LOGGER.error("[OmniCube] HUD element '{}' failed and was disabled.", element.id(), throwable);
			} finally {
				PerfMonitor.record("hud." + element.id(), started);
			}
		}
	}

	public void write(JsonObject root) {
		for (HudElement element : elements) {
			// Blocked elements and auto-disabled elements keep the entry that was saved
			// before, so removing the other mod or the low-FPS condition restores it.
			if (!element.isAvailable() || element.autoDisabled()) {
				continue;
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("x", element.x());
			entry.addProperty("y", element.y());
			entry.addProperty("enabled", element.isEnabled());
			root.add(element.id(), entry);
		}
	}

	public void read(JsonObject root) {
		for (HudElement element : elements) {
			// Start from defaults so a missing or removed entry cannot leave stale layout state.
			element.resetPosition();
			element.setEnabled(element.defaultEnabled());
			JsonElement raw = root.get(element.id());
			if (raw == null || !raw.isJsonObject()) {
				continue;
			}
			try {
				JsonObject entry = raw.getAsJsonObject();
				if (entry.has("x") && entry.has("y")) {
					element.setPosition(entry.get("x").getAsDouble(), entry.get("y").getAsDouble());
				}
				if (entry.has("enabled")) {
					element.setEnabled(entry.get("enabled").getAsBoolean());
				}
			} catch (RuntimeException exception) {
				// Bad entry: keep the default layout for this element only.
				Diagnostics.exception(element.id() + ".config", exception);
				OmniCubeMod.LOGGER.warn("[OmniCube] Invalid layout entry for '{}'; using defaults.", element.id(), exception);
			}
		}
	}

	public void recordAllStates() {
		for (HudElement element : elements) {
			element.recordState();
		}
	}

	public List<Feature> features() {
		return new ArrayList<>(elements);
	}
}
