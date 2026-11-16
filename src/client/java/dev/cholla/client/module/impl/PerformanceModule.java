
import net.minecraft.client.Minecraft;

public final class PerformanceModule extends Module {
	private static OptimizationBackend backend = new BuiltinOptimizationBackend();

	private final Setting.Num targetFps = add(new Setting.Num("target_fps", "Target FPS", 60.0D, 30.0D, 240.0D, 10.0D));
	private final Setting.Num minRenderDistance = add(new Setting.Num("min_render_distance", "Minimum render distance", 4.0D, 2.0D, 16.0D, 1.0D));
	private final Setting.Bool adaptiveParticles = add(new Setting.Bool("adaptive_particles", "Reduce particles under load", true));
	private final Setting.Bool adaptiveRenderDistance = add(new Setting.Bool("adaptive_render_distance", "Reduce render distance under load", true));
	private final Setting.Bool adaptiveClouds = add(new Setting.Bool("adaptive_clouds", "Reduce clouds under load", true));
	private final Setting.Bool adaptiveEntityDistance = add(new Setting.Bool("adaptive_entity_distance", "Reduce entity distance under load", true));

	public PerformanceModule() {
		super("performance", "Performance", "Adaptive quality based on your FPS. Changes only vanilla video options and restores them when disabled.", ModuleCategory.PERFORMANCE, false);
	}

	/**
	 */
	public static void setBackend(OptimizationBackend newBackend) {
		if (newBackend != null) {
			backend = newBackend;
		}
	}

	public static String backendName() {
		return backend.id();
	}

	@Override
	protected void onEnable() {
		try {
			backend.capture(Minecraft.getInstance());
		} catch (Throwable throwable) {
		}
	}

	@Override
	protected void onDisable() {
		try {
			backend.restore(Minecraft.getInstance());
		} catch (Throwable throwable) {
		}
	}

	public void restoreForSession() {
		try {
			backend.restore(Minecraft.getInstance());
		} catch (Throwable throwable) {
		}
	}

	public void captureForSession() {
		try {
			backend.capture(Minecraft.getInstance());
		} catch (Throwable throwable) {
		}
	}

	@Override
	public void tick() {
		try {
			backend.tick(Minecraft.getInstance(), new OptimizationSettings(
					targetFps.intValue(),
					minRenderDistance.intValue(),
					adaptiveParticles.get(),
					adaptiveRenderDistance.get(),
					adaptiveClouds.get(),
					adaptiveEntityDistance.get()
			));
		} catch (Throwable throwable) {
			setEnabled(false);
		}
	}
}
