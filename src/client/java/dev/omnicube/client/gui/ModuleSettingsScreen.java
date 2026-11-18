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
	private int scroll;
	private int contentHeight;
	private int viewTop;
	private int viewBottom;

	public ModuleSettingsScreen(Screen parent, Feature feature) {
		super(Component.literal(feature.name()));
		this.parent = parent;
		this.feature = feature;
	}

	@Override
	protected void init() {
		viewTop = Math.min(64, Math.max(48, this.height / 2));
		viewBottom = Math.max(viewTop + 20, this.height - 34);
		contentHeight = feature.settings().size() * ROW_HEIGHT;
		scroll = clamp(scroll);
		rebuild();
	}

	private int clamp(int value) {
		int max = Math.max(0, contentHeight - Math.max(1, viewBottom - viewTop));
		return Math.max(0, Math.min(max, value));
	}

	private void rebuild() {
		clearWidgets();
		int centerX = this.width / 2;
		int width = Math.min(360, Math.max(40, this.width - 12));
		int x = centerX - width / 2;
		int y = viewTop - scroll;

		for (Setting<?> setting : feature.settings()) {
			boolean visible = y >= viewTop && y + 20 <= viewBottom;
			if (setting instanceof Setting.Bool bool) {
				Button button = Button.builder(label(bool), pressed -> {
					bool.set(!bool.get());
					pressed.setMessage(label(bool));
					OmniCubeClient.saveConfig();
				}).bounds(x, y, width, 20).build();
				button.visible = visible;
				addRenderableWidget(button);
			} else if (setting instanceof Setting.Num number) {
				SliderWidget slider = new SliderWidget(x, y, width, 20, number, OmniCubeClient::saveConfig);
				slider.visible = visible;
				addRenderableWidget(slider);
			} else if (setting instanceof Setting.Choice choice) {
				Button button = Button.builder(label(choice), pressed -> {
					choice.next();
					pressed.setMessage(label(choice));
					OmniCubeClient.saveConfig();
				}).bounds(x, y, width, 20).build();
				button.visible = visible;
				addRenderableWidget(button);
			}
			y += ROW_HEIGHT;
		}

		int doneWidth = Math.min(100, Math.max(40, this.width - 20));
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(centerX - doneWidth / 2, this.height - 30, doneWidth, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		if (contentHeight > viewBottom - viewTop) {
			scroll = clamp(scroll - (int) Math.round(verticalAmount * 18.0D));
			rebuild();
			return true;
		}
		return false;
	}

	private Component label(Setting.Bool setting) {
		return Component.literal(setting.name() + ": " + setting.displayValue());
	}

	private Component label(Setting.Choice setting) {
		return Component.literal(setting.name() + ": " + setting.displayValue());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int centerX = this.width / 2;
		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawCenteredString(this.font, this.title, centerX, 18, 0xFFF2F2F2);
		graphics.drawCenteredString(this.font, Component.literal(feature.description()), centerX, 34, 0xFF9A9A9A);
		if (!feature.isAvailable()) {
			graphics.drawCenteredString(this.font, Component.literal("Unavailable: another mod provides this"),
					centerX, 46, 0xFFFF8888);
		} else {
			graphics.drawCenteredString(this.font,
					Component.literal(feature.isEnabled() ? "Feature is ON" : "Feature is OFF"),
					centerX, 46, feature.isEnabled() ? accent : 0xFF888888);
		}

		if (feature.settings().isEmpty()) {
			graphics.drawCenteredString(this.font, Component.literal("This feature has no extra settings."), centerX, 82, 0xFFAAAAAA);
		} else if (contentHeight > viewBottom - viewTop) {
			graphics.drawString(this.font, "Scroll for more settings", 12, this.height - 44, 0xFF777777, false);
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
