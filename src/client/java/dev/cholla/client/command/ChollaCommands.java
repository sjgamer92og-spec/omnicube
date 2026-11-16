
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

	private static final SuggestionProvider<FabricClientCommandSource> FEATURE_IDS = (context, builder) -> {
			builder.suggest(feature.id());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<FabricClientCommandSource> WAYPOINT_NAMES = (context, builder) -> {
			if (waypoint.name != null) {
				builder.suggest(waypoint.name);
			}
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<FabricClientCommandSource> MACRO_NAMES = (context, builder) -> {
			if (macro.name != null) {
				builder.suggest(macro.name);
			}
		}
		return builder.buildFuture();
	};

	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
						.then(ClientCommandManager.literal("config").executes(context -> openConfig(context.getSource())))
						.then(ClientCommandManager.literal("hud").executes(context -> openHudEditor(context.getSource())))
						.then(ClientCommandManager.literal("list").executes(context -> list(context.getSource())))
						.then(ClientCommandManager.literal("toggle")
								.then(ClientCommandManager.argument("feature", StringArgumentType.word())
										.suggests(FEATURE_IDS)
										.executes(context -> toggle(context.getSource(), StringArgumentType.getString(context, "feature")))))
						.then(ClientCommandManager.literal("reload").executes(context -> reload(context.getSource())))
						.then(ClientCommandManager.literal("coords").executes(context -> copyCoords(context.getSource())))
						.then(ClientCommandManager.literal("nbt").executes(context -> showNbt(context.getSource())))
						.then(ClientCommandManager.literal("screenshots").executes(context -> openScreenshots(context.getSource())))
						.then(ClientCommandManager.literal("waypoint")
								.then(ClientCommandManager.literal("add")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name")))))
								.then(ClientCommandManager.literal("remove")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.suggests(WAYPOINT_NAMES)
												.executes(context -> removeWaypoint(context.getSource(), StringArgumentType.getString(context, "name")))))
								.then(ClientCommandManager.literal("set")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.suggests(WAYPOINT_NAMES)
												.executes(context -> setWaypoint(context.getSource(), StringArgumentType.getString(context, "name")))))
								.then(ClientCommandManager.literal("list").executes(context -> listWaypoints(context.getSource())))
								.then(ClientCommandManager.literal("clear").executes(context -> clearWaypoints(context.getSource()))))
						.then(ClientCommandManager.literal("macro")
								.then(ClientCommandManager.literal("add")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.then(ClientCommandManager.argument("keycode", IntegerArgumentType.integer(32, 348))
														.then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
																.executes(context -> addMacro(context.getSource(),
																		StringArgumentType.getString(context, "name"),
																		IntegerArgumentType.getInteger(context, "keycode"),
																		StringArgumentType.getString(context, "command")))))))
								.then(ClientCommandManager.literal("remove")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.suggests(MACRO_NAMES)
												.executes(context -> removeMacro(context.getSource(), StringArgumentType.getString(context, "name")))))
								.then(ClientCommandManager.literal("run")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.suggests(MACRO_NAMES)
												.executes(context -> runMacro(context.getSource(), StringArgumentType.getString(context, "name")))))
								.then(ClientCommandManager.literal("list").executes(context -> listMacros(context.getSource()))))
						.then(ClientCommandManager.literal("timer")
								.then(ClientCommandManager.literal("start").executes(context -> timer(context.getSource(), "start")))
								.then(ClientCommandManager.literal("stop").executes(context -> timer(context.getSource(), "stop")))
								.then(ClientCommandManager.literal("reset").executes(context -> timer(context.getSource(), "reset"))))
		));
	}

	private static int openConfig(FabricClientCommandSource source) {
		return 1;
	}

	private static int openHudEditor(FabricClientCommandSource source) {
		source.getClient().execute(() -> source.getClient().setScreen(new HudEditorScreen(null)));
		return 1;
	}

	private static int list(FabricClientCommandSource source) {
		int enabled = 0;
		StringBuilder builder = new StringBuilder();
			if (feature.isEnabled()) {
				enabled++;
				if (builder.length() > 0) {
					builder.append(", ");
				}
				builder.append(feature.id());
			}
		}
				.withStyle(ChatFormatting.GREEN));
		if (builder.length() > 0) {
			source.sendFeedback(Component.literal(builder.toString()).withStyle(ChatFormatting.GRAY));
		}
		return enabled;
	}

	private static int toggle(FabricClientCommandSource source, String id) {
		if (feature == null) {
			source.sendError(Component.literal("Unknown feature: " + id));
			return 0;
		}
		feature.setEnabled(!feature.isEnabled());
				.withStyle(feature.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		return 1;
	}

	private static int reload(FabricClientCommandSource source) {
		return 1;
	}

	private static int copyCoords(FabricClientCommandSource source) {
			source.sendError(Component.literal("Coordinates module is unavailable."));
			return 0;
		}
		return 1;
	}

	private static int showNbt(FabricClientCommandSource source) {
		LocalPlayer player = source.getPlayer();
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			source.sendError(Component.literal("Hold an item first."));
			return 0;
		}
		String data = stack.getComponents().toString();
		if (data.length() > 500) {
			data = data.substring(0, 500) + "...";
		}
		source.sendFeedback(Component.literal(data).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int openScreenshots(FabricClientCommandSource source) {
		try {
			Minecraft client = source.getClient();
			Path folder = client.gameDirectory.toPath().resolve("screenshots");
			Files.createDirectories(folder);
			ClientCompat.openFile(folder.toFile());
			return 1;
		} catch (Throwable throwable) {
			source.sendError(Component.literal("Could not open the screenshots folder."));
			return 0;
		}
	}

	private static int addWaypoint(FabricClientCommandSource source, String name) {
		if (module == null || !module.isEnabled()) {
			source.sendError(Component.literal("Enable the Waypoints module first."));
			return 0;
		}
		LocalPlayer player = source.getPlayer();
		Waypoint waypoint = module.add(name, player.getX(), player.getY(), player.getZ());
		return 1;
	}

	private static int removeWaypoint(FabricClientCommandSource source, String name) {
		if (module == null || !module.remove(name)) {
			source.sendError(Component.literal("No waypoint named '" + name + "'."));
			return 0;
		}
		return 1;
	}

	private static int setWaypoint(FabricClientCommandSource source, String name) {
		if (module == null) {
			return 0;
		}
		boolean found = module.all().stream().anyMatch(waypoint -> waypoint.name != null && waypoint.name.equalsIgnoreCase(name));
		if (!found) {
			source.sendError(Component.literal("No waypoint named '" + name + "'."));
			return 0;
		}
		return 1;
	}

	private static int listWaypoints(FabricClientCommandSource source) {
		if (waypoints.isEmpty()) {
					.withStyle(ChatFormatting.GRAY));
			return 0;
		}
		for (int i = 0; i < Math.min(20, waypoints.size()); i++) {
			Waypoint waypoint = waypoints.get(i);
			String text = module == null ? waypoint.name : module.describe(waypoint);
			source.sendFeedback(Component.literal("  " + text + " [" + waypoint.dimension + "]").withStyle(ChatFormatting.GRAY));
		}
		return waypoints.size();
	}

	private static int clearWaypoints(FabricClientCommandSource source) {
		if (module == null) {
			return 0;
		}
		module.clear();
		return 1;
	}

	private static int addMacro(FabricClientCommandSource source, String name, int keyCode, String command) {
		if (module == null) {
			source.sendError(Component.literal("Macro module is unavailable."));
			return 0;
		}
		Macro macro = module.add(name, command, keyCode);
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int removeMacro(FabricClientCommandSource source, String name) {
		if (module == null || !module.remove(name)) {
			source.sendError(Component.literal("No macro named '" + name + "'."));
			return 0;
		}
		return 1;
	}

	private static int runMacro(FabricClientCommandSource source, String name) {
		if (module == null) {
			return 0;
		}
			if (macro.name != null && macro.name.equalsIgnoreCase(name)) {
				module.run(macro);
				return 1;
			}
		}
		source.sendError(Component.literal("No macro named '" + name + "'."));
		return 0;
	}

	private static int listMacros(FabricClientCommandSource source) {
		if (macros.isEmpty()) {
					.withStyle(ChatFormatting.GRAY));
			return 0;
		}
		for (Macro macro : macros) {
			source.sendFeedback(Component.literal("  " + macro.name + " [" + macro.keyName() + "] -> " + macro.command)
					.withStyle(ChatFormatting.GRAY));
		}
		return macros.size();
	}

	private static int timer(FabricClientCommandSource source, String action) {
		switch (action) {
			case "start" -> Stopwatch.start();
			case "stop" -> Stopwatch.stop();
			case "reset" -> Stopwatch.reset();
			default -> {
				return 0;
			}
		}
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}
}
