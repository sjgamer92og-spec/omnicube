package dev.omnicube.client;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.command.OmniCubeCommands;
import dev.omnicube.client.compat.CompatRegistry;
import dev.omnicube.client.config.OmniCubeConfig;
import dev.omnicube.client.config.ConfigManager;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.diag.SelfCheck;
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
import dev.omnicube.client.module.impl.FpsSafetyModule;
import dev.omnicube.client.module.impl.FreeLookModule;
import dev.omnicube.client.module.impl.ChatFilterModule;
import dev.omnicube.client.module.impl.DeathCoordsModule;
import dev.omnicube.client.module.impl.DurabilityWarningModule;
import dev.omnicube.client.module.impl.FullbrightModule;
import dev.omnicube.client.module.impl.RamWarningModule;
import dev.omnicube.client.module.impl.SettingsProfilesModule;
import dev.omnicube.client.module.impl.ToolSwapModule;
import dev.omnicube.client.module.impl.VisualTogglesModule;
import dev.omnicube.client.module.impl.HudModule;
import dev.omnicube.client.module.impl.InventoryToolsModule;
import dev.omnicube.client.module.impl.MacroModule;
import dev.omnicube.client.module.impl.MarkerModule;
import dev.omnicube.client.module.impl.NameTagModule;
import dev.omnicube.client.module.impl.PerformanceModule;
import dev.omnicube.client.module.impl.SplashModule;
import dev.omnicube.client.module.impl.StreamerModule;
import dev.omnicube.client.module.impl.TooltipModule;
import dev.omnicube.client.module.impl.WaypointModule;
import dev.omnicube.client.module.impl.ZoomModule;
import dev.omnicube.client.perf.PerfMonitor;
import dev.omnicube.client.tooltip.ClientContainerTooltip;
import dev.omnicube.client.tooltip.ContainerTooltip;
import dev.omnicube.client.util.OmniCubeText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
	private static SplashModule splashModule;
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
	private static int worldTicks;
	private static int fpsSampleTicks;
	private static boolean fpsGuardArmed = true;
	private static StreamerModule streamerModule;
	private static CrosshairModule crosshairModule;
	private static AutoReconnectModule autoReconnectModule;
	private static EntityCullingModule entityCullingModule;
	private static FpsSafetyModule fpsSafetyModule;
	private static ChatFilterModule chatFilterModule;
	private static RamWarningModule ramWarningModule;
	private static VisualTogglesModule visualTogglesModule;
	private static DeathCoordsModule deathCoordsModule;
	private static DurabilityWarningModule durabilityWarningModule;
	private static ToolSwapModule toolSwapModule;

	private static boolean hudOverlayAttached;
	private static boolean crosshairAttached;
	private static boolean startupWarningShown;
	private static boolean conflictsLogged;

	@Override
	public void onInitializeClient() {
		// Diagnostics first: anything that fails below must be recorded, not swallowed.
		Diagnostics.start();

		config = ConfigManager.load();
		modules = new ModuleManager();
		hud = new HudManager();

		safe("registry.modules", () -> {
			fullbrightModule = register(new FullbrightModule());
			zoomModule = register(new ZoomModule());
			splashModule = register(new SplashModule());
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
			chatFilterModule = register(new ChatFilterModule());
			ramWarningModule = register(new RamWarningModule());
			visualTogglesModule = register(new VisualTogglesModule());
			deathCoordsModule = register(new DeathCoordsModule());
			durabilityWarningModule = register(new DurabilityWarningModule());
			toolSwapModule = register(new ToolSwapModule());
			register(new SettingsProfilesModule());
		});

		safe("registry.hud", () -> HudElements.register(hud));
		safe("config.read.modules", () -> modules.read(config.modules));
		safe("config.read.hud", () -> hud.read(config.hud));
		safe("compat.detect", () -> CompatRegistry.detectAndApply(features()));
		safe("compat.summary", OmniCubeClient::logCompatibilitySummary);
		safe("hud.background", () -> {
			if (hudModule != null) {
				HudElement.masterEnabled = hudModule.isEnabled();
				HudElement.background = hudModule.isEnabled() && hudModule.backgroundEnabled();
			}
		});
		safe("diagnostics.redactor", () ->
				Diagnostics.installRedactor(text -> streamerModule == null ? text : streamerModule.redactString(text)));

		safe("keybinds", OmniCubeKeybinds::init);
		safe("commands", OmniCubeCommands::register);

		safe("tooltip.component.callback", () -> TooltipComponentCallback.EVENT.register(data -> {
			try {
				return data instanceof ContainerTooltip tooltip ? new ClientContainerTooltip(tooltip.stacks()) : null;
			} catch (Throwable throwable) {
				Diagnostics.exception("tooltip.render", throwable);
				return null;
			}
		}));

		registerHudOverlay();
		registerCrosshair();

		safe("tooltip.callback", () -> ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			long started = PerfMonitor.start();
			try {
				if (tooltipModule != null && !lines.isEmpty()) {
					tooltipModule.appendTooltip(stack, lines);
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("tooltip.append", throwable);
			} finally {
				PerfMonitor.record("tooltip.append", started);
			}
		}));

		safe("chat.allow", () -> ClientReceiveMessageEvents.ALLOW_CHAT.register(
				(message, signedMessage, sender, params, timestamp) -> {
					long started = PerfMonitor.start();
					try {
						Component incoming = message;
						if (chatFilterModule != null && chatFilterModule.isEnabled()) {
							if (chatFilterModule.hides(incoming)) {
								Diagnostics.event("chat_filter", "hid a chat line");
								return false;
							}
							Component filtered = chatFilterModule.apply(incoming);
							if (filtered == null) {
								return false;
							}
							incoming = filtered;
						}
						Component display = incoming;
						if (streamerModule != null) {
							display = streamerModule.redact(display);
						}
						// Keep the redacted, undecorated text for the log: logging the
						// timestamped line added the chat timestamp on top of the log timestamp.
						Component logged = display;
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
								chatLogModule.recordChat(logged.getString());
							}
							Minecraft client = Minecraft.getInstance();
							Component finalDisplay = display;
							client.execute(() -> {
								try {
									client.gui.getChat().addMessage(finalDisplay);
								} catch (Throwable throwable) {
									Diagnostics.exception("chat.display", throwable);
								}
							});
						}
						return !changed;
					} catch (Throwable throwable) {
						Diagnostics.exception("chat.receive", throwable);
						return true;
					} finally {
						PerfMonitor.record("chat.receive", started);
					}
				}));

		safe("chat.modify_game", () -> ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			try {
				Component display = message;
				if (!overlay && streamerModule != null) {
					display = streamerModule.redact(display);
				}
				// Only highlight here: hiding is handled by ALLOW_CHAT/ALLOW_GAME, and a
				// null component must never be returned from MODIFY_GAME.
				if (!overlay && chatFilterModule != null && !chatFilterModule.hideMode()) {
					Component highlighted = chatFilterModule.apply(display);
					if (highlighted != null) {
						display = highlighted;
					}
				}
				if (!overlay && chatModule != null && chatModule.timestampEnabled()) {
					display = chatModule.decorate(display);
				}
				return display;
			} catch (Throwable throwable) {
				Diagnostics.exception("chat.modify_game", throwable);
			}
			return message;
		}));

		safe("chat.allow_game", () -> ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			try {
				return chatFilterModule == null || !chatFilterModule.hides(message);
			} catch (Throwable throwable) {
				Diagnostics.exception("chat_filter.allow_game", throwable);
				return true;
			}
		}));

		safe("chat.log.callback", () -> ClientReceiveMessageEvents.CHAT.register(
				(message, signedMessage, sender, params, timestamp) -> {
					if (chatLogModule != null) {
						// Redact first: the log used to keep the unredacted line that Streamer
						// Mode only hid on screen.
						Component logged = streamerModule == null ? message : streamerModule.redact(message);
						chatLogModule.recordChat(logged.getString());
					}
				}));

		safe("chat.game.callback", () -> ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (chatLogModule != null && !overlay) {
				Component logged = streamerModule == null ? message : streamerModule.redact(message);
				chatLogModule.recordSystem(logged.getString());
			}
		}));

		safe("world.render.callback", () -> WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			long nameTagsStarted = PerfMonitor.start();
			try {
				if (nameTagModule != null && nameTagModule.isEnabled()) {
					nameTagModule.render(context);
				}
			} catch (Throwable throwable) {
				// One world feature must not break another or the vanilla pass, but the
				// failure is never silent.
				Diagnostics.exception("render.name_tags", throwable);
				if (nameTagModule != null) {
					nameTagModule.markFailed("Failed: " + Diagnostics.describe(throwable)
							+ " in NameTagModule.render() (see stack trace)");
				}
			} finally {
				PerfMonitor.record("render.name_tags", nameTagsStarted);
			}
			long markersStarted = PerfMonitor.start();
			try {
				if (markerModule != null && markerModule.isEnabled()) {
					markerModule.render(context);
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("render.world_markers", throwable);
				if (markerModule != null) {
					markerModule.markFailed("Failed: " + Diagnostics.describe(throwable)
							+ " in MarkerModule.render() (see stack trace)");
				}
			} finally {
				PerfMonitor.record("render.world_markers", markersStarted);
			}
		}));

		safe("tick.callback", () -> ClientTickEvents.END_CLIENT_TICK.register(OmniCubeClient::tick));

		safe("connection.callbacks", () -> {
			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
				Diagnostics.event("world", "joined world");
				worldTicks = 0;
				lowFpsTicks = 0;
				fpsSampleTicks = 0;
				fpsGuardArmed = true;
				try {
					if (performanceModule != null && performanceModule.isEnabled()) {
						performanceModule.captureForSession();
						performanceModule.onWorldJoin();
					}
					if (chatCollapseModule != null) {
						chatCollapseModule.reset();
					}
					if (autoReconnectModule != null) {
						autoReconnectModule.reset();
						autoReconnectModule.rememberServer(client);
					}
				} catch (Throwable throwable) {
					Diagnostics.exception("world.join", throwable);
				}
				showStartupWarning();
			});
			ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
				Diagnostics.event("world", "left world");
				try {
					restoreSessionOptions();
					if (chatCollapseModule != null) {
						chatCollapseModule.reset();
					}
					if (autoReconnectModule != null) {
						autoReconnectModule.onDisconnect(client);
					}
				} catch (Throwable throwable) {
					Diagnostics.exception("world.disconnect", throwable);
				}
				Diagnostics.flush();
			});
		});

		safe("lifecycle.callbacks", () -> {
			ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
								safe("shutdown.restore", OmniCubeClient::restoreSessionOptions);
				safe("shutdown.save", OmniCubeClient::saveConfig);
				safe("shutdown.config_flush", dev.omnicube.client.config.ConfigManager::flush);
				Diagnostics.event("lifecycle", "client stopping");
				Diagnostics.flushForShutdown();
			});
			// flushForShutdown is exception-proof, so the hook never throws during teardown.
			Runtime.getRuntime().addShutdownHook(new Thread(Diagnostics::flushForShutdown,
					"OmniCube-Diagnostics-Shutdown"));
		});

		// The feature status table is written before the report is published, so the log
		// always contains one row per feature even when startup fails.
		safe("diagnostics.status", () -> {
			modules.recordAllStates();
			hud.recordAllStates();
			Diagnostics.performanceLine("frame costs are measured in-game with /omnicube perf");
			Diagnostics.performanceLine("performance backend: " + PerformanceModule.backendId()
					+ (PerformanceModule.backendUsingFallback() ? " (fell back to built-in)" : ""));
			Diagnostics.note("fullbright", fullbrightModule == null ? "Fullbright: not registered"
					: fullbrightModule.statusLine());
			Diagnostics.note("chat_filter", chatFilterModule == null ? "Chat Filters: not registered"
					: chatFilterModule.statusLine());
			Diagnostics.note("ram_warning", ramWarningModule == null ? "RAM Warning: not registered"
					: ramWarningModule.statusLine());
			Diagnostics.note("visual_toggles", visualTogglesModule == null ? "Visual toggles: not registered"
					: visualTogglesModule.statusLine());
			Diagnostics.note("death_coords", deathCoordsModule == null ? "Death coordinates: not registered"
					: deathCoordsModule.statusLine());
			Diagnostics.note("durability_warning", durabilityWarningModule == null ? "Low durability: not registered"
					: durabilityWarningModule.statusLine());
			Diagnostics.note("tool_swap", toolSwapModule == null ? "Tool swap: not registered"
					: toolSwapModule.statusLine());
			Feature profilesFeature = featureById("settings_profiles");
			Diagnostics.note("settings_profiles", "Settings profiles: "
					+ (profilesFeature == null ? "not registered" : profilesFeature.isEnabled() ? "ON" : "OFF"));
			Diagnostics.note("config", "Config: " + dev.omnicube.client.config.ConfigManager.loadSummary());
			Diagnostics.note("config_save", "Config save: " + dev.omnicube.client.config.ConfigManager.saveSummary());
			Diagnostics.performanceLine("FPS safety: " + (fpsSafetyModule != null && fpsSafetyModule.isEnabled()
					? "armed at floor " + fpsSafetyModule.floor() + " FPS after 5s below it"
					: "disabled"));
		});

		// Create the config file on first run so users can find and edit it.
		safe("config.write.initial", () -> ConfigManager.save(config, modules, hud));

		safe("self.check", () -> SelfCheck.run(hudOverlayAttached, crosshairAttached));

		Diagnostics.publishStartupReport();
		Diagnostics.event("lifecycle", "OmniCube " + OmniCubeMod.MOD_NAME + " initialized: "
				+ modules.modules().size() + " modules, " + hud.elements().size() + " HUD elements, "
				+ SelfCheck.failures().size() + " failed check(s)");
		OmniCubeMod.LOGGER.info("[OmniCube] loaded {} modules and {} HUD elements", modules.modules().size(),
				hud.elements().size());
	}

	private static void registerHudOverlay() {
		Identifier id = Identifier.fromNamespaceAndPath(OmniCubeMod.MOD_ID, "overlay");
		try {
			HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, id, (graphics, tickCounter) -> {
				if (hudModule != null && hudModule.isEnabled()) {
					hud.render(graphics, Minecraft.getInstance());
				}
			});
			hudOverlayAttached = true;
		} catch (Throwable throwable) {
			// A single failed hook must never abort the rest of the initializer; that is
			// exactly how the HUD and the key handlers used to disappear silently.
			Diagnostics.exception("hud.overlay.attach", throwable);
			OmniCubeMod.LOGGER.error("[OmniCube] Could not attach the HUD overlay after the boss bar; trying last.", throwable);
			try {
				HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(OmniCubeMod.MOD_ID, "overlay_last"),
						(graphics, tickCounter) -> {
							if (hudModule != null && hudModule.isEnabled()) {
								hud.render(graphics, Minecraft.getInstance());
							}
						});
				hudOverlayAttached = true;
			} catch (Throwable fallbackFailure) {
				Diagnostics.exception("hud.overlay.attach.fallback", fallbackFailure);
				OmniCubeMod.LOGGER.error("[OmniCube] The HUD overlay could not be registered at all.", fallbackFailure);
			}
		}
	}

	private static void registerCrosshair() {
		try {
			HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tickCounter) -> {
				long started = PerfMonitor.start();
				try {
					if (crosshairModule != null && crosshairModule.isEnabled()) {
						crosshairModule.render(graphics);
					} else if (original != null) {
						original.render(graphics, tickCounter);
					}
				} catch (Throwable throwable) {
					Diagnostics.exception("render.crosshair", throwable);
					if (original != null) {
						original.render(graphics, tickCounter);
					}
				} finally {
					PerfMonitor.record("render.crosshair", started);
				}
			});
			crosshairAttached = true;
		} catch (Throwable throwable) {
			Diagnostics.exception("crosshair.attach", throwable);
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not register the custom crosshair; the vanilla one will be used.", throwable);
		}
	}

	/** Runs one registration step in isolation so one failure cannot hide the rest. */
	private static void safe(String name, Runnable action) {
		try {
			action.run();
		} catch (Throwable throwable) {
			Diagnostics.exception(name, throwable);
			Diagnostics.collisionLine("registration failed: " + name + " ("
					+ Diagnostics.describe(throwable) + ")");
			OmniCubeMod.LOGGER.error("[OmniCube] Startup step '{}' failed; continuing with the other features.", name, throwable);
		}
	}

	private static <T extends Module> T register(T module) {
		modules.register(module);
		return module;
	}

	private static void showStartupWarning() {
		if (startupWarningShown) {
			return;
		}
		startupWarningShown = true;
		int failures = SelfCheck.failures().size();
		if (failures <= 0) {
			return;
		}
		OmniCubeText.sendError("OmniCube: " + failures
				+ " features failed to load. See logs/" + Diagnostics.FILE_NAME);
	}

	private static void openScreen(String name, java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> screen) {
		try {
			Minecraft client = Minecraft.getInstance();
			client.setScreen(screen.get());
			Diagnostics.event("screen", "opened " + name);
		} catch (Throwable throwable) {
			Diagnostics.exception("screen." + name, throwable);
			OmniCubeText.sendError("Could not open " + name + ".");
		}
	}

	private static void tick(Minecraft client) {
		if (!conflictsLogged) {
			// Keybind scanning needs a fully constructed client, so it runs on the first
			// tick instead of during the initializer where Minecraft.getInstance() is not
			// guaranteed to have usable options yet.
			conflictsLogged = true;
			safe("keybind.conflicts", () -> {
				for (String conflict : dev.omnicube.client.keybind.KeybindConflicts.find()) {
					Diagnostics.collisionLine("keybind: " + conflict);
				}
			});
		}
		if (client.player != null && client.level != null) {
			worldTicks = Math.min(worldTicks + 1, Integer.MAX_VALUE);
		}
		if (client.player != null && client.screen == null) {
			safe("tick.keybinds", () -> {
				if (OmniCubeKeybinds.openMenu != null) {
					while (OmniCubeKeybinds.openMenu.consumeClick()) {
						openScreen("OmniCube", () -> new OmniCubeScreen(null));
					}
				}
				if (OmniCubeKeybinds.hudEditor != null) {
					while (OmniCubeKeybinds.hudEditor.consumeClick()) {
						openScreen("HUD Editor", () -> new HudEditorScreen(null));
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
							openScreen("Inventory Tools", () -> new InventoryToolsScreen(null));
						}
					}
				}
				if (OmniCubeKeybinds.swapTool != null) {
					while (OmniCubeKeybinds.swapTool.consumeClick()) {
						if (toolSwapModule != null) {
							toolSwapModule.swapToBestTool(client);
						}
					}
				}
				if (OmniCubeKeybinds.chatSearch != null) {
					while (OmniCubeKeybinds.chatSearch.consumeClick()) {
						if (chatModule != null && chatModule.isEnabled()) {
							openScreen("Chat Search", () -> new ChatSearchScreen(null));
						} else {
							OmniCubeText.sendError("Enable Better Chat first.");
						}
					}
				}
			});
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
				Diagnostics.exception("auto_reconnect.tick", throwable);
				OmniCubeMod.LOGGER.warn("[OmniCube] Auto Reconnect tick failed; resetting it.", throwable);
				autoReconnectModule.reset();
			}
		}
		fpsGuardTick(client);
		fpsSampleTick(client);
	}

	/** A single FPS sample every 30 seconds while in a world, queued off the render thread. */
	private static void fpsSampleTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		if (++fpsSampleTicks < 600) {
			return;
		}
		fpsSampleTicks = 0;
		try {
			Diagnostics.performance("FPS " + client.getFps() + " (frame "
					+ String.format(java.util.Locale.ROOT, "%.1f ms", client.getFrameTimeNs() / 1_000_000.0D)
					+ ", world ticks " + worldTicks + ")");
		} catch (Throwable throwable) {
			Diagnostics.exception("fps.sample", throwable);
		}
	}

	private static void fpsGuardTick(Minecraft client) {
		try {
			if (client.player == null || client.level == null || client.screen != null
					|| fpsSafetyModule == null || !fpsSafetyModule.isEnabled()) {
				lowFpsTicks = 0;
				return;
			}
			// Never judge FPS during the first ten seconds of a world: world load and
			// chunk building are always slow and used to disable features for good.
			if (worldTicks < 200) {
				lowFpsTicks = 0;
				return;
			}
			int fps = client.getFps();
			int floor = Math.max(10, fpsSafetyModule.floor());
			if (fps > 0 && fps < floor) {
				lowFpsTicks++;
			} else {
				lowFpsTicks = 0;
			}
			if (lowFpsTicks >= 100 && fpsGuardArmed) {
				fpsGuardArmed = false;
				lowFpsTicks = 0;
				String reason = "Disabled by FPS Safety (FPS " + fps + " < " + floor + " for 5s)";
				List<String> disabled = disableHeavyFeatures(reason);
				if (!disabled.isEmpty()) {
					OmniCubeText.sendError("FPS safety: below " + floor + " FPS, disabled "
							+ String.join(", ", disabled) + ".");
					Diagnostics.performance(reason + " -> " + String.join(", ", disabled));
					Diagnostics.event("fps_safety", reason + " -> " + String.join(", ", disabled));
					OmniCubeMod.LOGGER.warn("[OmniCube] FPS safety disabled: {}", disabled);
				}
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("fps_safety", throwable);
			OmniCubeMod.LOGGER.debug("[OmniCube] FPS safety check failed.", throwable);
		}
	}

	public static List<String> disableHeavyFeatures(String reason) {
		List<String> disabled = new ArrayList<>();
		for (String id : List.of("free_look", "name_tags", "world_markers", "entity_culling", "inventory_tools",
				"hud_entities", "hud_tnt", "hud_keystrokes", "hud_cps")) {
			Feature feature = featureById(id);
			if (feature == null || !feature.isEnabled()) {
				continue;
			}
			if (feature instanceof Module module) {
				module.setEnabledAutomatically(false, reason);
			} else if (feature instanceof HudElement element) {
				element.setEnabledAutomatically(false, reason);
			} else {
				continue;
			}
			disabled.add(id);
		}
		return disabled;
	}

	public static List<String> disableHeavyFeatures() {
		return disableHeavyFeatures("Disabled by FPS Safety (low FPS for 5s)");
	}

	public static void applyLowEndPreset() {
		List<String> disabled = new ArrayList<>();
		for (String id : List.of("free_look", "name_tags", "world_markers", "entity_culling", "inventory_tools",
				"chat_log", "hud_session", "hud_clock", "hud_timer", "hud_tnt", "hud_waypoint", "hud_experience",
				"hud_potions", "hud_armor", "hud_durability", "hud_keystrokes", "hud_cps", "hud_entities",
				"hud_speed", "hud_biome", "hud_facing", "hud_ping", "hud_server", "hud_memory",
				"hud_item_count", "tool_swap", "durability_warning", "chat_filter")) {
			Feature feature = featureById(id);
			if (feature == null || !feature.isEnabled()) {
				continue;
			}
			if (feature instanceof Module module) {
				module.setEnabledWithReason(false, "Disabled by Low-end preset");
			} else if (feature instanceof HudElement element) {
				element.setEnabledWithReason(false, "Disabled by Low-end preset");
			} else {
				continue;
			}
			disabled.add(id);
		}
		// Cheap features that help a low-end machine are switched on with safe values.
		if (ramWarningModule != null && !ramWarningModule.isEnabled()) {
			ramWarningModule.setEnabledWithReason(true, "Enabled by Low-end preset");
		}
		if (visualTogglesModule != null) {
			// Apply the safe values even when the module is already on; it used to skip
			// them, so a running Visual Toggles module kept its old expensive settings.
			visualTogglesModule.applyLowEndValues();
			if (!visualTogglesModule.isEnabled()) {
				visualTogglesModule.setEnabledWithReason(true, "Enabled by Low-end preset");
			}
		}
		if (splashModule != null) {
			splashModule.applyLowEndValues();
		}
		String performance = "already on";
		if (performanceModule != null) {
			if (!performanceModule.isAvailable()) {
				performance = "skipped (" + performanceModule.stateReason() + ")";
			} else if (!performanceModule.isEnabled()) {
				performanceModule.setEnabledWithReason(true, "Enabled by Low-end preset");
				performance = "enabled";
			}
		}
		fpsGuardArmed = false;
		saveConfig();
		Diagnostics.event("preset", "low-end preset applied: disabled " + disabled.size()
				+ " heavy features; Performance " + performance);
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
			if (visualTogglesModule != null) {
				// Put back the video options this module changed so they are not written
				// into options.txt as if the player had chosen them.
				visualTogglesModule.restoreForSession();
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("options.restore", throwable);
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
			Diagnostics.exception("compat.summary", throwable);
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
			Diagnostics.exception("compat.lines", throwable);
			lines.add("Compatibility information is unavailable.");
		}
		return lines;
	}

	public static void saveConfig() {
		try {
			// Asynchronous: the JSON snapshot is built here, the disk write happens on a
			// background thread, so no screen or tick ever waits for the disk.
			ConfigManager.save(config, modules, hud);
		} catch (Throwable throwable) {
			Diagnostics.exception("config.save", throwable);
			OmniCubeMod.LOGGER.error("[OmniCube] Unexpected config save failure.", throwable);
		}
	}

	public static void reloadConfig() {
		try {
			config = ConfigManager.load();
			applyConfigSections();
			Diagnostics.event("config", "reloaded " + ConfigManager.loadSummary());
			OmniCubeText.send("Config reloaded.");
		} catch (Throwable throwable) {
			Diagnostics.exception("config.reload", throwable);
			OmniCubeText.sendError("Config reload failed; see logs/" + Diagnostics.FILE_NAME);
		}
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

	public static ZoomModule zoom() {
		return zoomModule;
	}

	public static FullbrightModule fullbright() {
		return fullbrightModule;
	}

	/** Read from the weather render hook; cheap and safe when the module is absent. */
	public static boolean hideWeather() {
		VisualTogglesModule module = visualTogglesModule;
		return module != null && module.weatherHidden();
	}

	public static ChatFilterModule chatFilter() {
		return chatFilterModule;
	}

	public static RamWarningModule ramWarning() {
		return ramWarningModule;
	}

	public static VisualTogglesModule visualToggles() {
		return visualTogglesModule;
	}

	public static DeathCoordsModule deathCoords() {
		return deathCoordsModule;
	}

	public static DurabilityWarningModule durabilityWarning() {
		return durabilityWarningModule;
	}

	public static ToolSwapModule toolSwap() {
		return toolSwapModule;
	}

	/**
	 * Re-reads the modules and HUD sections of the live config and saves the result.
	 * Used by config reload and by loading a settings profile.
	 */
	/** Re-applies the HUD Overlay master switch to the HUD element flags. */
	public static void refreshHudMasterFlags() {
		try {
			if (hudModule != null) {
				HudElement.masterEnabled = hudModule.isEnabled();
				HudElement.background = hudModule.isEnabled() && hudModule.backgroundEnabled();
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("hud.master_flags", throwable);
		}
	}

	public static void applyConfigSections() {
		try {
			modules.read(config.modules);
			hud.read(config.hud);
			if (hudModule != null) {
				HudElement.masterEnabled = hudModule.isEnabled();
				HudElement.background = hudModule.isEnabled() && hudModule.backgroundEnabled();
			}
			modules.recordAllStates();
			hud.recordAllStates();
			saveConfig();
		} catch (Throwable throwable) {
			Diagnostics.exception("config.apply", throwable);
			OmniCubeText.sendError("Could not apply the settings; see the diagnostics log.");
		}
	}

	/**
	 * Called from the lightmap mixin for every packed light coordinate. Kept tiny and
	 * allocation-free because it runs once per rendered vertex.
	 */
	public static int fullbrightLight(int blockLight, int skyLight) {
		try {
			FullbrightModule module = fullbrightModule;
			return module == null ? -1 : module.lightOverride(blockLight, skyLight);
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.lightmap", throwable);
			return -1;
		}
	}

	public static SplashModule splash() {
		return splashModule;
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

	public static HudModule hudOverlay() {
		return hudModule;
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
