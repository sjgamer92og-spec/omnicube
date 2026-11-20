package dev.omnicube.client.diag;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.config.ConfigManager;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.gui.ChatSearchScreen;
import dev.omnicube.client.gui.CompatibilityScreen;
import dev.omnicube.client.gui.HudEditorScreen;
import dev.omnicube.client.gui.InventoryToolsScreen;
import dev.omnicube.client.gui.MacroScreen;
import dev.omnicube.client.gui.OmniCubeScreen;
import dev.omnicube.client.keybind.OmniCubeKeybinds;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup self-check: proves that the registry, keybinds, screens and mixins are all
 * really present, and reports anything that is missing as FAILED instead of failing
 * silently.
 */
public final class SelfCheck {
	private static final String[] REQUIRED_MODULES = {
			"fullbright", "zoom", "free_look", "auto_sprint", "fly_speed", "chat_log", "tooltips",
			"waypoints", "macros", "coordinates", "performance", "hud", "better_chat", "chat_collapse",
			"streamer_mode", "crosshair", "auto_reconnect", "entity_culling", "fps_safety", "name_tags",
			"world_markers", "inventory_tools"
	};

	private static final String[] REQUIRED_HUD = {
			"hud_fps", "hud_coords", "hud_biome", "hud_facing", "hud_ping", "hud_speed", "hud_entities",
			"hud_server", "hud_memory", "hud_session", "hud_clock", "hud_timer", "hud_tnt", "hud_waypoint",
			"hud_experience", "hud_potions", "hud_armor", "hud_durability", "hud_keystrokes", "hud_cps"
	};

	private static final List<String> FAILURES = new ArrayList<>();
	private static boolean ran;

	private SelfCheck() {
	}

	public static synchronized void run(boolean hudOverlayAttached, boolean crosshairAttached) {
		if (ran) {
			return;
		}
		ran = true;
		FAILURES.clear();
		try {
			checkRegistry();
			checkKeybinds();
			checkScreens();
			checkMixins();
			checkHudHook(hudOverlayAttached);
			checkCrosshairHook(crosshairAttached);
			checkFiles();
		} catch (Throwable throwable) {
			recordFailure("self-check", "Failed: " + Diagnostics.describe(throwable) + " in self check (see stack trace)");
			Diagnostics.exception("self-check", throwable);
		}
		Diagnostics.section("SELF CHECK RESULT");
		if (FAILURES.isEmpty()) {
			Diagnostics.event("self-check", "all checks passed");
		} else {
			for (String failure : FAILURES) {
				Diagnostics.event("self-check", "FAILED " + failure);
			}
		}
	}

	private static void checkRegistry() {
		int missing = 0;
		for (String id : REQUIRED_MODULES) {
			if (OmniCubeClient.modules().byId(id) == null) {
				missing++;
				recordFailure(id, "Failed: module was not registered (see registry code)");
			}
		}
		for (String id : REQUIRED_HUD) {
			if (OmniCubeClient.hud().byId(id) == null) {
				missing++;
				recordFailure(id, "Failed: HUD element was not registered (see registry code)");
			}
		}
		Diagnostics.event("self-check", "registry: " + OmniCubeClient.modules().modules().size() + " modules, "
				+ OmniCubeClient.hud().elements().size() + " HUD elements, " + missing + " missing");
		if (OmniCubeClient.modules().modules().isEmpty() || OmniCubeClient.hud().elements().isEmpty()) {
			recordFailure("registry", "Failed: the feature registry is empty, so no screen can list features");
		}
	}

	private static void checkKeybinds() {
		checkKeybind("open_menu", OmniCubeKeybinds.openMenu);
		checkKeybind("zoom", OmniCubeKeybinds.zoom);
		checkKeybind("hud_editor", OmniCubeKeybinds.hudEditor);
		checkKeybind("add_waypoint", OmniCubeKeybinds.addWaypoint);
		checkKeybind("copy_coordinates", OmniCubeKeybinds.copyCoordinates);
		checkKeybind("free_look", OmniCubeKeybinds.freeLook);
		checkKeybind("copy_chat", OmniCubeKeybinds.copyChat);
		checkKeybind("inventory_tools", OmniCubeKeybinds.inventoryTools);
		checkKeybind("chat_search", OmniCubeKeybinds.chatSearch);
	}

	private static void checkKeybind(String id, KeyMapping mapping) {
		if (mapping == null) {
			recordFailure("keybind." + id, "Failed: keybind was not registered (keybinds and screens cannot open)");
		}
	}

	private static void checkScreens() {
		probe("screen.OmniCubeScreen", () -> new OmniCubeScreen(null));
		probe("screen.HudEditorScreen", () -> new HudEditorScreen(null));
		probe("screen.CompatibilityScreen", () -> new CompatibilityScreen(null));
		probe("screen.ChatSearchScreen", () -> new ChatSearchScreen(null));
		probe("screen.MacroScreen", () -> new MacroScreen(null));
		probe("screen.InventoryToolsScreen", () -> new InventoryToolsScreen(null));
		Feature first = OmniCubeClient.features().isEmpty() ? null : OmniCubeClient.features().getFirst();
		if (first != null) {
			probe("screen.ModuleSettingsScreen", () -> new dev.omnicube.client.gui.ModuleSettingsScreen(null, first));
		}
	}

	private static void probe(String id, ScreenFactory factory) {
		try {
			Object screen = factory.create();
			if (screen == null) {
				recordFailure(id, "Failed: screen could not be constructed");
			}
		} catch (Throwable throwable) {
			recordFailure(id, "Failed: " + Diagnostics.describe(throwable) + " while constructing the screen (see stack trace)");
			Diagnostics.exception(id, throwable);
		}
	}

	private static void checkMixins() {
		checkMixin("ChatComponentMixin", ChatComponent.class, "omnicube$collapseRepeatedMessage");
		checkMixin("CameraMixin", Camera.class, "omnicube$applyFreeLook");
		checkMixin("MouseHandlerMixin", MouseHandler.class, "omnicube$freeLook");
		checkMixin("ItemStackMixin", ItemStack.class, "omnicube$containerPreview");
		checkMixin("LivingEntityRendererMixin", LivingEntityRenderer.class, "omnicube$hideVanillaPlayerNameTag");
		checkMixin("EntityRenderDispatcherMixin", EntityRenderDispatcher.class, "omnicube$cullHiddenMob");
	}

	/**
	 * A mixin merges its handler into the target class, so the presence of the handler
	 * method on the target is direct proof that the injection was applied.
	 */
	private static void checkMixin(String name, Class<?> target, String handler) {
		boolean applied = false;
		try {
			for (Method method : target.getDeclaredMethods()) {
				if (method.getName().equals(handler)) {
					applied = true;
					break;
				}
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("mixin." + name, throwable);
		}
		if (applied) {
			Diagnostics.mixinLine(name + " (" + target.getSimpleName() + ") -> APPLIED");
			Diagnostics.event("mixin", name + ": applied");
		} else {
			Diagnostics.mixinLine(name + " (" + target.getSimpleName() + ") -> NOT APPLIED (target changed or another mod replaced it)");
			recordFailure("mixin." + name, "Mixin failed to apply: " + target.getSimpleName() + " (see MIXIN REPORT)");
		}
	}

	private static void checkHudHook(boolean attached) {
		if (!attached) {
			recordFailure("hud.overlay", "Failed: the HUD render hook was not registered (HUD elements cannot draw)");
			Diagnostics.mixinLine("HUD overlay hook -> NOT REGISTERED");
		} else {
			Diagnostics.mixinLine("HUD overlay hook (after BOSS_BAR) -> REGISTERED");
		}
	}

	private static void checkCrosshairHook(boolean attached) {
		if (!attached) {
			Diagnostics.mixinLine("Crosshair replacement hook -> NOT REGISTERED (vanilla crosshair stays)");
		} else {
			Diagnostics.mixinLine("Crosshair replacement hook -> REGISTERED");
		}
	}

	private static void checkFiles() {
		try {
			Path config = ConfigManager.configPath();
			if (Files.exists(config) && !Files.isReadable(config)) {
				recordFailure("config", "Failed: " + config.getFileName() + " is not readable");
			}
			Path log = Diagnostics.logPath();
			if (log == null || log.getParent() == null) {
				// The file itself is created by the background writer right after this check.
				recordFailure("diagnostics", "Failed: no writable diagnostics log path could be resolved");
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("self-check.files", throwable);
		}
	}

	private static void recordFailure(String id, String reason) {
		FAILURES.add(id);
		String name = id;
		Feature feature = OmniCubeClient.featureById(id);
		if (feature != null) {
			feature.markFailed(reason);
			name = feature.name();
		}
		Diagnostics.status(id, name, "self-check", "FAILED", reason);
		OmniCubeMod.LOGGER.error("[OmniCube] Self check: {} -> {}", id, reason);
	}

	public static synchronized List<String> failures() {
		return List.copyOf(FAILURES);
	}

	public static synchronized boolean hasFailures() {
		return !FAILURES.isEmpty();
	}

	@FunctionalInterface
	private interface ScreenFactory {
		Object create();
	}
}
