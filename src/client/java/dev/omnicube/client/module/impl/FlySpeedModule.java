package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FlySpeedModule extends Module {
	private static final float VANILLA_FLY_SPEED = 0.05F;
	private final Setting.Num speed = add(new Setting.Num("speed", "Fly speed", 0.10D, 0.05D, 1.0D, 0.05D));
	private Float originalFlySpeed;

	public FlySpeedModule() {
		super("fly_speed", "Fly Speed", "Sets your creative flight speed. The server still decides whether you may fly.", ModuleCategory.PLAYER, false);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || !player.getAbilities().flying) {
			return;
		}
		try {
			if (originalFlySpeed == null) {
				originalFlySpeed = player.getAbilities().getFlyingSpeed();
			}
			float target = (float) Math.max(0.05D, Math.min(1.0D, speed.get()));
			if (Math.abs(player.getAbilities().getFlyingSpeed() - target) > 0.0001F) {
				player.getAbilities().setFlyingSpeed(target);
			}
		} catch (Throwable ignored) {
			// Abilities can be swapped out by the server at any time.
		}
	}

	@Override
	protected void onDisable() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			originalFlySpeed = null;
			return;
		}
		try {
			player.getAbilities().setFlyingSpeed(originalFlySpeed == null ? VANILLA_FLY_SPEED : originalFlySpeed);
		} catch (Throwable ignored) {
			// Nothing to restore if the player is gone.
		} finally {
			originalFlySpeed = null;
		}
	}
}
