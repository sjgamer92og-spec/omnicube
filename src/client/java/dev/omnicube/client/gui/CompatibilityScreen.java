package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.compat.CompatRegistry;
import dev.omnicube.client.keybind.KeybindConflicts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CompatibilityScreen extends Screen {
	private final Screen parent;
	private int scroll;
	private List<String> lines = new ArrayList<>();

	public CompatibilityScreen(Screen parent) {
		super(Component.literal("Compatibility"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		refreshLines();
		int footer = this.height - 28;
		int refreshWidth = Math.min(64, Math.max(28, this.width / 5));
		int refreshX = this.width - refreshWidth - 8;
		int backWidth = Math.min(60, Math.max(28, this.width / 4));
		int lowX = 6 + backWidth + 4;
		int lowWidth = Math.max(24, refreshX - lowX - 4);
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(6, footer, backWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Low-end"), button -> {
			OmniCubeClient.applyLowEndPreset();
			refreshLines();
		}).bounds(lowX, footer, lowWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> refreshLines())
				.bounds(refreshX, footer, refreshWidth, 20).build());
	}

	private void refreshLines() {
		List<String> next = new ArrayList<>();
		next.add("Detected overlapping mods");
		List<CompatRegistry.DetectedMod> mods = CompatRegistry.detectedMods();
		if (mods.isEmpty()) {
			next.add("  None. OmniCube features are available.");
		} else {
			for (CompatRegistry.DetectedMod mod : mods) {
				next.add("  " + mod.name() + " (" + mod.id() + ")");
				if (mod.featureIds().isEmpty()) {
					next.add("    no conflicting OmniCube feature");
				} else {
					next.add("    disables: " + String.join(", ", mod.featureIds()));
				}
			}
		}
		next.add("");
		next.add("Reasons");
		Map<String, String> disabled = CompatRegistry.disabledFeatures();
		if (disabled.isEmpty()) {
			next.add("  No OmniCube feature was auto-disabled.");
		} else {
			for (Map.Entry<String, String> entry : disabled.entrySet()) {
				next.add("  " + entry.getKey());
				for (String part : wrap(entry.getValue(), Math.max(30, this.width - 48))) {
					next.add("    " + part);
				}
			}
		}
		next.add("");
		next.add("Warnings");
		List<String> conflicts = KeybindConflicts.find();
		if (conflicts.isEmpty()) {
			next.add("  No keybind conflicts detected.");
		} else {
			for (String conflict : conflicts) {
				next.add("  ! " + conflict);
			}
		}
		next.add("  Performance backend: " + dev.omnicube.client.module.impl.PerformanceModule.backendName());
		lines = next;
		scroll = clamp(scroll);
	}

	private List<String> wrap(String text, int width) {
		List<String> result = new ArrayList<>();
		if (text == null) {
			return result;
		}
		String remaining = text;
		while (!remaining.isEmpty()) {
			String part = this.font.plainSubstrByWidth(remaining, width);
			if (part.isEmpty()) {
				break;
			}
			result.add(part);
			remaining = remaining.substring(part.length()).stripLeading();
		}
		return result;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xEE101010);
		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawString(this.font, "COMPATIBILITY", 12, 12, accent, true);
		graphics.drawString(this.font, "Other mods win: conflicting OmniCube features stay off.", 12, 25, 0xFF999999, false);

		int top = 44;
		int bottom = this.height - 36;
		graphics.fill(8, top - 3, this.width - 8, bottom + 3, 0x66101010);
		graphics.enableScissor(12, top, this.width - 12, bottom);
		int y = top - scroll;
		for (String line : lines) {
			if (y + 10 >= top && y <= bottom) {
				int color = line.startsWith("!") ? 0xFFFFAA55 : line.endsWith(":")
						? 0xFFFFFFFF : 0xFFB8B8B8;
				graphics.drawString(this.font, line, 18, y, color, false);
			}
			y += 11;
		}
		graphics.disableScissor();

		int contentHeight = lines.size() * 11;
		if (contentHeight > bottom - top) {
			int thumbHeight = Math.max(16, (bottom - top) * (bottom - top) / Math.max(1, contentHeight));
			int thumbY = top + (bottom - top - thumbHeight) * scroll / Math.max(1, contentHeight - (bottom - top));
			graphics.fill(this.width - 7, top, this.width - 4, bottom, 0x33FFFFFF);
			graphics.fill(this.width - 7, thumbY, this.width - 4, thumbY + thumbHeight, accent);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		scroll = clamp(scroll - (int) Math.round(verticalAmount * 18.0D));
		return true;
	}

	private int clamp(int value) {
		int content = lines.size() * 11;
		int visible = Math.max(1, this.height - 80);
		return Math.max(0, Math.min(Math.max(0, content - visible), value));
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
