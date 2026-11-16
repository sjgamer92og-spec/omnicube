
public final class Stopwatch {
	private static long startedAt;
	private static long accumulated;
	private static boolean running;

	private Stopwatch() {
	}

	public static void start() {
		if (!running) {
			running = true;
			startedAt = System.currentTimeMillis();
		}
	}

	public static void stop() {
		if (running) {
			accumulated += System.currentTimeMillis() - startedAt;
			running = false;
		}
	}

	public static void reset() {
		running = false;
		accumulated = 0L;
		startedAt = 0L;
	}

	public static boolean isRunning() {
		return running;
	}

	public static long elapsedMillis() {
		return accumulated + (running ? System.currentTimeMillis() - startedAt : 0L);
	}
}
