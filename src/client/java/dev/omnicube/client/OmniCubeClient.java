package dev.omnicube.client;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.command.OmniCubeCommands;
import dev.omnicube.client.compat.CompatRegistry;
import dev.omnicube.client.config.OmniCubeConfig;
import dev.omnicube.client.config.ConfigManager;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.gui.ChatSearchScreen;
import dev.omnicube.client.gui.OmniCubeScreen;
import dev.omnicube.client.gui.HudEditorScreen;
import dev.omnicube.client.gui.InventoryToolsScreen;
import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.hud.HudElements;
import dev.omnicube.client.hud.HudManager;
import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleManager;
import dev.omnicube.client.module.impl.AutoReconnectModule;
import dev.omnicube.client.module.impl.ChatCollapseModule;
import dev.omnicube.client.module.impl.ChatLogModule;
import dev.omnicube.client.module.impl.ChatModule;
import dev.omnicube.client.module.impl.CoordinatesModule;
import dev.omnicube.client.module.impl.CrosshairModule;
import dev.omnicube.client.module.impl.EntityCullingModule;
import dev.omnicube.client.module.impl.StreamerModule;
import dev.omnicube.client.module.impl.FpsSafetyModule;
import dev.omnicube.client.module.impl.FreeLookModule;
import dev.omnicube.client.module.impl.FullbrightModule;
import dev.omnicube.client.module.impl.InventoryToolsModule;
import dev.omnicube.client.module.impl.HudModule;
import dev.omnicube.client.module.impl.MacroModule;
import dev.omnicube.client.module.impl.MarkerModule;
import dev.omnicube.client.module.impl.NameTagModule;
import dev.omnicube.client.module.impl.PerformanceModule;
import dev.omnicube.client.module.impl.TooltipModule;
import dev.omnicube.client.module.impl.WaypointModule;
import dev.omnicube.client.module.impl.ZoomModule;
import dev.omnicube.client.perf.PerfMonitor;
import dev.omnicube.client.tooltip.ClientContainerTooltip;
import dev.omnicube.client.tooltip.ContainerTooltip;
import dev.omnicube.client.util.OmniCubeText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OmniCubeClient implements ClientModInitializer {
	private static OmniCubeConfig config = new OmniCubeConfig();
	private static ModuleManager modules = new ModuleManager();
	private static HudManager hud = new HudManager();

	private static HudModule hudModule;
	private static FullbrightModule fullbrightModule;
	private static ZoomModule zoomModule;
	private static WaypointModule waypointModule;
	private static MacroModule macroModule;
	private static CoordinatesModule coordinatesModule;
	private static TooltipModule tooltipModule;
	private static ChatLogModule chatLogModule;
	private static ChatModule chatModule;
	private static FreeLookModule freeLookModule;
	private static NameTagModule nameTagModule;
	private static MarkerModule markerModule;
	private static PerformanceModule performanceModule;
	private static ChatCollapseModule chatCollapseModule;
	private static int lowFpsTicks;
	private static boolean fpsGuardArmed = true;
	private static StreamerModule streamerModule;
	private static CrosshairModule crosshairModule;
	private static AutoReconnectModule autoReconnectModule;
	private static EntityCullingModule entityCullingModule;
	private static FpsSafetyModule fpsSafetyModule;

	@Override
	public void onInitializeClient() {
		config = ConfigManager.load();
		modules = new ModuleManager();
		hud = new HudManager();

		fullbrightModule = register(new FullbrightModule());
		zoomModule = register(new ZoomModule());
		freeLookModule = register(new FreeLookModule());
		register(new dev.omnicube.client.module.impl.AutoSprintModule());
		register(new dev.omnicube.client.module.impl.FlySpeedModule());
		register(new dev.omnicube.client.module.impl.ChatLogModule());
		tooltipModule = register(new TooltipModule());
		waypointModule = register(new WaypointModule());
		macroModule = register(new MacroModule());
		coordinatesModule = register(new CoordinatesModule());
		performanceModule = register(new PerformanceModule());
		hudModule = register(new HudModule());
		chatLogModule = (ChatLogModule) modules.byId("chat_log");
		chatModule = register(new ChatModule());
		chatCollapseModule = register(new ChatCollapseModule());
		streamerModule = register(new StreamerModule());
		crosshairModule = register(new CrosshairModule());
		autoReconnectModule = register(new AutoReconnectModule());
		entityCullingModule = register(new EntityCullingModule());
		fpsSafetyModule = register(new FpsSafetyModule());
		nameTagModule = register(new NameTagModule());
		markerModule = register(new MarkerModule());
		register(new InventoryToolsModule());

		HudElements.register(hud);
		modules.read(config.modules);
		hud.read(config.hud);
		try {
			CompatRegistry.detectAndApply(features());
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.error("[OmniCube] Compatibility detection failed; all features keep their configured state.", throwable);
		}
		logCompatibilitySummary();
		if (hudModule != null && hudModule.isEnabled()) {
			HudElement.background = hudModule.backgroundEnabled();
		}

		OmniCubeKeybinds.init();
		OmniCubeCommands.register();

		TooltipComponentCallback.EVENT.register(data -> {
			try {
				return data instanceof ContainerTooltip tooltip ? new ClientContainerTooltip(tooltip.stacks()) : null;
			} catch (Throwable ignored) {
				return null;
			}
		});

		// Draw before chat, the scoreboard and titles so those vanilla layers stay readable.
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,
				Identifier.fromNamespaceAndPath(OmniCubeMod.MOD_ID, "overlay"), (graphics, tickCounter) -> {
					if (hudModule != null && hudModule.isEnabled()) {
						hud.render(graphics, Minecraft.getInstance());
					}
				});

		// Fabric's HUD registry is deliberately used instead of patching vanilla
		// rendering, so Sodium/Iris keep drawing the crosshair through their path.
		try {
			HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tickCounter) -> {
				long started = PerfMonitor.start();
				try {
					if (crosshairModule != null && crosshairModule.isEnabled()) {
						crosshairModule.render(graphics);
					} else if (original != null) {
						original.render(graphics, tickCounter);
					}
				} finally {
					PerfMonitor.record("render.crosshair", started);
				}
			});
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not register the custom crosshair; the vanilla one will be used.", throwable);
		}

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			long started = PerfMonitor.start();
			try {
				if (tooltipModule != null && !lines.isEmpty()) {
					tooltipModule.appendTooltip(stack, lines);
				}
			} finally {
				PerfMonitor.record("tooltip.append", started);
			}
		});

		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> {
			long started = PerfMonitor.start();
			try {
				Component display = message;
				if (streamerModule != null) {
					display = streamerModule.redact(display);
				}
				boolean timestamped = chatModule != null && chatModule.timestampEnabled();
				if (timestamped) {
					display = chatModule.decorate(display);
				}
				if (chatModule != null) {
					chatModule.remember(display);
				}
				boolean changed = timestamped || !display.equals(message);
				if (changed) {
					if (chatLogModule != null) {
						chatLogModule.recordChat(display.getString());
					}
					Minecraft client = Minecraft.getInstance();
					Component finalDisplay = display;
					client.execute(() -> {
						try {
							client.gui.getChat().addMessage(finalDisplay);
						} catch (Throwable ignored) {
							// The message is simply dropped if the chat HUD is unavailable.
						}
					});
				}
				return !changed;
			} catch (Throwable throwable) {
				return true;
			} finally {
				PerfMonitor.record("chat.receive", started);
			}
		});

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			try {
				Component display = message;
				if (!overlay && streamerModule != null) {
					display = streamerModule.redact(display);
				}
				if (!overlay && chatModule != null && chatModule.timestampEnabled()) {
					display = chatModule.decorate(display);
				}
				return display;
			} catch (Throwable ignored) {
				// Keep the original game message.
			}
			return message;
		});

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
			if (chatLogModule != null) {
				chatLogModule.recordChat(message.getString());
			}
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (chatLogModule != null && !overlay) {
				chatLogModule.recordSystem(message.getString());
			}
		});

		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			long nameTagsStarted = PerfMonitor.start();
			try {
				if (nameTagModule != null) {
					nameTagModule.render(context);
				}
			} catch (Throwable ignored) {
				// One world feature must not break another or the vanilla pass.
			} finally {
				PerfMonitor.record("render.name_tags", nameTagsStarted);
			}
			long markersStarted = PerfMonitor.start();
			try {
				if (markerModule != null) {
					markerModule.render(context);
				}
			} catch (Throwable ignored) {
				// Same isolation for markers.
			} finally {
				PerfMonitor.record("render.world_markers", markersStarted);
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(OmniCubeClient::tick);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (performanceModule != null && performanceModule.isEnabled()) {
				performanceModule.captureForSession();
			}
			if (chatCollapseModule != null) {
				chatCollapseModule.reset();
			}
			if (autoReconnectModule != null) {
				autoReconnectModule.reset();
				autoReconnectModule.rememberServer(client);
			}
			lowFpsTicks = 0;
			fpsGuardArmed = true;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			restoreSessionOptions();
			if (chatCollapseModule != null) {
				chatCollapseModule.reset();
			}
			if (autoReconnectModule != null) {
				autoReconnectModule.onDisconnect(client);
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			restoreSessionOptions();
			saveConfig();
		});

		// Create the config file on first run so users can find and edit it.
		ConfigManager.save(config, modules, hud);
		OmniCubeMod.LOGGER.info("[OmniCube] loaded {} modules and {} HUD elements", modules.modules().size(), hud.elements().size());
	}

	private static <T extends Module> T register(T module) {
		modules.register(module);
		return module;
	}

	private static void tick(Minecraft client) {
		if (client.player != null && client.screen == null) {
			if (OmniCubeKeybinds.openMenu != null) {
				while (OmniCubeKeybinds.openMenu.consumeClick()) {
					client.setScreen(new OmniCubeScreen(null));
				}
			}
			if (OmniCubeKeybinds.hudEditor != null) {
				while (OmniCubeKeybinds.hudEditor.consumeClick()) {
					client.setScreen(new HudEditorScreen(null));
				}
			}
			if (OmniCubeKeybinds.addWaypoint != null) {
				while (OmniCubeKeybinds.addWaypoint.consumeClick()) {
					quickWaypoint(client.player);
				}
			}
			if (OmniCubeKeybinds.copyCoordinates != null) {
				while (OmniCubeKeybinds.copyCoordinates.consumeClick()) {
					if (coordinatesModule != null && coordinatesModule.isEnabled()) {
						coordinatesModule.copyPosition();
					}
				}
			}
			if (OmniCubeKeybinds.copyChat != null) {
				while (OmniCubeKeybinds.copyChat.consumeClick()) {
					if (chatModule != null) {
						chatModule.copyLast();
					}
				}
			}
			if (OmniCubeKeybinds.inventoryTools != null) {
				while (OmniCubeKeybinds.inventoryTools.consumeClick()) {
					if (modules.byId("inventory_tools") != null && modules.byId("inventory_tools").isEnabled()) {
						client.setScreen(new InventoryToolsScreen(null));
					}
				}
			}
			if (OmniCubeKeybinds.chatSearch != null) {
				while (OmniCubeKeybinds.chatSearch.consumeClick()) {
					if (chatModule != null && chatModule.isEnabled()) {
						client.setScreen(new ChatSearchScreen(null));
					} else {
						OmniCubeText.sendError("Enable Better Chat first.");
					}
				}
			}
		}

		long perfStart = PerfMonitor.start();
		try {
			modules.tick(client);
		} finally {
			PerfMonitor.record("modules.tick", perfStart);
		}
		if (autoReconnectModule != null) {
			try {
				autoReconnectModule.tick(client);
			} catch (Throwable throwable) {
				OmniCubeMod.LOGGER.warn("[OmniCube] Auto Reconnect tick failed; resetting it.", throwable);
				autoReconnectModule.reset();
			}
		}
		fpsGuardTick(client);
	}

	private static void fpsGuardTick(Minecraft client) {
		try {
			if (client.player == null || client.level == null || client.screen != null
					|| fpsSafetyModule == null || !fpsSafetyModule.isEnabled()) {
				lowFpsTicks = 0;
				return;
			}
			int fps = client.getFps();
			if (fps > 0 && fps < Math.max(10, fpsSafetyModule.floor())) {
				lowFpsTicks++;
			} else {
				lowFpsTicks = 0;
			}
			if (lowFpsTicks >= 100 && fpsGuardArmed) {
				fpsGuardArmed = false;
				lowFpsTicks = 0;
				List<String> disabled = disableHeavyFeatures();
				if (!disabled.isEmpty()) {
					OmniCubeText.sendError("FPS safety: below " + fpsSafetyModule.floor() + " FPS, disabled "
							+ String.join(", ", disabled) + ".");
					OmniCubeMod.LOGGER.warn("[OmniCube] FPS safety disabled: {}", disabled);
					OmniCubeClient.saveConfig();
				}
			}
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.debug("[OmniCube] FPS safety check failed.", throwable);
		}
	}

	public static List<String> disableHeavyFeatures() {
		List<String> disabled = new ArrayList<>();
		for (String id : List.of("free_look", "name_tags", "world_markers", "entity_culling", "inventory_tools",
				"hud_entities", "hud_tnt", "hud_keystrokes", "hud_cps")) {
			Feature feature = featureById(id);
			if (feature != null && feature.isEnabled()) {
				feature.setEnabled(false);
				disabled.add(id);
			}
		}
		return disabled;
	}

	public static void applyLowEndPreset() {
		List<String> disabled = new ArrayList<>();
		for (String id : List.of("free_look", "name_tags", "world_markers", "entity_culling", "inventory_tools",
				"chat_log", "hud_session", "hud_clock", "hud_timer", "hud_tnt", "hud_waypoint", "hud_experience",
				"hud_potions", "hud_armor", "hud_durability", "hud_keystrokes", "hud_cps", "hud_entities",
				"hud_speed", "hud_biome", "hud_facing", "hud_ping", "hud_server", "hud_memory")) {
			Feature feature = featureById(id);
			if (feature != null && feature.isEnabled()) {
				feature.setEnabled(false);
				disabled.add(id);
			}
		}
		String performance = "already on";
		if (performanceModule != null) {
			if (!performanceModule.isAvailable()) {
				performance = "skipped (another performance mod is active)";
			} else if (!performanceModule.isEnabled()) {
				performanceModule.setEnabled(true);
				performance = "enabled";
			}
		}
		fpsGuardArmed = false;
		saveConfig();
		OmniCubeText.send("Low-end preset applied: disabled " + disabled.size()
				+ " heavy features; Performance " + performance + ".");
	}


	private static void quickWaypoint(LocalPlayer player) {
		if (waypointModule == null || !waypointModule.isEnabled()) {
			OmniCubeText.sendError("Enable the Waypoints module first.");
			return;
		}
		int number = config.waypoints.size() + 1;
		waypointModule.add("WP " + number, player.getX(), player.getY(), player.getZ());
		OmniCubeText.send("Saved WP " + number + " at your position.");
	}

	private static void restoreSessionOptions() {
		try {
			if (fullbrightModule != null) {
				fullbrightModule.restore();
			}
			if (zoomModule != null) {
				zoomModule.restore();
			}
			if (performanceModule != null) {
				performanceModule.restoreForSession();
			}
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not restore options while leaving the world.", throwable);
		}
	}

	private static void logCompatibilitySummary() {
		try {
			List<CompatRegistry.DetectedMod> mods = CompatRegistry.detectedMods();
			if (mods.isEmpty()) {
				OmniCubeMod.LOGGER.info("[OmniCube] Compatibility: no known overlapping mods detected.");
				return;
			}
			OmniCubeMod.LOGGER.info("[OmniCube] Compatibility: {} overlapping mod(s) detected.", mods.size());
			for (CompatRegistry.DetectedMod mod : mods) {
				OmniCubeMod.LOGGER.info("[OmniCube]   {} ({}) -> disabled: {}", mod.name(), mod.id(),
						mod.featureIds().isEmpty() ? "none" : String.join(", ", mod.featureIds()));
			}
			for (Map.Entry<String, String> entry : CompatRegistry.disabledFeatures().entrySet()) {
				OmniCubeMod.LOGGER.info("[OmniCube]   reason for {}: {}", entry.getKey(), entry.getValue());
			}
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not log the compatibility summary.", throwable);
		}
	}

	public static List<String> compatibilityLines() {
		List<String> lines = new ArrayList<>();
		try {
			lines.addAll(CompatRegistry.summaryLines());
			if (!CompatRegistry.disabledFeatures().isEmpty()) {
				lines.add("");
				lines.add("Disabled features:");
				for (Map.Entry<String, String> entry : CompatRegistry.disabledFeatures().entrySet()) {
					lines.add("  " + entry.getKey() + ": " + entry.getValue());
				}
			}
		} catch (Throwable throwable) {
			lines.add("Compatibility information is unavailable.");
		}
		return lines;
	}

	public static void saveConfig() {
		try {
			ConfigManager.save(config, modules, hud);
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.error("[OmniCube] Unexpected config save failure.", throwable);
		}
	}

	public static void reloadConfig() {
		config = ConfigManager.load();
		modules.read(config.modules);
		hud.read(config.hud);
		if (hudModule != null) {
			HudElement.background = hudModule.isEnabled() && hudModule.backgroundEnabled();
		}
		OmniCubeText.send("Config reloaded.");
	}

	public static OmniCubeConfig config() {
		return config;
	}

	public static ModuleManager modules() {
		return modules;
	}

	public static HudManager hud() {
		return hud;
	}

	public static WaypointModule waypoints() {
		return waypointModule;
	}

	public static MacroModule macros() {
		return macroModule;
	}

	public static CoordinatesModule coordinates() {
		return coordinatesModule;
	}

	public static PerformanceModule performance() {
		return performanceModule;
	}

	public static FreeLookModule freeLook() {
		return freeLookModule;
	}

	public static TooltipModule tooltips() {
		return tooltipModule;
	}

	public static ChatModule chat() {
		return chatModule;
	}

	public static ChatLogModule chatLog() {
		return chatLogModule;
	}

	public static NameTagModule nameTags() {
		return nameTagModule;
	}

	public static ChatCollapseModule chatCollapse() {
		return chatCollapseModule;
	}

	public static StreamerModule streamer() {
		return streamerModule;
	}

	public static CrosshairModule crosshair() {
		return crosshairModule;
	}

	public static AutoReconnectModule autoReconnect() {
		return autoReconnectModule;
	}

	public static EntityCullingModule entityCulling() {
		return entityCullingModule;
	}

	public static FpsSafetyModule fpsSafety() {
		return fpsSafetyModule;
	}

	public static List<Feature> features() {
		List<Feature> features = new ArrayList<>(modules.features());
		features.addAll(hud.features());
		return features;
	}

	public static Feature featureById(String id) {
		if (id == null) {
			return null;
		}
		for (Feature feature : features()) {
			if (feature.id().equalsIgnoreCase(id)) {
				return feature;
			}
		}
		return null;
	}
}
