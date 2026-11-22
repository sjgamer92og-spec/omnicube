package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Real full brightness.
 *
 * <p>In 1.21.11 the lightmap is computed in {@code lightmap.fsh} from the lightmap
 * UBO, where {@code BrightnessFactor = max(0, gamma - darknessScale)}. The gamma
 * option never leaves 0..1, and a texel with light level 0 stays 0 no matter how
 * high that factor is, which is why writing gamma cannot light a cave. The only
 * reliable client-side path is the light coordinate every vertex is built with,
 * packed by {@link LightTexture#pack(int, int)}: the mixin hands that call to this
 * module and the module returns fully lit coordinates while it is on.
 *
 * <p>The module caches its mode and lift values as plain ints so the hot lightmap
 * path allocates nothing and never reads a setting object. It never writes the
 * gamma option, except as a logged last resort when the lightmap hook is missing.
 */
public final class FullbrightModule extends Module {
	public static final int MODE_FULL = 0;
	public static final int MODE_GAMMA = 1;
	public static final int MODE_NIGHT_VISION = 2;
	/** Fully lit light coordinates (block 15, sky 15). */
	public static final int FULL_BRIGHT = (15 << 4) | (15 << 20);

	private static final List<String> MODES = List.of("Full", "Gamma", "Night vision");

	private final Setting.Choice mode = add(new Setting.Choice("mode", "Mode", MODES, "Full"));
	private final Setting.Num brightness = add(new Setting.Num("brightness", "Brightness", 100.0D, 10.0D, 100.0D, 5.0D));

	/** Cached hot-path state; -1 means "do not touch the lightmap". */
	private volatile int modeCode = -1;
	private volatile int lift;
	/** Last mode/brightness the cache was built from, so the tick can compare cheaply. */
	private String cachedMode = "";
	private int cachedBrightness = -1;
	private boolean hookChecked;
	private boolean hookApplied;
	/** Set only when the lightmap hook is missing; restored as soon as possible. */
	private Double fallbackGamma;

	public FullbrightModule() {
		super("fullbright", "Fullbright",
				"Makes night, caves and dark rooms fully bright by overriding the lightmap light values. "
						+ "Modes: Full, Gamma (soft) and Night vision style.",
				ModuleCategory.RENDER, false);
	}

	@Override
	protected void onEnable() {
		try {
			refreshCache();
			checkHookOnce();
			if (!hookApplied) {
				useGammaFallback();
			} else {
				restoreFallbackGamma();
			}
			publishStatus();
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.enable", throwable);
		}
	}

	@Override
	protected void onDisable() {
		modeCode = -1;
		restoreFallbackGamma();
		publishStatus();
	}

	@Override
	public void tick() {
		// Once per client tick: cheap, and it is how a changed mode or brightness is
		// picked up without touching settings from the render path.
		refreshCache();
		if (hookChecked && !hookApplied) {
			applyGammaFallback();
		}
	}

	/**
	 * Called from the lightmap mixin for every packed light coordinate. Returns a
	 * replacement coordinate, or -1 to leave vanilla's value alone.
	 */
	public int lightOverride(int blockLight, int skyLight) {
		if (modeCode < 0) {
			return -1;
		}
		if (modeCode == MODE_FULL) {
			return FULL_BRIGHT;
		}
		int block = Math.max(blockLight, lift);
		// Night vision style keeps a little sky tint so it does not look flat white.
		int sky = Math.max(skyLight, modeCode == MODE_NIGHT_VISION ? Math.max(0, lift - 2) : lift);
		return (block << 4) | (sky << 20);
	}

	private void refreshCache() {
		try {
			String currentMode = mode.get();
			int currentBrightness = (int) Math.round(brightness.get());
			if (currentMode.equals(cachedMode) && currentBrightness == cachedBrightness) {
				modeCode = isEnabled() ? modeCode : -1;
				if (isEnabled() && modeCode < 0) {
					modeCode = codeOf(currentMode);
				}
				return;
			}
			cachedMode = currentMode;
			cachedBrightness = currentBrightness;
			int code = codeOf(currentMode);
			double fraction = Math.max(0.0D, Math.min(1.0D, currentBrightness / 100.0D));
			lift = code == MODE_GAMMA
					? 1 + (int) Math.round(fraction * 10.0D)
					: 4 + (int) Math.round(fraction * 11.0D);
			modeCode = isEnabled() ? code : -1;
			publishStatus();
		} catch (Throwable throwable) {
			// A broken setting must never leave the light override half-applied.
			modeCode = -1;
			Diagnostics.exception("fullbright.cache", throwable);
		}
	}

	private static int codeOf(String name) {
		if ("Gamma".equals(name)) {
			return MODE_GAMMA;
		}
		if ("Night vision".equals(name)) {
			return MODE_NIGHT_VISION;
		}
		return MODE_FULL;
	}

	/** Checks the mixin once; a missing hook is reported, never silently ignored. */
	private void checkHookOnce() {
		if (hookChecked) {
			return;
		}
		hookChecked = true;
		try {
			for (Method method : LightTexture.class.getDeclaredMethods()) {
				String name = method.getName();
				if (name.endsWith("omnicube$fullbrightLight")) {
					hookApplied = true;
					break;
				}
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.hook", throwable);
		}
		if (!hookApplied) {
			Diagnostics.event("fullbright", "lightmap hook NOT applied; falling back to gamma mode");
			Diagnostics.collisionLine("fullbright: the lightmap hook was not applied (target changed or another mod "
					+ "replaced it); Gamma mode is used instead");
			Diagnostics.note("fullbright", "Fullbright: hook NOT applied - Gamma fallback");
		}
	}

	/** Last resort when the lightmap hook did not apply: raise the session gamma only. */
	private void useGammaFallback() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null) {
				return;
			}
			if (fallbackGamma == null) {
				fallbackGamma = client.options.gamma().get();
			}
			applyGammaFallback();
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.gamma_fallback", throwable);
		}
	}

	private void applyGammaFallback() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null || fallbackGamma == null) {
				return;
			}
			double target = Math.max(0.0D, Math.min(1.0D, brightness.get() / 100.0D));
			if (Math.abs(client.options.gamma().get() - target) > 0.001D) {
				client.options.gamma().set(target);
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.gamma_fallback.apply", throwable);
		}
	}

	private void restoreFallbackGamma() {
		if (fallbackGamma == null) {
			return;
		}
		try {
			Minecraft client = Minecraft.getInstance();
			if (client != null && client.options != null) {
				client.options.gamma().set(fallbackGamma);
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.gamma_fallback.restore", throwable);
		} finally {
			fallbackGamma = null;
		}
	}

	/** Kept for existing callers: restores anything the module may have changed. */
	public void restore() {
		modeCode = -1;
		restoreFallbackGamma();
	}

	/** One-line state used by the diagnostics log, the Diagnostics tab and /omnicube diag. */
	public String statusLine() {
		StringBuilder builder = new StringBuilder("Fullbright: ");
		builder.append(isEnabled() ? "ON" : "OFF");
		builder.append(" | mode ").append(mode.get());
		if (modeCode == MODE_FULL) {
			builder.append(" | light 15/15");
		} else if (modeCode >= 0) {
			builder.append(" | light floor ").append(lift);
		}
		builder.append(" | lightmap hook ").append(hookApplied ? "applied" : (hookChecked ? "missing" : "unchecked"));
		return builder.toString();
	}

	private void publishStatus() {
		try {
			Diagnostics.note("fullbright", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("fullbright.status", throwable);
		}
	}
}
