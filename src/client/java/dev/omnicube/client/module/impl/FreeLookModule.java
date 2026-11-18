package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.keybind.OmniCubeKeybinds;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.perf.PerfMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Camera-only free look. The mouse handler is intercepted before vanilla turns
 * the player, and the camera receives the captured rotation during setup.
 */
public final class FreeLookModule extends Module {
	private boolean active;
	private float freeYaw;
	private float freePitch;
	private boolean failureLogged;

	public FreeLookModule() {
		super("free_look", "Free Look", "Hold the free-look key to look around without turning your player.", ModuleCategory.RENDER, false);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		boolean held = player != null && client.screen == null
				&& OmniCubeKeybinds.freeLook != null && OmniCubeKeybinds.freeLook.isDown();

		if (!held) {
			active = false;
			return;
		}
		if (!active) {
			active = true;
			freeYaw = player.getYRot();
			freePitch = player.getXRot();
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
			freeYaw += (float) (deltaX * 0.15D);
			freePitch += (float) (deltaY * 0.15D);
			freePitch = Math.max(-90.0F, Math.min(90.0F, freePitch));
			freeYaw = freeYaw % 360.0F;
			return true;
		} catch (Throwable ignored) {
			active = false;
			return false;
		} finally {
			PerfMonitor.record("free_look.mouse", started);
		}
	}

	public boolean isLooking() {
		return isEnabled() && active;
	}

	public float cameraYaw() {
		return freeYaw;
	}

	public float cameraPitch() {
		return freePitch;
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
		active = false;
	}
}
