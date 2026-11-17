package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.hud.HudElement;
import dev.omnicube.client.hud.HudManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class HudEditorScreen extends Screen {
	private final Screen parent;
	private final List<ElementBox> boxes = new ArrayList<>();
	private HudElement dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("Reset layout"), button -> {
			HudManager hud = OmniCubeClient.hud();
			for (HudElement element : hud.elements()) {
				element.resetPosition();
				element.setEnabled(true);
			}
			OmniCubeClient.saveConfig();
		}).bounds(12, this.height - 28, 100, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(this.width - 92, this.height - 28, 80, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x44000000);

		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawString(this.font, "HUD EDITOR", 12, 10, accent, true);
		graphics.drawString(this.font, "Drag to move  •  Right-click to show/hide  •  Positions save automatically",
				12, 22, 0xFFB0B0B0, false);

		for (int x = 0; x < this.width; x += 40) {
			graphics.fill(x, 34, x + 1, this.height - 34, 0x18FFFFFF);
		}
		for (int y = 34; y < this.height - 34; y += 40) {
			graphics.fill(0, y, this.width, y + 1, 0x18FFFFFF);
		}

		boxes.clear();
		HudManager hud = OmniCubeClient.hud();
		for (HudElement element : hud.elements()) {
			int width = Math.max(12, element.width(this.minecraft));
			int height = Math.max(12, element.height(this.minecraft));
			int x = Math.max(0, Math.min(this.width - width, element.screenX(this.minecraft, this.width)));
			int y = Math.max(0, Math.min(this.height - height, element.screenY(this.minecraft, this.height)));

			if (element.isEnabled()) {
				try {
					element.render(graphics, this.minecraft, x, y);
				} catch (Throwable ignored) {
					// An element that needs a world simply draws nothing here.
				}
			} else {
				try {
					List<String> lines = element.lines(this.minecraft);
					int lineY = y;
					for (String line : lines) {
						graphics.drawString(this.font, line, x, lineY, 0x66FFFFFF, false);
						lineY += 10;
					}
				} catch (Throwable ignored) {
					// Show only the border for elements that cannot compute without a world.
				}
			}

			int border = element.isEnabled() ? accent : 0xFF777777;
			graphics.fill(x - 2, y - 2, x + width + 2, y - 1, border);
			graphics.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, border);
			graphics.fill(x - 2, y - 1, x - 1, y + height + 1, border);
			graphics.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, border);

			graphics.drawString(this.font, element.name(), x, Math.max(34, y - 10), element.isEnabled() ? 0xFFDDDDDD : 0xFF888888, false);
			boxes.add(new ElementBox(element, x, y, width, height));
		}

		graphics.drawString(this.font, "Performance backend: " + dev.omnicube.client.module.impl.PerformanceModule.backendName(),
				12, this.height - 42, 0xFF777777, false);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		for (int i = boxes.size() - 1; i >= 0; i--) {
			ElementBox box = boxes.get(i);
			if (!box.contains(event.x(), event.y())) {
				continue;
			}
			if (event.button() == 1) {
				box.element().setEnabled(!box.element().isEnabled());
				OmniCubeClient.saveConfig();
				return true;
			}
			if (event.button() == 0) {
				dragging = box.element();
				dragOffsetX = (int) event.x() - box.x();
				dragOffsetY = (int) event.y() - box.y();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging != null) {
			int x = (int) event.x() - dragOffsetX;
			int y = (int) event.y() - dragOffsetY;
			int availableX = Math.max(1, this.width - dragging.measuredWidth());
			int availableY = Math.max(1, this.height - dragging.measuredHeight());
			dragging.setPosition(x / (double) availableX, y / (double) availableY);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			OmniCubeClient.saveConfig();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		OmniCubeClient.saveConfig();
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private record ElementBox(HudElement element, int x, int y, int width, int height) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x - 2 && mouseX <= x + width + 2 && mouseY >= y - 2 && mouseY <= y + height + 2;
		}
	}
}
