
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * A draggable HUD element. Elements render plain text by default; a few
 * (keystrokes) override {@link #render} to draw simple rectangles.
 */
public abstract class HudElement implements Feature {
	private static final int PADDING = 3;
	private static final int LINE_HEIGHT = 10;
	public static boolean background = true;

	private final String id;
	private final String name;
	private final String description;
	private final double defaultX;
	private final double defaultY;
	private double x;
	private double y;
	private boolean enabled;
	private int measuredWidth = 160;
	private int measuredHeight = 40;

	protected HudElement(String id, String name, String description, double defaultX, double defaultY) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
		this.enabled = true;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public ModuleCategory category() {
		return ModuleCategory.HUD;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public void setPosition(double newX, double newY) {
		this.x = HudPos.clamp(newX);
		this.y = HudPos.clamp(newY);
	}

	public void resetPosition() {
		this.x = defaultX;
		this.y = defaultY;
	}

	public int screenX(Minecraft client, int guiWidth) {
		return (int) Math.round(x * Math.max(0, guiWidth - measuredWidth));
	}

	public int screenY(Minecraft client, int guiHeight) {
		return (int) Math.round(y * Math.max(0, guiHeight - measuredHeight));
	}

	protected final void setMeasuredSize(int width, int height) {
		this.measuredWidth = Math.max(1, width);
		this.measuredHeight = Math.max(1, height);
	}

	public int measuredWidth() {
		return measuredWidth;
	}

	public int measuredHeight() {
		return measuredHeight;
	}

	/** Lines shown by the default text renderer. */
	public abstract List<String> lines(Minecraft client);

	protected int accentColor() {
		return 0xFF7ED957;
	}

	public void render(GuiGraphics graphics, Minecraft client, int screenX, int screenY) {
		List<String> lines = lines(client);
		if (lines.isEmpty()) {
			return;
		}

		int width = 0;
		for (String line : lines) {
			width = Math.max(width, client.font.width(line));
		}
		int height = lines.size() * LINE_HEIGHT;
		setMeasuredSize(width + PADDING * 2, height + PADDING * 2);

		if (background) {
			graphics.fill(screenX - PADDING, screenY - PADDING, screenX + width + PADDING, screenY + height + PADDING, 0x66000000);
		}

		int y = screenY;
		for (int i = 0; i < lines.size(); i++) {
			int color = i == 0 ? accentColor() : 0xFFE6E6E6;
			graphics.drawString(client.font, lines.get(i), screenX, y, color, true);
			y += LINE_HEIGHT;
		}
	}

	public int width(Minecraft client) {
		int width = 0;
		for (String line : lines(client)) {
			width = Math.max(width, client.font.width(line));
		}
		return width + PADDING * 2;
	}

	public int height(Minecraft client) {
		return Math.max(1, lines(client).size()) * LINE_HEIGHT + PADDING * 2;
	}
}
