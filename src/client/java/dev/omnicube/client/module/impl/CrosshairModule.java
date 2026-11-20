package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class CrosshairModule extends Module {
	private final Setting.Choice shape = add(new Setting.Choice("shape", "Shape",
			List.of("Cross", "Dot", "Square", "Circle"), "Cross"));
	private final Setting.Choice color = add(new Setting.Choice("color", "Color",
			List.of("Green", "White", "Red", "Cyan", "Orange"), "Green"));
	private final Setting.Num size = add(new Setting.Num("size", "Size", 10.0D, 2.0D, 24.0D, 1.0D));

	public CrosshairModule() {
		super("crosshair", "Crosshair", "Replaces the vanilla crosshair with a configurable shape, color and size.", ModuleCategory.RENDER, false);
	}

	public void render(GuiGraphics graphics) {
		try {
			Minecraft client = Minecraft.getInstance();
			if (!isEnabled() || client.player == null || client.screen != null || client.options.hideGui
					|| !client.options.getCameraType().isFirstPerson() || client.player.isSpectator()) {
				return;
			}
			int centerX = graphics.guiWidth() / 2;
			int centerY = graphics.guiHeight() / 2;
			int size = Math.max(2, this.size.intValue());
			int color = color();
			String shape = this.shape.get();

			switch (shape) {
				case "Dot" -> graphics.fill(centerX - size / 2, centerY - size / 2,
						centerX + (size + 1) / 2, centerY + (size + 1) / 2, color);
				case "Square" -> {
					int half = Math.max(1, size / 2);
					graphics.fill(centerX - half, centerY - half, centerX + half, centerY - half + 1, color);
					graphics.fill(centerX - half, centerY + half - 1, centerX + half, centerY + half, color);
					graphics.fill(centerX - half, centerY - half, centerX - half + 1, centerY + half, color);
					graphics.fill(centerX + half - 1, centerY - half, centerX + half, centerY + half, color);
				}
				case "Circle" -> drawCircle(graphics, centerX, centerY, Math.max(1, size / 2), color);
				default -> {
					// Keep at least one visible pixel per arm even at the minimum size.
					int half = Math.max(2, size / 2);
					int gap = Math.max(1, Math.min(half - 1, size / 6));
					graphics.fill(centerX - half, centerY, centerX - gap, centerY + 1, color);
					graphics.fill(centerX + gap, centerY, centerX + half, centerY + 1, color);
					graphics.fill(centerX, centerY - half, centerX + 1, centerY - gap, color);
					graphics.fill(centerX, centerY + gap, centerX + 1, centerY + half, color);
				}
			}
		} catch (Throwable throwable) {
			// The vanilla crosshair remains the fallback if custom drawing fails.
			Diagnostics.exception("render.crosshair", throwable);
		}
	}

	private void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		int x = radius;
		int y = 0;
		int error = 1 - x;
		while (x >= y) {
			plot(graphics, centerX + x, centerY + y, color);
			plot(graphics, centerX + y, centerY + x, color);
			plot(graphics, centerX - y, centerY + x, color);
			plot(graphics, centerX - x, centerY + y, color);
			plot(graphics, centerX - x, centerY - y, color);
			plot(graphics, centerX - y, centerY - x, color);
			plot(graphics, centerX + y, centerY - x, color);
			plot(graphics, centerX + x, centerY - y, color);
			y++;
			if (error < 0) {
				error += 2 * y + 1;
			} else {
				x--;
				error += 2 * (y - x) + 1;
			}
		}
	}

	private void plot(GuiGraphics graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 1, y + 1, color);
	}

	private int color() {
		return switch (color.get()) {
			case "White" -> 0xFFFFFFFF;
			case "Red" -> 0xFFFF5555;
			case "Cyan" -> 0xFF55E5FF;
			case "Orange" -> 0xFFFFAA33;
			default -> 0xFF7ED957;
		};
	}
}
