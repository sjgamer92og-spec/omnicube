package dev.omnicube.client.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.compat.ClientCompat;
import dev.omnicube.client.config.ProfileManager;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.gui.OmniCubeScreen;
import dev.omnicube.client.gui.ChatSearchScreen;
import dev.omnicube.client.gui.HudEditorScreen;
import dev.omnicube.client.macro.Macro;
import dev.omnicube.client.perf.PerfMonitor;
import dev.omnicube.client.module.impl.ChatFilterModule;
import dev.omnicube.client.module.impl.MacroModule;
import dev.omnicube.client.module.impl.WaypointModule;
import dev.omnicube.client.util.OmniCubeText;
import dev.omnicube.client.util.Stopwatch;
import dev.omnicube.client.waypoint.Waypoint;
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

public final class OmniCubeCommands {
	private static final SuggestionProvider<FabricClientCommandSource> FEATURE_IDS = (context, builder) -> {
		for (Feature feature : OmniCubeClient.features()) {
			builder.suggest(feature.id());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<FabricClientCommandSource> WAYPOINT_NAMES = (context, builder) -> {
		for (Waypoint waypoint : OmniCubeClient.config().waypoints) {
			if (waypoint.name != null) {
				builder.suggest(waypoint.name);
			}
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<FabricClientCommandSource> MACRO_NAMES = (context, builder) -> {
		for (Macro macro : OmniCubeClient.config().macros) {
			if (macro.name != null) {
				builder.suggest(macro.name);
			}
		}
		return builder.buildFuture();
	};

	private OmniCubeCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("omnicube")
						.then(ClientCommandManager.literal("config").executes(context -> openConfig(context.getSource())))
						.then(ClientCommandManager.literal("hud").executes(context -> openHudEditor(context.getSource())))
						.then(ClientCommandManager.literal("list").executes(context -> list(context.getSource())))
						.then(ClientCommandManager.literal("compat").executes(context -> compat(context.getSource())))
						.then(ClientCommandManager.literal("diag").executes(context -> diagnostics(context.getSource())))
						.then(ClientCommandManager.literal("perf").executes(context -> perf(context.getSource())))
						.then(ClientCommandManager.literal("chatsearch").executes(context -> chatSearch(context.getSource())))
						.then(ClientCommandManager.literal("lowend").executes(context -> lowEnd(context.getSource())))
						.then(ClientCommandManager.literal("toggle")
								.then(ClientCommandManager.argument("feature", StringArgumentType.word())
										.suggests(FEATURE_IDS)
										.executes(context -> toggle(context.getSource(), StringArgumentType.getString(context, "feature")))))
						.then(ClientCommandManager.literal("reload").executes(context -> reload(context.getSource())))
						.then(ClientCommandManager.literal("deaths").executes(context -> deaths(context.getSource())))
						.then(ClientCommandManager.literal("filter")
								.then(ClientCommandManager.literal("add")
										.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
												.executes(context -> addFilter(context.getSource(),
														StringArgumentType.getString(context, "pattern"), false))))
								.then(ClientCommandManager.literal("addregex")
										.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
												.executes(context -> addFilter(context.getSource(),
														StringArgumentType.getString(context, "pattern"), true))))
								.then(ClientCommandManager.literal("remove")
										.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
												.executes(context -> removeFilter(context.getSource(),
														StringArgumentType.getString(context, "pattern")))))
								.then(ClientCommandManager.literal("list").executes(context -> listFilters(context.getSource())))
								.then(ClientCommandManager.literal("clear").executes(context -> clearFilters(context.getSource()))))
						.then(ClientCommandManager.literal("profile")
								.then(ClientCommandManager.literal("list").executes(context -> listProfiles(context.getSource())))
								.then(ClientCommandManager.literal("save")
										.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
												.executes(context -> profileAction(context.getSource(), "save",
														StringArgumentType.getString(context, "name"), null))))
								.then(ClientCommandManager.literal("load")
										.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
												.executes(context -> profileAction(context.getSource(), "load",
														StringArgumentType.getString(context, "name"), null))))
								.then(ClientCommandManager.literal("delete")
										.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
												.executes(context -> profileAction(context.getSource(), "delete",
														StringArgumentType.getString(context, "name"), null))))
								.then(ClientCommandManager.literal("rename")
										.then(ClientCommandManager.argument("name", StringArgumentType.word())
												.then(ClientCommandManager.argument("new_name", StringArgumentType.word())
														.executes(context -> profileAction(context.getSource(), "rename",
																StringArgumentType.getString(context, "name"),
																StringArgumentType.getString(context, "new_name")))))))
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
		source.getClient().execute(() -> source.getClient().setScreen(new OmniCubeScreen(null)));
		return 1;
	}

	private static int openHudEditor(FabricClientCommandSource source) {
		source.getClient().execute(() -> source.getClient().setScreen(new HudEditorScreen(null)));
		return 1;
	}

	private static int list(FabricClientCommandSource source) {
		int enabled = 0;
		StringBuilder builder = new StringBuilder();
		for (Feature feature : OmniCubeClient.features()) {
			if (feature.isEnabled()) {
				enabled++;
				if (builder.length() > 0) {
					builder.append(", ");
				}
				builder.append(feature.id());
			}
		}
		source.sendFeedback(Component.literal("[OmniCube] " + enabled + "/" + OmniCubeClient.features().size() + " enabled")
				.withStyle(ChatFormatting.GREEN));
		if (builder.length() > 0) {
			source.sendFeedback(Component.literal(builder.toString()).withStyle(ChatFormatting.GRAY));
		}
		return enabled;
	}

	private static int toggle(FabricClientCommandSource source, String id) {
		Feature feature = OmniCubeClient.featureById(id);
		if (feature == null) {
			source.sendError(Component.literal("Unknown feature: " + id));
			return 0;
		}
		if (!feature.isAvailable()) {
			source.sendError(Component.literal(feature.unavailableReason()));
			return 0;
		}
		feature.setEnabled(!feature.isEnabled());
		OmniCubeClient.saveConfig();
		source.sendFeedback(Component.literal("[OmniCube] " + feature.name() + " is now " + (feature.isEnabled() ? "ON" : "OFF"))
				.withStyle(feature.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		return 1;
	}

	private static int compat(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("[OmniCube] Compatibility report")
				.withStyle(ChatFormatting.GREEN));
		for (String line : OmniCubeClient.compatibilityLines()) {
			if (!line.isBlank()) {
				source.sendFeedback(Component.literal(line).withStyle(ChatFormatting.GRAY));
			}
		}
		return 1;
	}

	private static int diagnostics(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("[OmniCube] Diagnostics")
				.withStyle(ChatFormatting.GREEN));
		for (String line : Diagnostics.summaryLines()) {
			source.sendFeedback(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
		}
		source.sendFeedback(Component.literal("  Copy the full report with the Diagnostics tab -> Copy report")
				.withStyle(ChatFormatting.DARK_GRAY));
		return 1;
	}

	private static int perf(FabricClientCommandSource source) {
		PerfMonitor.enable();
		List<String> lines = PerfMonitor.summary();
		if (lines.isEmpty()) {
			source.sendFeedback(Component.literal("[OmniCube] Profiling enabled; play for a while, then run /omnicube perf again.")
					.withStyle(ChatFormatting.GREEN));
			return 1;
		}
		source.sendFeedback(Component.literal("[OmniCube] Client callback timings (not GPU):")
				.withStyle(ChatFormatting.GREEN));
		Diagnostics.section("PROFILER SNAPSHOT");
		for (String line : lines) {
			source.sendFeedback(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
			Diagnostics.performance(line);
		}
		return 1;
	}

	private static int chatSearch(FabricClientCommandSource source) {
		source.getClient().execute(() -> source.getClient().setScreen(new ChatSearchScreen(null)));
		return 1;
	}

	private static int lowEnd(FabricClientCommandSource source) {
		OmniCubeClient.applyLowEndPreset();
		return 1;
	}

	private static int reload(FabricClientCommandSource source) {
		OmniCubeClient.reloadConfig();
		return 1;
	}

	private static int copyCoords(FabricClientCommandSource source) {
		if (OmniCubeClient.coordinates() == null) {
			source.sendError(Component.literal("Coordinates module is unavailable."));
			return 0;
		}
		OmniCubeClient.coordinates().copyPosition();
		return 1;
	}

	private static int showNbt(FabricClientCommandSource source) {
		LocalPlayer player = source.getPlayer();
		if (player == null) {
			source.sendError(Component.literal("This command needs a player."));
			return 0;
		}
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			source.sendError(Component.literal("Hold an item first."));
			return 0;
		}
		String data = stack.getComponents().toString();
		if (data.length() > 500) {
			data = data.substring(0, 500) + "...";
		}
		source.sendFeedback(Component.literal("[OmniCube] " + stack.getHoverName().getString()).withStyle(ChatFormatting.GREEN));
		source.sendFeedback(Component.literal(data).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int openScreenshots(FabricClientCommandSource source) {
		try {
			Minecraft client = source.getClient();
			Path folder = client.gameDirectory.toPath().resolve("screenshots");
			Files.createDirectories(folder);
			ClientCompat.openFile(folder.toFile());
			source.sendFeedback(Component.literal("[OmniCube] Opened " + folder).withStyle(ChatFormatting.GREEN));
			return 1;
		} catch (Throwable throwable) {
			Diagnostics.exception("command.screenshots", throwable);
			source.sendError(Component.literal("Could not open the screenshots folder."));
			return 0;
		}
	}

	private static int addWaypoint(FabricClientCommandSource source, String name) {
		WaypointModule module = OmniCubeClient.waypoints();
		if (module == null || !module.isEnabled()) {
			source.sendError(Component.literal("Enable the Waypoints module first."));
			return 0;
		}
		LocalPlayer player = source.getPlayer();
		if (player == null) {
			source.sendError(Component.literal("This command needs a player."));
			return 0;
		}
		Waypoint waypoint = module.add(name, player.getX(), player.getY(), player.getZ());
		source.sendFeedback(Component.literal("[OmniCube] Saved " + module.describe(waypoint)).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int removeWaypoint(FabricClientCommandSource source, String name) {
		WaypointModule module = OmniCubeClient.waypoints();
		if (module == null || !module.remove(name)) {
			source.sendError(Component.literal("No waypoint named '" + name + "'."));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] Removed waypoint '" + name + "'.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int setWaypoint(FabricClientCommandSource source, String name) {
		WaypointModule module = OmniCubeClient.waypoints();
		if (module == null) {
			return 0;
		}
		boolean found = module.all().stream().anyMatch(waypoint -> waypoint.name != null && waypoint.name.equalsIgnoreCase(name));
		if (!found) {
			source.sendError(Component.literal("No waypoint named '" + name + "'."));
			return 0;
		}
		OmniCubeClient.config().activeWaypoint = name;
		OmniCubeClient.saveConfig();
		source.sendFeedback(Component.literal("[OmniCube] Tracking waypoint '" + name + "'.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int listWaypoints(FabricClientCommandSource source) {
		List<Waypoint> waypoints = OmniCubeClient.config().waypoints;
		if (waypoints.isEmpty()) {
			source.sendFeedback(Component.literal("[OmniCube] No waypoints yet. Use /omnicube waypoint add <name>.")
					.withStyle(ChatFormatting.GRAY));
			return 0;
		}
		WaypointModule module = OmniCubeClient.waypoints();
		source.sendFeedback(Component.literal("[OmniCube] " + waypoints.size() + " waypoint(s):").withStyle(ChatFormatting.GREEN));
		for (int i = 0; i < Math.min(20, waypoints.size()); i++) {
			Waypoint waypoint = waypoints.get(i);
			if (waypoint == null) {
				continue;
			}
			String text = module == null ? waypoint.name : module.describe(waypoint);
			source.sendFeedback(Component.literal("  " + text + " [" + waypoint.dimension + "]").withStyle(ChatFormatting.GRAY));
		}
		return waypoints.size();
	}

	private static int clearWaypoints(FabricClientCommandSource source) {
		WaypointModule module = OmniCubeClient.waypoints();
		if (module == null) {
			return 0;
		}
		module.clear();
		source.sendFeedback(Component.literal("[OmniCube] Cleared all waypoints.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int addMacro(FabricClientCommandSource source, String name, int keyCode, String command) {
		MacroModule module = OmniCubeClient.macros();
		if (module == null) {
			source.sendError(Component.literal("Macro module is unavailable."));
			return 0;
		}
		Macro macro = module.add(name, command, keyCode);
		source.sendFeedback(Component.literal("[OmniCube] Added macro '" + macro.name + "' bound to keycode " + keyCode + ".")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int removeMacro(FabricClientCommandSource source, String name) {
		MacroModule module = OmniCubeClient.macros();
		if (module == null || !module.remove(name)) {
			source.sendError(Component.literal("No macro named '" + name + "'."));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] Removed macro '" + name + "'.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int runMacro(FabricClientCommandSource source, String name) {
		MacroModule module = OmniCubeClient.macros();
		if (module == null) {
			return 0;
		}
		for (Macro macro : OmniCubeClient.config().macros) {
			if (macro.name != null && macro.name.equalsIgnoreCase(name)) {
				module.run(macro);
				source.sendFeedback(Component.literal("[OmniCube] Ran macro '" + name + "'.").withStyle(ChatFormatting.GREEN));
				return 1;
			}
		}
		source.sendError(Component.literal("No macro named '" + name + "'."));
		return 0;
	}

	private static int listMacros(FabricClientCommandSource source) {
		List<Macro> macros = OmniCubeClient.config().macros;
		if (macros.isEmpty()) {
			source.sendFeedback(Component.literal("[OmniCube] No macros. Open the Macros screen or use /omnicube macro add.")
					.withStyle(ChatFormatting.GRAY));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] " + macros.size() + " macro(s):").withStyle(ChatFormatting.GREEN));
		for (Macro macro : macros) {
			source.sendFeedback(Component.literal("  " + macro.name + " [" + macro.keyName() + "] -> " + macro.command)
					.withStyle(ChatFormatting.GRAY));
		}
		return macros.size();
	}

	private static int deaths(FabricClientCommandSource source) {
		List<String> deaths = OmniCubeClient.config().deaths;
		if (deaths.isEmpty()) {
			source.sendFeedback(Component.literal("[OmniCube] No deaths recorded yet.").withStyle(ChatFormatting.GRAY));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] Last " + deaths.size() + " death(s):")
				.withStyle(ChatFormatting.GREEN));
		for (int i = deaths.size() - 1; i >= 0; i--) {
			source.sendFeedback(Component.literal("  " + deaths.get(i)).withStyle(ChatFormatting.GRAY));
		}
		return deaths.size();
	}

	private static int addFilter(FabricClientCommandSource source, String pattern, boolean regex) {
		ChatFilterModule module = OmniCubeClient.chatFilter();
		if (module == null) {
			source.sendError(Component.literal("Chat Filters are unavailable."));
			return 0;
		}
		String problem = module.add(pattern, regex);
		if (problem != null) {
			source.sendError(Component.literal(problem));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] Added " + (regex ? "regex " : "keyword ")
				+ "filter: " + pattern).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int removeFilter(FabricClientCommandSource source, String pattern) {
		ChatFilterModule module = OmniCubeClient.chatFilter();
		if (module == null || !module.remove(pattern)) {
			source.sendError(Component.literal("No saved filter matches '" + pattern + "'."));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] Removed the filter '" + pattern + "'.")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int listFilters(FabricClientCommandSource source) {
		ChatFilterModule module = OmniCubeClient.chatFilter();
		if (module == null) {
			return 0;
		}
		var filters = module.filters();
		if (filters.isEmpty()) {
			source.sendFeedback(Component.literal("[OmniCube] No chat filters yet. Use /omnicube filter add <text>.")
					.withStyle(ChatFormatting.GRAY));
			return 0;
		}
		source.sendFeedback(Component.literal("[OmniCube] " + filters.size() + " chat filter(s), "
				+ module.statusLine()).withStyle(ChatFormatting.GREEN));
		for (var filter : filters) {
			source.sendFeedback(Component.literal("  " + filter.describe()).withStyle(ChatFormatting.GRAY));
		}
		return filters.size();
	}

	private static int clearFilters(FabricClientCommandSource source) {
		ChatFilterModule module = OmniCubeClient.chatFilter();
		if (module == null) {
			return 0;
		}
		int count = module.clear();
		source.sendFeedback(Component.literal("[OmniCube] Cleared " + count + " chat filter(s).")
				.withStyle(ChatFormatting.GREEN));
		return count;
	}

	private static int listProfiles(FabricClientCommandSource source) {
		ProfileManager.namesAsync(names -> {
			source.sendFeedback(Component.literal("[OmniCube] Built-in profiles: "
					+ String.join(", ", ProfileManager.builtInNames())).withStyle(ChatFormatting.GREEN));
			if (names.isEmpty()) {
				source.sendFeedback(Component.literal("  No saved profiles yet. Use /omnicube profile save <name>.")
						.withStyle(ChatFormatting.GRAY));
				return;
			}
			source.sendFeedback(Component.literal("  Saved: " + String.join(", ", names))
					.withStyle(ChatFormatting.GRAY));
		});
		return 1;
	}

	private static int profileAction(FabricClientCommandSource source, String action, String name, String newName) {
		java.util.function.Consumer<String> result = message -> source.sendFeedback(
				Component.literal("[OmniCube] " + message).withStyle(ChatFormatting.GRAY));
		switch (action) {
			case "save" -> ProfileManager.saveAsync(name, result);
			case "load" -> ProfileManager.loadAsync(name, result);
			case "delete" -> ProfileManager.deleteAsync(name, result);
			case "rename" -> ProfileManager.renameAsync(name, newName, result);
			default -> {
				return 0;
			}
		}
		return 1;
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
		source.sendFeedback(Component.literal("[OmniCube] Timer " + action + " (" + OmniCubeText.formatDuration(Stopwatch.elapsedMillis()) + ").")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}
}
