package dev.omnicube.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.feature.Feature;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Settings profiles.
 *
 * <p>Four built-in profiles are applied programmatically so they always match the
 * features of the running build. A player profile is a small JSON file holding only
 * the {@code modules} and {@code hud} sections, written and read on a background
 * thread, atomically, with a corrupt file reported instead of applied.
 */
public final class ProfileManager {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final int MAX_NAME_LENGTH = 24;
	private static final List<String> BUILT_IN = List.of("Default", "Low-end", "Streaming", "PvP");

	private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "OmniCube-Profiles");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	private static volatile String lastResult = "no profile action yet";

	private ProfileManager() {
	}

	public static List<String> builtInNames() {
		return BUILT_IN;
	}

	public static boolean isBuiltIn(String name) {
		for (String builtIn : BUILT_IN) {
			if (builtIn.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public static String lastResult() {
		return lastResult;
	}

	public static Path directory() {
		try {
			return FabricLoader.getInstance().getConfigDir().resolve("omnicube").resolve("profiles");
		} catch (Throwable throwable) {
			Diagnostics.exception("profiles.dir", throwable);
			return Path.of("config", "omnicube", "profiles");
		}
	}

	/** Reads the saved profile names off the render thread, then calls back on it. */
	public static void namesAsync(Consumer<List<String>> callback) {
		IO.submit(() -> {
			List<String> names = namesNow();
			Minecraft.getInstance().execute(() -> {
				try {
					callback.accept(names);
				} catch (Throwable throwable) {
					Diagnostics.exception("profiles.callback", throwable);
				}
			});
		});
	}

	private static List<String> namesNow() {
		List<String> names = new ArrayList<>();
		Path directory = directory();
		if (!Files.isDirectory(directory)) {
			return names;
		}
		try (var stream = Files.newDirectoryStream(directory, "*.json")) {
			for (Path path : stream) {
				String file = path.getFileName().toString();
				names.add(file.substring(0, file.length() - ".json".length()));
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("profiles.list", throwable);
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	/** Saves the current modules and HUD layout under a profile name. */
	public static void saveAsync(String rawName, Consumer<String> result) {
		String name = sanitizeName(rawName);
		if (name == null) {
			report(result, "Profile name must be 1-" + MAX_NAME_LENGTH + " letters, digits, spaces, - or _.");
			return;
		}
		if (isBuiltIn(name)) {
			report(result, "The built-in profile '" + name + "' cannot be overwritten.");
			return;
		}
		// Snapshot on the calling (client) thread: the live config is mutated by ticks and
		// screens, and deepCopying it from the IO thread raced those writes.
		JsonObject root = new JsonObject();
		try {
			OmniCubeConfig config = OmniCubeClient.config();
			root.addProperty("version", OmniCubeConfig.CURRENT_VERSION);
			root.add("modules", config.modules.deepCopy());
			root.add("hud", config.hud.deepCopy());
		} catch (Throwable throwable) {
			Diagnostics.exception("profiles.snapshot", throwable);
			report(result, "Could not read the current settings: " + Diagnostics.describe(throwable));
			return;
		}
		IO.submit(() -> {
			try {
				Path path = directory().resolve(name + ".json");
				writeAtomic(path, GSON.toJson(root));
				lastResult = "saved profile '" + name + "'";
				Diagnostics.event("profiles", lastResult);
				report(result, "Saved profile '" + name + "'.");
			} catch (Throwable throwable) {
				Diagnostics.exception("profiles.save", throwable);
				report(result, "Could not save profile '" + name + "': " + Diagnostics.describe(throwable));
			}
		});
	}

	/** Loads a built-in or saved profile; the settings are applied on the client thread. */
	public static void loadAsync(String rawName, Consumer<String> result) {
		String name = sanitizeName(rawName);
		if (name == null) {
			report(result, "Unknown profile.");
			return;
		}
		if (isBuiltIn(name)) {
			Minecraft.getInstance().execute(() -> {
				try {
					applyBuiltIn(name);
					lastResult = "applied built-in profile '" + name + "'";
					Diagnostics.event("profiles", lastResult);
					report(result, "Applied the built-in '" + name + "' profile.");
				} catch (Throwable throwable) {
					Diagnostics.exception("profiles.builtin", throwable);
					report(result, "Could not apply '" + name + "': " + Diagnostics.describe(throwable));
				}
			});
			return;
		}
		IO.submit(() -> {
			JsonObject root = null;
			String failure = null;
			try {
				Path path = directory().resolve(name + ".json");
				if (!Files.isRegularFile(path)) {
					failure = "Profile '" + name + "' does not exist.";
				} else {
					try (BufferedReader reader = Files.newBufferedReader(path)) {
						root = GSON.fromJson(reader, JsonObject.class);
					}
					if (root == null || !root.has("modules") || !root.get("modules").isJsonObject()
							|| !root.has("hud") || !root.get("hud").isJsonObject()) {
						root = null;
						failure = "Profile '" + name + "' is damaged; your current settings were kept.";
						Diagnostics.exception("profiles.load", new IOException(failure));
					}
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("profiles.load", throwable);
				failure = "Could not read profile '" + name + "': " + Diagnostics.describe(throwable);
			}
			JsonObject profile = root;
			String message = failure;
			Minecraft.getInstance().execute(() -> {
				if (profile == null) {
					report(result, message == null ? "Profile could not be loaded." : message);
					return;
				}
				try {
					OmniCubeConfig config = OmniCubeClient.config();
					config.modules = profile.getAsJsonObject("modules").deepCopy();
					config.hud = profile.getAsJsonObject("hud").deepCopy();
					OmniCubeClient.applyConfigSections();
					lastResult = "loaded profile '" + name + "'";
					Diagnostics.event("profiles", lastResult);
					report(result, "Loaded profile '" + name + "'.");
				} catch (Throwable throwable) {
					Diagnostics.exception("profiles.apply", throwable);
					report(result, "Could not apply profile '" + name + "': " + Diagnostics.describe(throwable));
				}
			});
		});
	}

	public static void deleteAsync(String rawName, Consumer<String> result) {
		String name = sanitizeName(rawName);
		if (name == null || isBuiltIn(name)) {
			report(result, "Built-in profiles cannot be deleted.");
			return;
		}
		IO.submit(() -> {
			try {
				Path path = directory().resolve(name + ".json");
				boolean deleted = Files.deleteIfExists(path);
				lastResult = deleted ? "deleted profile '" + name + "'" : "profile '" + name + "' was not found";
				Diagnostics.event("profiles", lastResult);
				report(result, deleted ? "Deleted profile '" + name + "'." : "Profile '" + name + "' was not found.");
			} catch (Throwable throwable) {
				Diagnostics.exception("profiles.delete", throwable);
				report(result, "Could not delete profile '" + name + "': " + Diagnostics.describe(throwable));
			}
		});
	}

	public static void renameAsync(String rawOld, String rawNew, Consumer<String> result) {
		String oldName = sanitizeName(rawOld);
		String newName = sanitizeName(rawNew);
		if (oldName == null || newName == null || isBuiltIn(oldName) || isBuiltIn(newName)) {
			report(result, "Choose a saved profile and a new name (built-in profiles cannot be renamed).");
			return;
		}
		IO.submit(() -> {
			try {
				Path from = directory().resolve(oldName + ".json");
				Path to = directory().resolve(newName + ".json");
				if (!Files.isRegularFile(from)) {
					report(result, "Profile '" + oldName + "' was not found.");
					return;
				}
				if (Files.exists(to)) {
					report(result, "A profile named '" + newName + "' already exists.");
					return;
				}
				Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
				lastResult = "renamed profile '" + oldName + "' to '" + newName + "'";
				Diagnostics.event("profiles", lastResult);
				report(result, "Renamed '" + oldName + "' to '" + newName + "'.");
			} catch (Throwable throwable) {
				Diagnostics.exception("profiles.rename", throwable);
				report(result, "Could not rename the profile: " + Diagnostics.describe(throwable));
			}
		});
	}

	/** Applies one of the four built-in profiles. Runs on the client thread. */
	public static void applyBuiltIn(String name) {
		String key = name.toLowerCase(Locale.ROOT);
		switch (key) {
			case "low-end" -> OmniCubeClient.applyLowEndPreset();
			case "streaming" -> applyStreaming();
			case "pvp" -> applyPvp();
			default -> resetToDefaults();
		}
	}

	private static void resetToDefaults() {
		for (Feature feature : OmniCubeClient.features()) {
			for (var setting : feature.settings()) {
				setting.reset();
			}
			feature.setEnabled(defaultStateOf(feature));
		}
		// The Default profile also restores the stock HUD layout; it used to leave
		// whatever positions the player had dragged their elements to.
		for (dev.omnicube.client.hud.HudElement element : OmniCubeClient.hud().elements()) {
			element.resetPosition();
		}
		// The features were changed directly, so persist them instead of re-reading
		// the (still stale) saved modules section, which would undo the profile.
		OmniCubeClient.refreshHudMasterFlags();
		OmniCubeClient.saveConfig();
		Diagnostics.event("profiles", "reset every feature to its defaults");
	}

	private static boolean defaultStateOf(Feature feature) {
		if (feature instanceof dev.omnicube.client.module.Module module) {
			return module.defaultEnabled();
		}
		if (feature instanceof dev.omnicube.client.hud.HudElement element) {
			return element.defaultEnabled();
		}
		return false;
	}

	private static void applyStreaming() {
		set("streamer_mode", true);
		set("better_chat", true);
		set("chat_log", false);
		set("hud_server", false);
		set("hud_coords", true);
		set("performance", false);
		OmniCubeClient.refreshHudMasterFlags();
		OmniCubeClient.saveConfig();
		Diagnostics.event("profiles", "applied the Streaming profile");
	}

	private static void applyPvp() {
		set("crosshair", true);
		set("auto_sprint", true);
		set("hud_fps", true);
		set("hud_ping", true);
		set("name_tags", false);
		set("world_markers", false);
		set("chat_log", false);
		set("hud_entities", false);
		set("hud_session", false);
		set("hud_clock", false);
		set("hud_timer", false);
		OmniCubeClient.refreshHudMasterFlags();
		OmniCubeClient.saveConfig();
		Diagnostics.event("profiles", "applied the PvP profile");
	}

	private static void set(String id, boolean enabled) {
		Feature feature = OmniCubeClient.featureById(id);
		if (feature != null && feature.isAvailable()) {
			feature.setEnabled(enabled);
		}
	}

	private static String sanitizeName(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.strip();
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			return null;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			boolean allowed = Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
			if (!allowed) {
				return null;
			}
		}
		return name;
	}

	private static void writeAtomic(Path path, String json) throws IOException {
		Files.createDirectories(path.getParent());
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temp)) {
			writer.write(json);
		}
		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailure) {
			Diagnostics.exception("profiles.atomic", atomicFailure);
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void report(Consumer<String> callback, String message) {
		Diagnostics.event("profiles", message);
		if (callback == null) {
			return;
		}
		Minecraft.getInstance().execute(() -> {
			try {
				callback.accept(message);
			} catch (Throwable throwable) {
				Diagnostics.exception("profiles.report", throwable);
			}
		});
	}

	static {
		try {
			OmniCubeMod.LOGGER.debug("[OmniCube] Profiles stored in {}.", directory());
		} catch (Throwable throwable) {
			// Logging must never stop class initialisation.
		}
	}
}
