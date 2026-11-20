package dev.omnicube.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.hud.HudManager;
import dev.omnicube.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final String CONFIG_FILE = "omnicube.json";
	private static final String TEMP_FILE = "omnicube.json.tmp";

	/**
	 * Pre-rename config filename. This is the only old name kept in code because
	 * it is required to find and migrate an existing config exactly once.
	 */
	private static final String LEGACY_BACKUP_SUFFIX = ".pre-omnicube.bak";

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
				record(path.getFileName() + " was unreadable; recovered settings from "
						+ LEGACY_CONFIG_FILE, true);
				return recovered;
			}
			record(path.getFileName() + " was unreadable; backed up and reset to defaults", true);
			return new OmniCubeConfig();
		}

		OmniCubeConfig migrated = loadLegacy(path);
		if (migrated != null) {
			record("migrated " + LEGACY_CONFIG_FILE + " to " + CONFIG_FILE, false);
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
		Path legacy = target.resolveSibling(LEGACY_CONFIG_FILE);
		if (!Files.exists(legacy)) {
			return null;
		}
		OmniCubeConfig migrated = read(legacy);
		if (migrated == null) {
			backupCorruptFile(legacy);
			record(LEGACY_CONFIG_FILE + " was unreadable; backed up and defaults used", true);
			return null;
		}
		migrateLegacy(legacy, target, migrated);
		return migrated;
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
		Path backup = legacy.resolveSibling(LEGACY_CONFIG_FILE + LEGACY_BACKUP_SUFFIX);

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

	public static void save(OmniCubeConfig config, ModuleManager modules, HudManager hud) {
		Path path = configPath();
		try {
			config.sanitize();
			modules.write(config.modules);
			hud.write(config.hud);
			write(path, config);
		} catch (Throwable throwable) {
			// A failed save must never break a screen, a tick or the shutdown hook.
			Diagnostics.exception("config.save", throwable);
			OmniCubeMod.LOGGER.error("[OmniCube] Could not save the config to {}.", path, throwable);
		}
	}

	private static void write(Path path, OmniCubeConfig config) throws IOException {
		Path temp = path.resolveSibling(TEMP_FILE);
		config.sanitize();
		Files.createDirectories(path.getParent());

		try (Writer writer = Files.newBufferedWriter(temp)) {
			GSON.toJson(config, writer);
		}

		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailure) {
			Diagnostics.exception("config.save.atomic", atomicFailure);
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
