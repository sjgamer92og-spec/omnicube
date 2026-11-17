package dev.omnicube.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final Map<String, Module> byId = new LinkedHashMap<>();
	private boolean errorLogged;

	public void register(Module module) {
		if (byId.containsKey(module.id())) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Duplicate module id '{}' ignored.", module.id());
			return;
		}
		modules.add(module);
		byId.put(module.id(), module);
	}

	public List<Module> modules() {
		return Collections.unmodifiableList(modules);
	}

	public Module byId(String id) {
		return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
	}

	public void tick(Minecraft client) {
		for (Module module : modules) {
			if (!module.isEnabled()) {
				continue;
			}
			try {
				module.tick();
			} catch (Throwable throwable) {
				// Never let one module break the client tick loop.
				module.setEnabled(false);
				if (!errorLogged) {
					errorLogged = true;
					OmniCubeMod.LOGGER.error("[OmniCube] Module '{}' threw during tick and was disabled.", module.id(), throwable);
				}
			}
		}
	}

	public void write(JsonObject root) {
		for (Module module : modules) {
			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			JsonObject values = new JsonObject();
			for (Setting<?> setting : module.settings()) {
				values.add(setting.id(), setting.toJson());
			}
			entry.add("settings", values);
			root.add(module.id(), entry);
		}
	}

	public void read(JsonObject root) {
		for (Module module : modules) {
			// Reset to defaults first so a removed setting does not linger.
			for (Setting<?> setting : module.settings()) {
				setting.reset();
			}
			JsonElement element = root.get(module.id());
			if (element != null && element.isJsonObject()) {
				JsonObject entry = element.getAsJsonObject();
				JsonElement values = entry.get("settings");
				if (values != null && values.isJsonObject()) {
					JsonObject object = values.getAsJsonObject();
					for (Setting<?> setting : module.settings()) {
						JsonElement raw = object.get(setting.id());
						if (raw != null) {
							setting.fromJson(raw);
						}
					}
				}
				if (entry.has("enabled")) {
					try {
						module.setEnabled(entry.get("enabled").getAsBoolean());
					} catch (RuntimeException ignored) {
						module.setEnabled(module.defaultEnabled());
					}
				} else {
					module.setEnabled(module.defaultEnabled());
				}
			} else {
				module.setEnabled(module.defaultEnabled());
			}
		}
	}

	public List<Feature> features() {
		return new ArrayList<>(modules);
	}
}
