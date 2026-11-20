package dev.omnicube.client.module.impl;

import dev.omnicube.OmniCubeMod;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.render.WorldTextRenderer;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class NameTagModule extends Module {
	private final Setting.Bool showHealth = add(new Setting.Bool("show_health", "Show health", true));
	private final Setting.Bool showDistance = add(new Setting.Bool("show_distance", "Show distance", true));
	private final Setting.Num maxDistance = add(new Setting.Num("max_distance", "Maximum distance", 64.0D, 8.0D, 256.0D, 8.0D));
	private final Setting.Num scale = add(new Setting.Num("scale", "Tag scale", 1.0D, 0.5D, 1.8D, 0.1D));
	private boolean failureLogged;

	public NameTagModule() {
		super("name_tags", "Name Tags+", "Adds client-side player tags with health and distance above players.", ModuleCategory.PLAYER, false);
	}

	public void fail(Throwable throwable) {
		Diagnostics.exception("name_tags", throwable);
		if (!failureLogged) {
			failureLogged = true;
			OmniCubeMod.LOGGER.error("[OmniCube] Name tag hook failed; disabling it for this session.", throwable);
		}
		markFailed("Failed: " + Diagnostics.describe(throwable) + " in NameTagModule (see stack trace)");
	}

	public boolean shouldReplace(Player player) {
		if (!isEnabled() || player == null || player.isSpectator()) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui || client.screen != null) {
			return false;
		}
		return player.distanceToSqr(client.player.getX(), client.player.getY(), client.player.getZ())
				<= maxDistance.get() * maxDistance.get();
	}

	public void render(WorldRenderContext context) {
		if (!isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer self = client.player;
		if (self == null || client.level == null || client.options.hideGui || client.screen != null) {
			return;
		}

		Vec3 camera = context.worldState().cameraRenderState.pos;
		for (Player player : client.level.players()) {
			try {
				if (!shouldReplace(player)) {
					continue;
				}
				double distance = player.position().distanceTo(camera);
				if (player.isInvisibleTo(self)) {
					continue;
				}

				MutableComponent label = Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE);
				if (showHealth.get()) {
					float health = Math.max(0.0F, player.getHealth());
					float maxHealth = Math.max(1.0F, player.getMaxHealth());
					ChatFormatting color = health / maxHealth > 0.5F ? ChatFormatting.GREEN
							: health / maxHealth > 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
					label.append(Component.literal(String.format(Locale.ROOT, " %.1f HP", health)).withStyle(color));
				}
				if (showDistance.get()) {
					label.append(Component.literal(" " + OmniCubeText.formatDistance(distance) + "m").withStyle(ChatFormatting.GRAY));
				}

				Vec3 position = new Vec3(player.getX(), player.getY() + player.getBbHeight() + 0.75D, player.getZ());
				WorldTextRenderer.draw(context, position, label, 0xFFFFFFFF, scale.get().floatValue());
			} catch (Throwable throwable) {
				// One broken player must not prevent the other name tags from rendering.
				Diagnostics.exception("name_tags.player", throwable);
			}
		}
	}
}
