package dev.omnicube.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClientContainerTooltip implements ClientTooltipComponent {
	private static final int COLUMNS = 9;
	private static final int CELL = 18;
	private static final int PADDING = 2;

	private final List<ItemStack> stacks;

	public ClientContainerTooltip(List<ItemStack> stacks) {
		this.stacks = stacks == null ? List.of() : List.copyOf(stacks);
	}

	@Override
	public int getHeight(Font font) {
		return Math.max(1, (stacks.size() + COLUMNS - 1) / COLUMNS) * CELL + PADDING * 2;
	}

	@Override
	public int getWidth(Font font) {
		return COLUMNS * CELL + PADDING * 2;
	}

	@Override
	public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
		try {
			graphics.fill(x, y, x + getWidth(font), y + getHeight(font), 0x55101010);
			for (int i = 0; i < stacks.size(); i++) {
				ItemStack stack = stacks.get(i);
				if (stack == null || stack.isEmpty()) {
					continue;
				}
				int column = i % COLUMNS;
				int row = i / COLUMNS;
				int itemX = x + PADDING + column * CELL + 1;
				int itemY = y + PADDING + row * CELL + 1;
				graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x55202020);
				graphics.renderItem(stack, itemX, itemY);
				graphics.renderItemDecorations(font, stack, itemX, itemY);
			}
		} catch (Throwable ignored) {
			// A malformed stack should leave the rest of the tooltip intact.
		}
	}

}
