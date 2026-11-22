package dev.omnicube.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.hud.HudManager;
import dev.omnicube.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final String CONFIG_FILE = "omnicube.json";
	private static final String TEMP_FILE = "omnicube.json.tmp";

	private static final String LEGACY_BACKUP_SUFFIX = ".pre-omnicube.bak";

	/**
	 * Rapid setting changes (a slider drag, for example) only schedule one write:
	 * a later request replaces the generation, and a scheduled write whose
	 * generation is stale does nothing. Writes always run on this single daemon
	 * thread, never on the render thread.
	 */
	private static final long DEBOUNCE_MILLIS = 300L;
	private static final AtomicLong GENERATION = new AtomicLong();
	private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "OmniCube-Config-Writer");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	private static volatile String pendingJson;
	private static volatile String lastResult = "no save yet";

	private static volatile String loadSummary = "config load has not run yet";
	private static volatile boolean usedDefaults;

	private ConfigManager() {
	}

	public static Path configPath() {
		try {
			return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
		} catch (Throwable throwable) {
			Diagnostics.exception("config.path", throwable);
			return Path.of("config", CONFIG_FILE);
		}
	}

	/** Human readable result of the last load, shown by diagnostics and /omnicube diag. */
	public static String loadSummary() {
		return loadSummary;
	}

	/** Human readable result of the last background write. */
	public static String saveSummary() {
		return lastResult;
	}

	public static boolean usedDefaults() {
		return usedDefaults;
	}

	public static OmniCubeConfig load() {
		Path path = configPath();

		if (Files.exists(path)) {
			OmniCubeConfig config = read(path);
			if (config != null) {
				record("loaded " + path.getFileName(), false);
				return config;
			}
			// A damaged or empty omnicube.json is backed up, then a still-valid
			// legacy config is used instead of silently dropping the user's setup.
			backupCorruptFile(path);
			OmniCubeConfig recovered = loadLegacy(path);
			if (recovered != null) {
				record(path.getFileName() + " was unreadable; recovered settings from an earlier config", true);
				return recovered;
			}
			record(path.getFileName() + " was unreadable; backed up and reset to defaults", true);
			return new OmniCubeConfig();
		}

		OmniCubeConfig migrated = loadLegacy(path);
		if (migrated != null) {
			record("migrated an earlier config to " + CONFIG_FILE, false);
			return migrated;
		}
		record("no config found; created defaults", true);
		return new OmniCubeConfig();
	}

	private static void record(String summary, boolean defaults) {
		loadSummary = summary;
		usedDefaults = defaults;
		Diagnostics.configLine(summary + " (" + configPath() + ")");
		Diagnostics.event("config", summary);
	}

	private static OmniCubeConfig loadLegacy(Path target) {
		Path legacy = findLegacyConfig(target);
		if (legacy == null) {
			return null;
		}
		OmniCubeConfig migrated = read(legacy);
		if (migrated == null) {
			backupCorruptFile(legacy);
			record(legacy.getFileName() + " was unreadable; backed up and defaults used", true);
			return null;
		}
		migrateLegacy(legacy, target, migrated);
		return migrated;
	}

	/**
	 * Finds a config written by an older build under a different file name. The file
	 * is recognised by its shape rather than its name, so a rename never strands a
	 * player's settings, and no obsolete file name has to live in the source.
	 */
	private static Path findLegacyConfig(Path target) {
		Path directory = target.getParent();
		if (directory == null || !Files.isDirectory(directory)) {
			return null;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
			for (Path candidate : stream) {
				if (Files.isRegularFile(candidate) && !candidate.equals(target)
						&& looksLikeOmniCubeConfig(candidate)) {
					return candidate;
				}
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("config.legacy.scan", throwable);
		}
		return null;
	}

	private static boolean looksLikeOmniCubeConfig(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			return root != null && root.has("modules") && root.get("modules").isJsonObject()
					&& root.has("hud") && root.get("hud").isJsonObject();
		} catch (Throwable throwable) {
			// Not a config; a malformed file must not stop the search.
			return false;
		}
	}

	private static OmniCubeConfig read(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			OmniCubeConfig config = GSON.fromJson(reader, OmniCubeConfig.class);
			if (config == null) {
				Diagnostics.configLine(path.getFileName() + " was empty or JSON null");
				return null;
			}
			config.sanitize();
			return config;
		} catch (Exception exception) {
			Diagnostics.exception("config.read", exception);
			OmniCubeMod.LOGGER.error("[OmniCube] Could not read {}; starting from defaults.", path, exception);
			return null;
		}
	}

	private static void migrateLegacy(Path legacy, Path target, OmniCubeConfig config) {
		Path backup = legacy.resolveSibling(legacy.getFileName() + LEGACY_BACKUP_SUFFIX);

		try {
			Files.copy(legacy, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			Diagnostics.configLine("backed up the legacy config to " + backup.getFileName());
			OmniCubeMod.LOGGER.info("[OmniCube] Backed up the legacy config to {}.", backup.getFileName());
		} catch (IOException exception) {
			Diagnostics.exception("config.migrate.backup", exception);
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not back up the legacy config.", exception);
		}

		try {
			write(target, config);
			Diagnostics.configLine("wrote the migrated config to " + target.getFileName());
			OmniCubeMod.LOGGER.info("[OmniCube] Migrated the legacy config to {}.", target.getFileName());
		} catch (Exception exception) {
			Diagnostics.exception("config.migrate.write", exception);
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not write the migrated config to {}; it will be retried on the next launch.",
					target.getFileName(), exception);
		}
	}

	private static void backupCorruptFile(Path path) {
		try {
			Path backup = path.resolveSibling(path.getFileName() + ".broken-" + Instant.now().toEpochMilli());
			Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
			Diagnostics.configLine("unreadable " + path.getFileName() + " copied to " + backup.getFileName());
			OmniCubeMod.LOGGER.warn("[OmniCube] The unreadable config was copied to {}.", backup.getFileName());
		} catch (IOException exception) {
			Diagnostics.exception("config.backup", exception);
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not back up the unreadable config.", exception);
		}
	}

	/**
	 * Builds the JSON snapshot on the calling thread (it only reads in-memory state)
	 * and schedules the disk write on the background writer. Nothing here blocks on
	 * the disk, and repeated calls within the debounce window collapse into one write.
	 */
	public static void save(OmniCubeConfig config, ModuleManager modules, HudManager hud) {
		try {
			pendingJson = snapshot(config, modules, hud);
			long generation = GENERATION.incrementAndGet();
			WRITER.schedule(() -> writePending(generation), DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
		} catch (Throwable throwable) {
			// A failed save must never break a screen, a tick or the shutdown hook.
			Diagnostics.exception("config.save", throwable);
			OmniCubeMod.LOGGER.error("[OmniCube] Could not prepare the config save.", throwable);
		}
	}

	/** Serialises the live state into the exact JSON that will be written. */
	public static String snapshot(OmniCubeConfig config, ModuleManager modules, HudManager hud) {
		config.sanitize();
		modules.write(config.modules);
		hud.write(config.hud);
		return GSON.toJson(config);
	}

	private static void writePending(long generation) {
		if (generation != GENERATION.get()) {
			// A newer change was requested; that request owns the write.
			return;
		}
		String json = pendingJson;
		if (json == null) {
			return;
		}
		Path path = configPath();
		try {
			writeJson(path, json);
			lastResult = "saved " + path.getFileName() + " in the background";
			Diagnostics.note("config_save", "Config save: wrote " + path.getFileName() + " on a background thread");
		} catch (Throwable throwable) {
			lastResult = "FAILED to save " + path.getFileName() + ": " + Diagnostics.describe(throwable);
			Diagnostics.exception("config.save.async", throwable);
			Diagnostics.note("config_save", "Config save: FAILED - " + Diagnostics.describe(throwable));
			OmniCubeMod.LOGGER.error("[OmniCube] Could not save the config to {}.", path, throwable);
		}
	}

	/**
	 * Writes any pending snapshot and waits briefly. Called on shutdown/disconnect
	 * only, never from the render loop.
	 */
	public static void flush() {
		try {
			GENERATION.incrementAndGet();
			String json = pendingJson;
			if (json == null) {
				return;
			}
			Future<?> future = WRITER.submit(() -> {
				try {
					writeJson(configPath(), json);
					lastResult = "saved " + configPath().getFileName() + " on shutdown";
				} catch (Throwable throwable) {
					Diagnostics.exception("config.flush", throwable);
					OmniCubeMod.LOGGER.error("[OmniCube] Could not flush the config on shutdown.", throwable);
				}
			});
			future.get(2L, TimeUnit.SECONDS);
		} catch (Throwable throwable) {
			Diagnostics.exception("config.flush", throwable);
		}
	}

	private static void write(Path path, OmniCubeConfig config) throws IOException {
		config.sanitize();
		writeJson(path, GSON.toJson(config));
	}

	/** Atomic write: temp file in the same directory, then rename over the target. */
	private static void writeJson(Path path, String json) throws IOException {
		Path temp = path.resolveSibling(TEMP_FILE);
		Files.createDirectories(path.getParent());

		try (Writer writer = Files.newBufferedWriter(temp)) {
			writer.write(json);
		}

		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailure) {
			Diagnostics.exception("config.save.atomic", atomicFailure);
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
