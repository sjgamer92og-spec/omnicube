
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SliderWidget extends AbstractWidget {
	private final Setting.Num setting;
	private final Runnable onChange;
	private boolean dragging;

	public SliderWidget(int x, int y, int width, int height, Setting.Num setting, Runnable onChange) {
		super(x, y, width, height, Component.literal(setting.name() + ": " + setting.displayValue()));
		this.setting = setting;
		this.onChange = onChange;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		int x = getX();
		int y = getY();
		int width = getWidth();
		int height = getHeight();
		int background = isHovered() ? 0xCC333333 : 0xAA1A1A1A;
		graphics.fill(x, y, x + width, y + height, background);
		graphics.fill(x, y, x + width, y + 1, 0x55FFFFFF);
		graphics.fill(x, y + height - 1, x + width, y + height, 0x55FFFFFF);

		double range = setting.max() - setting.min();
		double ratio = range <= 0.0D ? 0.0D : (setting.get() - setting.min()) / range;
		int filled = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, ratio)));
		graphics.fill(x, y + height - 4, x + filled, y + height, 0xFF7ED957);

		String text = setting.name() + ": " + setting.displayValue();
		graphics.drawString(client.font, text, x + 5, y + (height - 8) / 2, 0xFFF2F2F2, true);
	}

	private void applyFrom(double mouseX) {
		double range = setting.max() - setting.min();
		if (range <= 0.0D) {
			return;
		}
		double ratio = (mouseX - getX()) / Math.max(1.0D, getWidth());
		ratio = Math.max(0.0D, Math.min(1.0D, ratio));
		double raw = setting.min() + ratio * range;
		double stepped = Math.round(raw / setting.step()) * setting.step();
		setting.set(stepped);
		setMessage(Component.literal(setting.name() + ": " + setting.displayValue()));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.active && this.visible && event.button() == 0 && isMouseOver(event.x(), event.y())) {
			dragging = true;
			applyFrom(event.x());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging) {
			applyFrom(event.x());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean wasDragging = dragging;
		dragging = false;
		if (wasDragging) {
			// Persist once per interaction instead of on every drag update.
			onChange.run();
		}
		return wasDragging || super.mouseReleased(event);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
