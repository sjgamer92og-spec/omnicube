package dev.omnicube.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.feature.Feature;
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
	private boolean errorLogged;

	public void register(HudElement element) {
		if (byId.containsKey(element.id())) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Duplicate HUD element id '{}' ignored.", element.id());
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
			try {
				element.measure(client);
				int x = element.screenX(client, guiWidth);
				int y = element.screenY(client, guiHeight);
				element.render(graphics, client, x, y);
			} catch (Throwable throwable) {
				element.setEnabled(false);
				if (!errorLogged) {
					errorLogged = true;
					OmniCubeMod.LOGGER.error("[OmniCube] HUD element '{}' failed and was disabled.", element.id(), throwable);
				}
			}
		}
	}

	public void write(JsonObject root) {
		for (HudElement element : elements) {
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
			} catch (RuntimeException ignored) {
				// Bad entry: keep the default layout for this element only.
			}
		}
	}

	public List<Feature> features() {
		return new ArrayList<>(elements);
	}
}
