package dev.omnicube.client.compat;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.module.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single compatibility table. Add a rule here when another mod provides a
 * feature that OmniCube also implements. No optional mod class is referenced:
 * detection is only FabricLoader.isModLoaded(id).
 */
public final class CompatRegistry {
	public record DetectedMod(String id, String name, List<String> featureIds) {
		public DetectedMod {
			featureIds = new ArrayList<>(featureIds);
		}
	}

	private record Rule(String modId, String displayName, List<String> featureIds) {
		Rule {
			featureIds = List.copyOf(featureIds);
		}
	}

	private static final List<Rule> RULES = List.of(
			rule("essential", "Essential", "hud_fps", "hud_coords", "hud_server", "hud_ping"),
			rule("raised", "Raised", "hud_fps", "hud_coords", "hud_server", "hud_ping"),
			rule("betterhud", "BetterHUD", "hud_fps", "hud_coords", "hud_server"),
			rule("jade", "Jade", "hud_durability", "hud_armor"),
			rule("appleskin", "AppleSkin", "hud_durability", "hud_armor"),
			rule("inspecio", "Inspecio", "tooltips", "hud_durability", "hud_armor"),
			rule("shulkerboxtooltip", "Shulker Box Tooltip", "tooltips"),
			rule("logical_zoom", "Logical Zoom", "zoom"),
			rule("ok_zoomer", "Ok Zoomer", "zoom"),
			rule("zoomify", "Zoomify", "zoom"),
			rule("justzoom", "Just Zoom", "zoom"),
			rule("gamma_utils", "Gamma Utils", "fullbright"),
			rule("gammautils", "Gamma Utils", "fullbright"),
			rule("fullbright", "Fullbright", "fullbright"),
			rule("freelook", "Freelook", "free_look"),
			rule("perspective_mod_redux", "Perspective Mod Redux", "free_look"),
			rule("perspectivemod", "Perspective Mod", "free_look"),
			rule("chat_heads", "Chat Heads", "better_chat", "chat_collapse"),
			rule("chatpatches", "Chat Patches", "better_chat", "chat_collapse"),
			rule("chat_plus", "Chat Plus", "better_chat", "chat_collapse"),
			rule("chatplus", "Chat Plus", "better_chat", "chat_collapse"),
			rule("auto_reconnect", "Auto Reconnect", "auto_reconnect"),
			rule("autoreconnect", "Auto Reconnect", "auto_reconnect"),
			rule("entityculling", "Entity Culling", "entity_culling", "performance"),
			rule("entity_culling", "Entity Culling", "entity_culling", "performance"),
			rule("sodium", "Sodium", "performance"),
			rule("sodiumextra", "Sodium Extra", "performance"),
			rule("sodium_extra", "Sodium Extra", "performance"),
			rule("sodium-extra", "Sodium Extra", "performance"),
			rule("vulkanmod", "VulkanMod", "performance"),
			rule("lithium", "Lithium", "performance"),
			rule("ferritecore", "FerriteCore", "performance"),
			rule("immediatelyfast", "ImmediatelyFast", "performance"),
			rule("iris", "Iris", "performance"),
			rule("indium", "Indium", "performance"),
			rule("modernfix", "ModernFix", "performance"),
			rule("dynamic_fps", "Dynamic FPS", "performance"),
			rule("embeddium", "Embeddium", "performance"),
			rule("rubidium", "Rubidium", "performance")
	);

	private static final List<DetectedMod> DETECTED = new ArrayList<>();
	private static final Map<String, String> DISABLED = new LinkedHashMap<>();
	private static boolean initialized;

	private CompatRegistry() {
	}

	private static Rule rule(String modId, String displayName, String... featureIds) {
		return new Rule(modId, displayName, List.of(featureIds));
	}

	public static synchronized void detectAndApply(List<Feature> features) {
		DETECTED.clear();
		DISABLED.clear();
		Map<String, DetectedMod> detected = new LinkedHashMap<>();

		for (Rule rule : RULES) {
			if (!isLoaded(rule.modId())) {
				continue;
			}
			detected.computeIfAbsent(rule.modId(),
					id -> new DetectedMod(id, rule.displayName(), new ArrayList<>()));
			DetectedMod mod = detected.get(rule.modId());
			for (String featureId : rule.featureIds()) {
				Feature feature = find(features, featureId);
				if (feature == null || !feature.isAvailable() || DISABLED.containsKey(featureId)) {
					continue;
				}
				String reason = "This feature is already provided by " + rule.displayName()
						+ ". Remove that mod first to enable it in OmniCube.";
				if (feature instanceof Module module) {
					module.blockForCompatibility(reason);
				} else if (feature instanceof HudElement element) {
					element.blockForCompatibility(reason);
				} else {
					continue;
				}
				DISABLED.put(featureId, reason);
				mod.featureIds().add(featureId);
			}
		}

		DETECTED.addAll(detected.values());
		initialized = true;
	}

	private static Feature find(List<Feature> features, String id) {
		for (Feature feature : features) {
			if (feature.id().equalsIgnoreCase(id)) {
				return feature;
			}
		}
		return null;
	}

	private static boolean isLoaded(String modId) {
		try {
			return FabricLoader.getInstance().isModLoaded(modId);
		} catch (Throwable throwable) {
			// A broken loader query is never allowed to break startup.
			OmniCubeMod.LOGGER.debug("[OmniCube] Could not query mod '{}'.", modId, throwable);
			return false;
		}
	}

	public static synchronized boolean isInitialized() {
		return initialized;
	}

	public static synchronized List<DetectedMod> detectedMods() {
		return List.copyOf(DETECTED);
	}

	public static synchronized Map<String, String> disabledFeatures() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(DISABLED));
	}

	public static synchronized List<String> summaryLines() {
		List<String> lines = new ArrayList<>();
		if (DETECTED.isEmpty()) {
			lines.add("No known overlapping mods detected.");
			return lines;
		}
		for (DetectedMod mod : DETECTED) {
			lines.add(mod.name() + " (" + mod.id() + ")");
			for (String featureId : mod.featureIds()) {
				lines.add("  disabled: " + featureId);
			}
		}
		return lines;
	}

	public static synchronized Set<String> detectedModIds() {
		Set<String> ids = new LinkedHashSet<>();
		for (DetectedMod mod : DETECTED) {
			ids.add(mod.id());
		}
		return Collections.unmodifiableSet(ids);
	}
}
