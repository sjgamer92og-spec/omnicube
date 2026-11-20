package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.perf.PerfMonitor;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Camera-only free look. The mouse handler is intercepted before vanilla turns
 * the player, and the camera receives the captured rotation during setup.
 *
 * <p>The look is deliberately limited to a configurable arc around the body
 * instead of a full 360 degree spin, the perspective is kept in first person
 * (so it never feels like a third-person orbit), and releasing the key eases
 * the camera back instead of snapping.</p>
 */
public final class FreeLookModule extends Module {
	private static final long RETURN_NANOS = 180_000_000L;

	private final Setting.Num range = add(new Setting.Num("range", "Yaw range", 110.0D, 30.0D, 180.0D, 5.0D));
	private final Setting.Num sensitivity = add(new Setting.Num("sensitivity", "Sensitivity", 1.0D, 0.3D, 2.0D, 0.1D));
	private final Setting.Bool smooth = add(new Setting.Bool("smooth", "Smooth return", true));
	private final Setting.Bool firstPerson = add(new Setting.Bool("first_person", "Stay in first person", true));

	private boolean active;
	private boolean returning;
	private float yawOffset;
	private float pitch;
	private float blend = 1.0F;
	private long lastUpdate;
	private CameraType previousCameraType;
	private boolean failureLogged;

	public FreeLookModule() {
		super("free_look", "Free Look",
				"Hold the free-look key to look around within a limited arc without turning your player.",
				ModuleCategory.RENDER, false);
	}

	@Override
	public void tick() {
		try {
			Minecraft client = Minecraft.getInstance();
			LocalPlayer player = client.player;
			boolean held = player != null && client.screen == null
					&& OmniCubeKeybinds.freeLook != null && OmniCubeKeybinds.freeLook.isDown();

			if (held) {
				if (!active) {
					begin(client, player);
				}
				returning = false;
				blend = 1.0F;
				lastUpdate = System.nanoTime();
				return;
			}
			if (!active) {
				return;
			}
			returning = true;
			long now = System.nanoTime();
			float delta = Math.min(0.1F, Math.max(0.0F, (now - lastUpdate) / 1_000_000_000.0F));
			lastUpdate = now;
			if (smooth.get()) {
				blend = Math.max(0.0F, blend - delta / (RETURN_NANOS / 1_000_000_000.0F));
			} else {
				blend = 0.0F;
			}
			if (blend <= 0.001F) {
				finish(client);
			}
		} catch (Throwable throwable) {
			// Free look is cosmetic; a failure must never break the camera or the tick loop.
			Diagnostics.exception("free_look.tick", throwable);
			active = false;
			returning = false;
		}
	}

	private void begin(Minecraft client, LocalPlayer player) {
		active = true;
		returning = false;
		blend = 1.0F;
		yawOffset = 0.0F;
		pitch = player.getXRot();
		lastUpdate = System.nanoTime();
		if (firstPerson.get()) {
			try {
				CameraType current = client.options.getCameraType();
				if (!current.isFirstPerson()) {
					previousCameraType = current;
					client.options.setCameraType(CameraType.FIRST_PERSON);
					Diagnostics.event("free_look", "forced first person (was " + current + ")");
				}
			} catch (Throwable throwable) {
				Diagnostics.exception("free_look.perspective", throwable);
			}
		}
	}

	private void finish(Minecraft client) {
		active = false;
		returning = false;
		blend = 0.0F;
		yawOffset = 0.0F;
		if (previousCameraType != null) {
			try {
				client.options.setCameraType(previousCameraType);
			} catch (Throwable throwable) {
				Diagnostics.exception("free_look.perspective.restore", throwable);
			} finally {
				previousCameraType = null;
			}
		}
	}

	/**
	 * Called from the mouse-handler mixin before vanilla applies mouse movement.
	 *
	 * @return true when the movement was consumed by free look
	 */
	public boolean handleMouse(double deltaX, double deltaY) {
		if (!isEnabled() || !active) {
			return false;
		}
		long started = PerfMonitor.start();
		try {
			Minecraft client = Minecraft.getInstance();
			double base = client.options == null ? 0.5D
					: client.options.sensitivity().get() * 0.6D + 0.2D;
			float scale = (float) (0.12D * base * sensitivity.get());
			boolean invertX = client.options != null && client.options.invertMouseX().get();
			boolean invertY = client.options != null && client.options.invertMouseY().get();

			float limit = (float) range.get().doubleValue();
			yawOffset = Mth.clamp(yawOffset + (float) deltaX * scale * (invertX ? -1.0F : 1.0F), -limit, limit);
			pitch = Mth.clamp(pitch + (float) deltaY * scale * (invertY ? -1.0F : 1.0F), -89.9F, 89.9F);
			return true;
		} catch (Throwable throwable) {
			Diagnostics.exception("free_look.mouse", throwable);
			active = false;
			return false;
		} finally {
			PerfMonitor.record("free_look.mouse", started);
		}
	}

	public boolean isLooking() {
		return isEnabled() && active;
	}

	public boolean isReturning() {
		return returning;
	}

	public float cameraYaw() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		float base = player == null ? 0.0F : player.getYRot();
		return base + yawOffset * blend;
	}

	public float cameraPitch() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		float base = player == null ? pitch : player.getXRot();
		return base + (pitch - base) * blend;
	}

	public void fail(Throwable throwable) {
		if (!failureLogged) {
			failureLogged = true;
			OmniCubeMod.LOGGER.error("[OmniCube] Free look hook failed; disabling it for this session.", throwable);
		}
		setEnabled(false);
	}

	@Override
	protected void onDisable() {
		try {
			finish(Minecraft.getInstance());
		} catch (Throwable throwable) {
			Diagnostics.exception("free_look.disable", throwable);
		}
		active = false;
		returning = false;
	}
}
