package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.gui.widget.SliderWidget;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModuleSettingsScreen extends Screen {
	private static final int ROW_HEIGHT = 26;
	private final Screen parent;
	private final Feature feature;

	public ModuleSettingsScreen(Screen parent, Feature feature) {
		super(Component.literal(feature.name()));
		this.parent = parent;
		this.feature = feature;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int width = Math.min(360, this.width - 40);
		int x = centerX - width / 2;
		int y = 64;

		for (Setting<?> setting : feature.settings()) {
			if (setting instanceof Setting.Bool bool) {
				Button button = Button.builder(label(bool), pressed -> {
					bool.set(!bool.get());
					pressed.setMessage(label(bool));
					OmniCubeClient.saveConfig();
				}).bounds(x, y, width, 20).build();
				addRenderableWidget(button);
				y += ROW_HEIGHT;
			} else if (setting instanceof Setting.Num number) {
				addRenderableWidget(new SliderWidget(x, y, width, 20, number, OmniCubeClient::saveConfig));
				y += ROW_HEIGHT;
			}

			if (y > this.height - 50) {
				break;
			}
		}

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(centerX - 50, this.height - 30, 100, 20).build());
	}

	private Component label(Setting.Bool setting) {
		return Component.literal(setting.name() + ": " + setting.displayValue());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int centerX = this.width / 2;
		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawCenteredString(this.font, this.title, centerX, 18, 0xFFF2F2F2);
		graphics.drawCenteredString(this.font, Component.literal(feature.description()), centerX, 34, 0xFF9A9A9A);
		graphics.drawCenteredString(this.font,
				Component.literal(feature.isEnabled() ? "Feature is ON" : "Feature is OFF"),
				centerX, 46, feature.isEnabled() ? accent : 0xFF888888);

		if (feature.settings().isEmpty()) {
			graphics.drawCenteredString(this.font, Component.literal("This feature has no extra settings."), centerX, 82, 0xFFAAAAAA);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
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
