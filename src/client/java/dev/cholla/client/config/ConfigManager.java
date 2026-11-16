
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

	private ConfigManager() {
	}

	public static Path configPath() {
	}

		Path path = configPath();
		if (!Files.exists(path)) {
		}

		try (BufferedReader reader = Files.newBufferedReader(path)) {
			if (config == null) {
			}
			config.sanitize();
			return config;
		} catch (Exception exception) {
			backupCorruptFile(path);
		}
	}

	private static void backupCorruptFile(Path path) {
		try {
			Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (IOException exception) {
		}
	}

		Path path = configPath();
		config.sanitize();
		modules.write(config.modules);
		hud.write(config.hud);

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(temp)) {
				GSON.toJson(config, writer);
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException atomicFailure) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
		}
	}
}
