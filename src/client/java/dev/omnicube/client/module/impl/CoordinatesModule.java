package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

public final class CoordinatesModule extends Module {
	private final Setting.Bool includeDimension = add(new Setting.Bool("include_dimension", "Include dimension", true));

	public CoordinatesModule() {
		super("coordinates", "Coordinates Copy", "Press the copy keybind to copy your exact position and dimension to the clipboard.", ModuleCategory.MISC, true);
	}

	public void copyPosition() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		String text = String.format(Locale.ROOT, "%.2f %.2f %.2f", player.getX(), player.getY(), player.getZ());
		if (includeDimension.get()) {
			try {
				String dimension = client.level == null ? "" : client.level.dimension().identifier().getPath();
				if (!dimension.isEmpty()) {
					text = text + " (" + dimension + ")";
				}
			} catch (Throwable throwable) {
				// Copy the coordinates even if the dimension is unavailable.
				Diagnostics.exception("coordinates.dimension", throwable);
			}
		}
		OmniCubeText.copyToClipboard(text);
		OmniCubeText.send("Copied: " + text);
	}
}
