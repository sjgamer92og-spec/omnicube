package dev.omnicube.client;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.command.OmniCubeCommands;
import dev.omnicube.client.config.OmniCubeConfig;
import dev.omnicube.client.config.ConfigManager;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.gui.OmniCubeScreen;
import dev.omnicube.client.gui.HudEditorScreen;
import dev.omnicube.client.gui.InventoryToolsScreen;
import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.hud.HudElements;
import dev.omnicube.client.hud.HudManager;
import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleManager;
import dev.omnicube.client.module.impl.ChatLogModule;
import dev.omnicube.client.module.impl.ChatModule;
import dev.omnicube.client.module.impl.CoordinatesModule;
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
		nameTagModule = register(new NameTagModule());
		markerModule = register(new MarkerModule());
		register(new InventoryToolsModule());

		HudElements.register(hud);
		modules.read(config.modules);
		hud.read(config.hud);
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

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			if (tooltipModule != null && !lines.isEmpty()) {
				tooltipModule.appendTooltip(stack, lines);
			}
		});

		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> {
			try {
				if (chatModule == null) {
					return true;
				}
				chatModule.remember(message);
				if (!chatModule.timestampEnabled()) {
					return true;
				}
				if (chatLogModule != null) {
					chatLogModule.recordChat(message.getString());
				}
				Minecraft client = Minecraft.getInstance();
				Component decorated = chatModule.decorate(message);
				client.execute(() -> {
					try {
						client.gui.getChat().addMessage(decorated);
					} catch (Throwable ignored) {
						// The message is simply dropped if the chat HUD is unavailable.
					}
				});
				return false;
			} catch (Throwable throwable) {
				return true;
			}
		});

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			try {
				if (!overlay && chatModule != null && chatModule.timestampEnabled()) {
					return chatModule.decorate(message);
				}
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
			try {
				if (nameTagModule != null) {
					nameTagModule.render(context);
				}
			} catch (Throwable ignored) {
				// One world feature must not break another or the vanilla pass.
			}
			try {
				if (markerModule != null) {
					markerModule.render(context);
				}
			} catch (Throwable ignored) {
				// Same isolation for markers.
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(OmniCubeClient::tick);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (performanceModule != null && performanceModule.isEnabled()) {
				performanceModule.captureForSession();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> restoreSessionOptions());
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
		}

		modules.tick(client);
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

	public static NameTagModule nameTags() {
		return nameTagModule;
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
