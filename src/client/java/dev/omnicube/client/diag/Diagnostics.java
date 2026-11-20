package dev.omnicube.client.diag;

import dev.omnicube.OmniCubeMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * The single diagnostics sink for OmniCube.
 *
 * <p>Rules this class follows strictly:
 * <ul>
 *     <li>Nothing is written to disk on the render thread. Runtime lines are queued and
 *     written by one daemon thread.</li>
 *     <li>Every method is exception-proof: diagnostics can never break the game, even if
 *     the disk is full or the game directory is read-only.</li>
 *     <li>Repeated exceptions are logged once with a repeat counter instead of spamming.</li>
 *     <li>Every line passes through the redactor, so Streamer Mode also hides addresses in
 *     this file.</li>
 * </ul>
 */
public final class Diagnostics {
	public static final String FILE_NAME = "omnicube-diagnostics.log";
	public static final String PREVIOUS_FILE_NAME = "omnicube-diagnostics-prev.log";

	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final int MAX_QUEUED_LINES = 4096;
	private static final String FLUSH_MARKER = "\u0000flush";
	private static final String STOP_MARKER = "\u0000stop";
	private static final String SNAPSHOT_MARKER = "\u0000snapshot";

	private static final BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>(MAX_QUEUED_LINES);
	private static final AtomicBoolean STARTED = new AtomicBoolean();
	private static final AtomicBoolean DROP_WARNED = new AtomicBoolean();
	private static final AtomicBoolean SNAPSHOT_WRITTEN = new AtomicBoolean();
	private static final AtomicLong DROPPED = new AtomicLong();
	private static final Map<String, FeatureRecord> FEATURES = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final Map<String, ExceptionRecord> EXCEPTIONS = new ConcurrentHashMap<>();
	private static final List<String> PRE_START_LINES = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> MOD_LINES = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> CONFIG_LINES = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> COLLISION_LINES = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> MIXIN_LINES = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> PERFORMANCE_LINES = Collections.synchronizedList(new ArrayList<>());

	private static volatile Path logPath;
	private static volatile Path previousPath;
	private static volatile String preamble;
	private static volatile BufferedWriter writer;
	private static volatile Thread writerThread;
	private static volatile boolean stopping;
	private static volatile Function<String, String> redactor = Function.identity();
	private static volatile String environmentSummary = "unavailable";

	private Diagnostics() {
	}

	/** Immutable snapshot of one feature row shown in the log, the GUI and /omnicube diag. */
	public record FeatureRecord(String id, String name, String kind, String state, String reason, String time) {
		public boolean enabled() {
			return "ENABLED".equals(state);
		}

		public boolean failed() {
			return "FAILED".equals(state);
		}

		public boolean blocked() {
			return "BLOCKED".equals(state);
		}
	}

	private static final class ExceptionRecord {
		private final String feature;
		private final String type;
		private final String message;
		private final String firstStack;
		private int count;
		private String lastTime;

		private ExceptionRecord(String feature, String type, String message, String firstStack, String time) {
			this.feature = feature;
			this.type = type;
			this.message = message;
			this.firstStack = firstStack;
			this.count = 1;
			this.lastTime = time;
		}

		private synchronized int increment(String time) {
			count++;
			lastTime = time;
			return count;
		}
	}

	/** Installs the Streamer Mode redactor. */
	public static void installRedactor(Function<String, String> newRedactor) {
		redactor = newRedactor == null ? Function.identity() : newRedactor;
	}

	public static Path logPath() {
		return logPath == null ? fallbackPath() : logPath;
	}

	public static Path logDirectory() {
		Path path = logPath();
		return path.getParent() == null ? path : path.getParent();
	}

	private static Path fallbackPath() {
		try {
			return FabricLoader.getInstance().getGameDir().resolve("logs").resolve(FILE_NAME);
		} catch (Throwable throwable) {
			return Path.of("logs", FILE_NAME);
		}
	}

	/**
	 * Resolves the log paths and collects environment/mod data. No disk access happens
	 * here: rotation, opening the file and every write happen on the background writer
	 * thread, so the client/render thread never touches the disk.
	 */
	public static void start() {
		if (!STARTED.compareAndSet(false, true)) {
			return;
		}
		try {
			Path path = FabricLoader.getInstance().getGameDir().resolve("logs").resolve(FILE_NAME);
			logPath = path;
			previousPath = path.resolveSibling(PREVIOUS_FILE_NAME);
		} catch (Throwable throwable) {
			logPath = fallbackPath();
			previousPath = logPath.resolveSibling(PREVIOUS_FILE_NAME);
			OmniCubeMod.LOGGER.warn("[OmniCube] Diagnostics log path could not be resolved; using {}.", logPath, throwable);
		}

		collectEnvironment();
		collectMods();
	}

	private static void collectEnvironment() {
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("OmniCube: ").append(modVersion("omnicube")).append('\n');
			builder.append("Minecraft: ").append(modVersion("minecraft")).append('\n');
			builder.append("Fabric Loader: ").append(safeVersion("fabricloader")).append('\n');
			builder.append("Fabric API: ").append(modVersion("fabric-api")).append('\n');
			builder.append("Java: ").append(System.getProperty("java.version", "?"))
					.append(" (").append(System.getProperty("java.vendor", "?")).append(")\n");
			builder.append("Java VM: ").append(System.getProperty("java.vm.name", "?"))
					.append(' ').append(System.getProperty("java.vm.version", "?")).append('\n');
			builder.append("OS: ").append(System.getProperty("os.name", "?"))
					.append(' ').append(System.getProperty("os.version", "?"))
					.append(" (").append(System.getProperty("os.arch", "?")).append(")\n");
			builder.append("Locale: ").append(Locale.getDefault()).append('\n');
			long maxHeap = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
			long totalHeap = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
			builder.append("RAM (JVM): max ").append(maxHeap).append(" MB, current ").append(totalHeap).append(" MB\n");
			builder.append("CPU: ").append(System.getProperty("os.arch", "?"))
					.append(", ").append(Runtime.getRuntime().availableProcessors()).append(" thread(s)\n");
			builder.append("GPU: reported by the launcher only; see the vanilla log for the GL renderer line\n");
			builder.append("Launcher: ").append(detectLauncher()).append('\n');
			builder.append("Render thread rule: log writing happens on a background thread\n");
			environmentSummary = builder.toString();
		} catch (Throwable throwable) {
			environmentSummary = "environment collection failed: " + describe(throwable);
		}
	}

	private static String detectLauncher() {
		try {
			String command = ProcessHandle.current().info().commandLine().orElse("");
			String lower = command.toLowerCase(Locale.ROOT);
			if (lower.contains("prism")) {
				return "Prism Launcher";
			}
			if (lower.contains("multimc")) {
				return "MultiMC";
			}
			if (lower.contains("curseforge")) {
				return "CurseForge";
			}
			if (lower.contains("modrinth")) {
				return "Modrinth App";
			}
			if (lower.contains("gdlauncher")) {
				return "GDLauncher";
			}
			if (lower.contains("gradle")) {
				return "development (Gradle)";
			}
			return "not detected";
		} catch (Throwable throwable) {
			return "not detectable";
		}
	}

	private static void collectMods() {
		try {
			for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
				ModMetadata metadata = container.getMetadata();
				MOD_LINES.add(metadata.getId() + " " + metadata.getVersion().getFriendlyString()
						+ " (" + metadata.getName() + ")");
			}
			MOD_LINES.sort(String.CASE_INSENSITIVE_ORDER);
		} catch (Throwable throwable) {
			MOD_LINES.add("could not list loaded mods: " + describe(throwable));
		}
	}

	private static String modVersion(String id) {
		try {
			return FabricLoader.getInstance().getModContainer(id)
					.map(container -> container.getMetadata().getVersion().getFriendlyString())
					.orElse("not installed");
		} catch (Throwable throwable) {
			return "unknown";
		}
	}

	private static String safeVersion(String id) {
		return modVersion(id);
	}

	// ------------------------------------------------------------------
	// Startup sections
	// ------------------------------------------------------------------

	public static void configLine(String text) {
		if (text != null) {
			CONFIG_LINES.add(text);
		}
	}

	public static void collisionLine(String text) {
		if (text != null) {
			COLLISION_LINES.add(text);
		}
	}

	public static void mixinLine(String text) {
		if (text != null) {
			MIXIN_LINES.add(text);
		}
	}

	public static void performanceLine(String text) {
		if (text != null) {
			PERFORMANCE_LINES.add(text);
		}
	}

	/**
	 * Writes the ordered startup report once and then starts the live background writer.
	 * Called from the client initializer, never from rendering code.
	 */
	public static void publishStartupReport() {
		start();
		if (!SNAPSHOT_WRITTEN.compareAndSet(false, true)) {
			return;
		}
		try {
			StringBuilder snapshot = new StringBuilder(8192);
			snapshot.append("OmniCube diagnostics\n");
			snapshot.append("Generated: ").append(java.time.LocalDateTime.now()).append('\n');
			appendSection(snapshot, "1. ENVIRONMENT", lines(environmentSummary));
			appendSection(snapshot, "2. MODS DETECTED", copyOf(MOD_LINES));
			appendSection(snapshot, "3. FEATURE STATUS TABLE", featureTableLines());
			appendSection(snapshot, "4. COLLISIONS AND CONFLICTS", copyOf(COLLISION_LINES));
			appendSection(snapshot, "5. MIXIN REPORT", copyOf(MIXIN_LINES));
			appendSection(snapshot, "6. EXCEPTIONS", exceptionTableLines());
			appendSection(snapshot, "7. CONFIG", copyOf(CONFIG_LINES));
			appendSection(snapshot, "8. PERFORMANCE", copyOf(PERFORMANCE_LINES));
			appendSection(snapshot, "9. RUNTIME EVENTS", preStartLines());
			snapshot.append('\n');
			preamble = snapshot.toString();
		} catch (Throwable throwable) {
			preamble = "OmniCube diagnostics\n(failed to build the startup report: "
					+ describe(throwable) + ")\n";
		}

		Thread thread = new Thread(Diagnostics::writeLoop, "OmniCube-Diagnostics");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		writerThread = thread;
		thread.start();
	}

	/** Copies a concurrent list so a report never races the writer. */
	private static List<String> copyOf(List<String> source) {
		try {
			synchronized (source) {
				return new ArrayList<>(source);
			}
		} catch (Throwable throwable) {
			return List.of();
		}
	}

	private static List<String> preStartLines() {
		synchronized (PRE_START_LINES) {
			if (PRE_START_LINES.isEmpty()) {
				return List.of("(no events recorded before the report was written)");
			}
			return new ArrayList<>(PRE_START_LINES);
		}
	}

	private static List<String> lines(String block) {
		if (block == null || block.isBlank()) {
			return List.of("(none)");
		}
		return List.of(block.split("\\R"));
	}

	private static void appendSection(StringBuilder builder, String title, List<String> content) {
		builder.append('\n').append("===== ").append(title).append(" =====").append('\n');
		if (content == null || content.isEmpty()) {
			builder.append("(none)").append('\n');
			return;
		}
		for (String line : content) {
			if (line != null) {
				builder.append(redact(line)).append('\n');
			}
		}
	}

	// ------------------------------------------------------------------
	// Live log
	// ------------------------------------------------------------------

	/** Rotation, file creation and every write happen here, never on the render thread. */
	private static void writeLoop() {
		openLog();
		String header = preamble;
		if (header != null) {
			writeLine(header.stripTrailing());
			flushWriter();
		}
		while (true) {
			String line;
			try {
				line = QUEUE.take();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			}
			if (STOP_MARKER.equals(line)) {
				drainToWriter();
				closeWriter();
				return;
			}
			if (FLUSH_MARKER.equals(line) || SNAPSHOT_MARKER.equals(line)) {
				flushWriter();
				continue;
			}
			writeLine(line);
			// Flushing per line keeps the file usable while the game is running and costs
			// nothing at OmniCube's event volume.
			flushWriter();
		}
	}

	private static void openLog() {
		try {
			Path path = logPath();
			Files.createDirectories(path.getParent());
			if (Files.exists(path)) {
				try {
					Files.move(path, path.resolveSibling(PREVIOUS_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
				} catch (Throwable throwable) {
					OmniCubeMod.LOGGER.warn("[OmniCube] Could not rotate the previous diagnostics log.", throwable);
				}
			}
			writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not open the diagnostics log at {}.", logPath(), throwable);
			writer = null;
		}
	}

	private static void drainToWriter() {
		String line;
		while ((line = QUEUE.poll()) != null) {
			if (FLUSH_MARKER.equals(line) || SNAPSHOT_MARKER.equals(line) || STOP_MARKER.equals(line)) {
				continue;
			}
			writeLine(line);
		}
	}

	private static void writeLine(String line) {
		BufferedWriter out = writer;
		if (out == null) {
			synchronized (PRE_START_LINES) {
				if (PRE_START_LINES.size() < MAX_QUEUED_LINES) {
					PRE_START_LINES.add(line);
				}
			}
			return;
		}
		try {
			out.write(line);
			out.write('\n');
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.warn("[OmniCube] Diagnostics write failed; switching to vanilla logging only.", throwable);
			closeWriter();
		}
	}

	private static void flushWriter() {
		BufferedWriter out = writer;
		if (out == null) {
			return;
		}
		try {
			out.flush();
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.debug("[OmniCube] Diagnostics flush failed.", throwable);
			closeWriter();
		}
	}

	private static void closeWriter() {
		BufferedWriter out = writer;
		writer = null;
		if (out == null) {
			return;
		}
		try {
			out.flush();
			out.close();
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.debug("[OmniCube] Diagnostics close failed.", throwable);
		}
	}

	private static void enqueue(String raw) {
		try {
			if (raw == null) {
				return;
			}
			String line = redact(raw);
			if (!STARTED.get()) {
				synchronized (PRE_START_LINES) {
					if (PRE_START_LINES.size() < MAX_QUEUED_LINES) {
						PRE_START_LINES.add(line);
						return;
					}
				}
			}
			if (!QUEUE.offer(line)) {
				DROPPED.incrementAndGet();
				if (DROP_WARNED.compareAndSet(false, true)) {
					OmniCubeMod.LOGGER.warn("[OmniCube] Diagnostics queue is full; some lines are dropped.");
				}
			}
		} catch (Throwable ignored) {
			// Logging must never throw.
		}
	}

	/** Live event line: "12:00:01.020 [world] joined world". */
	public static void event(String category, String message) {
		try {
			enqueue(timestamp() + " [" + (category == null ? "event" : category) + "] "
					+ (message == null ? "" : message));
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	public static void section(String title) {
		try {
			enqueue("");
			enqueue("--- " + (title == null ? "" : title) + " ---");
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	/** Records the current state of one feature and appends a state-change line. */
	public static void status(String id, String name, String kind, String state, String reason) {
		if (id == null) {
			return;
		}
		try {
			FeatureRecord record = new FeatureRecord(id, name == null ? id : name, kind == null ? "feature" : kind,
					state == null ? "DISABLED" : state, reason == null ? "" : reason, timestamp());
			FEATURES.put(id, record);
			enqueue(record.time() + " [feature] " + record.id() + " | " + record.state() + " | " + record.reason());
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	public static FeatureRecord record(String id) {
		return id == null ? null : FEATURES.get(id);
	}

	public static List<FeatureRecord> features() {
		synchronized (FEATURES) {
			return new ArrayList<>(FEATURES.values());
		}
	}

	public static int failedFeatures() {
		int failed = 0;
		for (FeatureRecord record : features()) {
			if (record.failed()) {
				failed++;
			}
		}
		return failed;
	}

	public static void exception(String feature, Throwable throwable) {
		if (throwable == null) {
			return;
		}
		try {
			String owner = feature == null || feature.isBlank() ? "unknown" : feature;
			String type = throwable.getClass().getName();
			String message = throwable.getMessage() == null ? "" : throwable.getMessage();
			String key = owner + "|" + type + "|" + message;
			ExceptionRecord record = EXCEPTIONS.get(key);
			if (record == null) {
				record = new ExceptionRecord(owner, type, message, stackTrace(throwable), timestamp());
				ExceptionRecord existing = EXCEPTIONS.putIfAbsent(key, record);
				if (existing != null) {
					existing.increment(timestamp());
					return;
				}
				enqueue("");
				enqueue(record.lastTime + " [exception] " + owner + ": " + type
						+ (message.isEmpty() ? "" : ": " + message));
				enqueue(record.firstStack);
				enqueue("");
			} else {
				int count = record.increment(timestamp());
				enqueue(timestamp() + " [exception] repeat #" + count + " " + owner + ": " + type
						+ (message.isEmpty() ? "" : ": " + message));
			}
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	public static int exceptionCount() {
		int count = 0;
		for (ExceptionRecord record : EXCEPTIONS.values()) {
			count += record.count;
		}
		return count;
	}

	public static void performance(String message) {
		try {
			enqueue(timestamp() + " [performance] " + (message == null ? "" : message));
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	/** Requests a flush without blocking; safe from any thread. */
	public static void flush() {
		try {
			if (writerThread != null) {
				QUEUE.offer(FLUSH_MARKER);
			}
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	/** Final flush; call only from shutdown, disconnect or crash handlers. */
	public static void flushForShutdown() {
		try {
			if (stopping) {
				return;
			}
			stopping = true;
			if (writerThread == null) {
				// Nothing was published; write a minimal report so the file always exists.
				publishStartupReport();
			}
			appendShutdownState();
			QUEUE.offer(STOP_MARKER);
			Thread thread = writerThread;
			if (thread != null && thread != Thread.currentThread()) {
				thread.join(1500L);
			}
		} catch (Throwable throwable) {
			OmniCubeMod.LOGGER.debug("[OmniCube] Could not flush diagnostics on shutdown.", throwable);
		}
	}

	private static void appendShutdownState() {
		try {
			enqueue("");
			enqueue("--- shutdown snapshot " + timestamp() + " ---");
			for (String line : featureTableLines()) {
				enqueue(line);
			}
			enqueue("exceptions total: " + exceptionCount());
			enqueue("dropped log lines: " + DROPPED.get());
		} catch (Throwable ignored) {
			// Never throw from diagnostics.
		}
	}

	private static List<String> featureTableLines() {
		List<FeatureRecord> records = features();
		List<String> lines = new ArrayList<>();
		lines.add("name | state | reason | time");
		if (records.isEmpty()) {
			lines.add("(no features registered)");
			return lines;
		}
		for (FeatureRecord record : records) {
			lines.add(record.name() + " | " + record.state() + " | "
					+ (record.reason().isBlank() ? "-" : record.reason()) + " | " + record.time());
		}
		return lines;
	}

	private static List<String> exceptionTableLines() {
		List<String> lines = new ArrayList<>();
		if (EXCEPTIONS.isEmpty()) {
			lines.add("(no exceptions recorded)");
			return lines;
		}
		lines.add("feature | exception | repeats");
		for (ExceptionRecord record : EXCEPTIONS.values()) {
			lines.add(record.feature + " | " + record.type
					+ (record.message.isEmpty() ? "" : ": " + record.message)
					+ " | " + record.count);
		}
		return lines;
	}

	/**
	 * Full text report used by the "Copy report" button and /omnicube diag. Built from
	 * memory so it never touches the disk.
	 */
	public static String report() {
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("OmniCube diagnostics report").append('\n');
			builder.append("Log file: ").append(logPath()).append('\n');
			builder.append("Previous log: ").append(previousPath == null ? "none" : previousPath).append('\n');
			builder.append('\n').append("ENVIRONMENT").append('\n').append(environmentSummary).append('\n');
			builder.append("FEATURE STATUS").append('\n');
			for (String line : featureTableLines()) {
				builder.append("  ").append(redact(line)).append('\n');
			}
			builder.append('\n').append("EXCEPTIONS").append('\n');
			for (String line : exceptionTableLines()) {
				builder.append("  ").append(redact(line)).append('\n');
			}
			builder.append('\n').append("MIXINS").append('\n');
			for (String line : copyOf(MIXIN_LINES)) {
				builder.append("  ").append(redact(line)).append('\n');
			}
			builder.append('\n').append("CONFLICTS").append('\n');
			for (String line : copyOf(COLLISION_LINES)) {
				builder.append("  ").append(redact(line)).append('\n');
			}
			builder.append('\n').append("CONFIG").append('\n');
			for (String line : copyOf(CONFIG_LINES)) {
				builder.append("  ").append(redact(line)).append('\n');
			}
			builder.append('\n').append("PERFORMANCE").append('\n');
			for (String line : copyOf(PERFORMANCE_LINES)) {
				builder.append("  ").append(redact(line)).append('\n');
			}
		} catch (Throwable throwable) {
			return "OmniCube diagnostics report could not be built: " + describe(throwable);
		}
		return builder.toString();
	}

	/** Compact lines for /omnicube diag. */
	public static List<String> summaryLines() {
		List<String> lines = new ArrayList<>();
		int enabled = 0;
		int disabled = 0;
		int blocked = 0;
		int failed = 0;
		for (FeatureRecord record : features()) {
			switch (record.state()) {
				case "ENABLED" -> enabled++;
				case "BLOCKED" -> blocked++;
				case "FAILED" -> failed++;
				default -> disabled++;
			}
		}
		lines.add("Features: " + enabled + " enabled, " + disabled + " disabled, "
				+ blocked + " blocked, " + failed + " failed");
		lines.add("Exceptions logged: " + exceptionCount());
		if (failed > 0) {
			for (FeatureRecord record : features()) {
				if (record.failed()) {
					lines.add("FAILED " + record.id() + ": " + record.reason());
				}
			}
		}
		lines.add("Log: " + logPath());
		lines.add("Previous: " + (previousPath == null ? "none" : previousPath));
		return lines;
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private static final ThreadLocal<Boolean> REDACTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public static String redact(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		// A redactor that fails may log, and logging redacts again; guard the recursion.
		if (Boolean.TRUE.equals(REDACTING.get())) {
			return text;
		}
		REDACTING.set(Boolean.TRUE);
		try {
			String redacted = redactor.apply(text);
			return redacted == null ? text : redacted;
		} catch (Throwable throwable) {
			return text;
		} finally {
			REDACTING.set(Boolean.FALSE);
		}
	}

	private static String timestamp() {
		try {
			return LocalTime.now().format(CLOCK);
		} catch (Throwable throwable) {
			return "--:--:--.---";
		}
	}

	public static String describe(Throwable throwable) {
		if (throwable == null) {
			return "unknown";
		}
		String message = throwable.getMessage();
		return throwable.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
	}

	private static String stackTrace(Throwable throwable) {
		try {
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
				throwable.printStackTrace(printWriter);
			}
			return stringWriter.toString().stripTrailing();
		} catch (Throwable failure) {
			return "stack trace unavailable: " + describe(failure);
		}
	}
}
