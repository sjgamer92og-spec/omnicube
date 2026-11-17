package dev.omnicube.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.omnicube.OmniCubeMod;
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

	private ConfigManager() {
	}

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
	}

	public static OmniCubeConfig load() {
		Path path = configPath();

		if (Files.exists(path)) {
			OmniCubeConfig config = read(path);
			if (config == null) {
				backupCorruptFile(path);
				return new OmniCubeConfig();
			}
			return config;
		}

		Path legacy = path.resolveSibling(LEGACY_CONFIG_FILE);
		if (Files.exists(legacy)) {
			OmniCubeConfig migrated = read(legacy);
			if (migrated != null) {
				migrateLegacy(legacy, path, migrated);
				return migrated;
			}
			backupCorruptFile(legacy);
		}

		return new OmniCubeConfig();
	}

	private static OmniCubeConfig read(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			OmniCubeConfig config = GSON.fromJson(reader, OmniCubeConfig.class);
			if (config == null) {
				return null;
			}
			config.sanitize();
			return config;
		} catch (Exception exception) {
			OmniCubeMod.LOGGER.error("[OmniCube] Could not read {}; starting from defaults.", path, exception);
			return null;
		}
	}

	private static void migrateLegacy(Path legacy, Path target, OmniCubeConfig config) {
		Path backup = legacy.resolveSibling(LEGACY_CONFIG_FILE + LEGACY_BACKUP_SUFFIX);

		try {
			Files.copy(legacy, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			OmniCubeMod.LOGGER.info("[OmniCube] Backed up the legacy config to {}.", backup.getFileName());
		} catch (IOException exception) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not back up the legacy config.", exception);
		}

		try {
			write(target, config);
			OmniCubeMod.LOGGER.info("[OmniCube] Migrated the legacy config to {}.", target.getFileName());
		} catch (IOException exception) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not write the migrated config to {}; defaults will be used for this session.",
					target.getFileName(), exception);
		}
	}

	private static void backupCorruptFile(Path path) {
		try {
			Path backup = path.resolveSibling(path.getFileName() + ".broken-" + Instant.now().toEpochMilli());
			Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
			OmniCubeMod.LOGGER.warn("[OmniCube] The unreadable config was copied to {}.", backup.getFileName());
		} catch (IOException exception) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not back up the unreadable config.", exception);
		}
	}

	public static void save(OmniCubeConfig config, ModuleManager modules, HudManager hud) {
		Path path = configPath();
		config.sanitize();
		modules.write(config.modules);
		hud.write(config.hud);

		try {
			write(path, config);
		} catch (IOException exception) {
			OmniCubeMod.LOGGER.error("[OmniCube] Could not save the config to {}.", path, exception);
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
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
