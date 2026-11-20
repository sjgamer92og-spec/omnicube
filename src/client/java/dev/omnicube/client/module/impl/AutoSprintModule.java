package dev.omnicube.client.module.impl;

import dev.omnicube.client.compat.ClientCompat;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

public final class AutoSprintModule extends Module {
	public AutoSprintModule() {
		super("auto_sprint", "Auto Sprint", "Automatically sprints while you hold forward, unless you are sneaking or using an item.", ModuleCategory.PLAYER, false);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.level == null || client.screen != null) {
			return;
		}
		if (player.isSprinting() || player.isShiftKeyDown() || player.isUsingItem() || player.isPassenger()) {
			return;
		}
		if (ClientCompat.isKeyDown(GLFW.GLFW_KEY_W)) {
			try {
				player.setSprinting(true);
			} catch (Throwable throwable) {
				// A server may reject the state change; never let that break the tick.
				Diagnostics.exception("auto_sprint", throwable);
			}
		}
	}
}
