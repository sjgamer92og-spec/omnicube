package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.util.OmniCubeText;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class InventoryToolsScreen extends Screen {
	private static final int CELL = 24;
	private static final int COLUMNS = 9;

	private final Screen parent;
	private EditBox search;
	private Button quickMoveButton;
	private Button sortButton;
	private List<Slot> slots = new ArrayList<>();
	private List<Slot> visible = new ArrayList<>();
	private Slot selected;
	private String query = "";
	private boolean sortView;
	private int scroll;

	public InventoryToolsScreen(Screen parent) {
		super(Component.literal("Inventory Tools"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Minecraft client = this.minecraft;
		if (client == null || client.player == null) {
			return;
		}
		slots = new ArrayList<>();
		for (Slot slot : client.player.inventoryMenu.slots) {
			if (slot.index >= 9 && slot.index < 45) {
				slots.add(slot);
			}
		}

		int x = Math.max(10, this.width / 2 - 180);
		int width = Math.min(360, this.width - 20);
		search = new EditBox(this.font, x, 24, width, 18, Component.literal("Search items"));
		search.setValue(query);
		search.setResponder(value -> {
			query = value == null ? "" : value;
			rebuild();
		});
		addRenderableWidget(search);

		int footer = this.height - 28;
		int buttonWidth = Math.max(12, Math.min(100, (width - 8) / 3 - 4));
		quickMoveButton = Button.builder(Component.literal("Quick move"), button -> quickMove())
				.bounds(x, footer, buttonWidth, 20).build();
		// Start from the real state: the label used to say "off" after reopening the
		// screen even though the view was still sorted.
		sortButton = Button.builder(Component.literal(sortView ? "Sort: on" : "Sort: off"), button -> {
			sortView = !sortView;
			button.setMessage(Component.literal(sortView ? "Sort: on" : "Sort: off"));
			rebuild();
		}).bounds(x + buttonWidth + 4, footer, buttonWidth, 20).build();
		addRenderableWidget(quickMoveButton);
		addRenderableWidget(sortButton);

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(x + (buttonWidth + 4) * 2, footer, buttonWidth, 20).build());

		rebuild();
	}

	private void rebuild() {
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		List<Slot> next = new ArrayList<>();
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (!needle.isEmpty()) {
				if (stack.isEmpty() || !stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
					continue;
				}
			}
			next.add(slot);
		}
		if (sortView) {
			next.sort(Comparator
					.comparing((Slot slot) -> slot.getItem().isEmpty())
					.thenComparing(slot -> slot.getItem().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
		}
		visible = next;
		if (selected != null && !visible.contains(selected)) {
			selected = null;
		}
		int rows = Math.max(1, (this.height - 100) / cell());
		int maxScroll = Math.max(0, (visible.size() + COLUMNS - 1) / COLUMNS - rows);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		updateButtons();
	}

	private void updateButtons() {
		if (quickMoveButton != null) {
			quickMoveButton.active = selected != null && selected.hasItem();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xEE101010);
		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawString(this.font, "INVENTORY TOOLS", 12, 8, accent, true);
		graphics.drawString(this.font, "Search, select a stack and quick-move it. Sort changes the view order only.",
				12, this.height - 42, 0xFF888888, false);

		int cell = cell();
		int gridX = Math.max(4, this.width / 2 - (COLUMNS * cell) / 2);
		int gridY = 52;
		int rows = Math.max(1, (this.height - 100) / cell);
		graphics.fill(gridX - 2, gridY - 2, gridX + COLUMNS * cell + 2, gridY + rows * cell + 2, 0x88101010);

		int start = scroll * COLUMNS;
		for (int i = 0; i < rows * COLUMNS; i++) {
			int index = start + i;
			if (index >= visible.size()) {
				break;
			}
			Slot slot = visible.get(index);
			int column = i % COLUMNS;
			int row = i / COLUMNS;
			int x = gridX + column * cell;
			int y = gridY + row * cell;
			ItemStack stack = slot.getItem();
			boolean hovered = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
			graphics.fill(x + 1, y + 1, x + cell - 1, y + cell - 1,
					slot == selected ? 0x667ED957 : hovered ? 0x44FFFFFF : 0x22101010);
			if (!stack.isEmpty()) {
				int itemOffset = Math.max(1, (cell - 16) / 2);
				graphics.renderItem(stack, x + itemOffset, y + itemOffset);
				graphics.renderItemDecorations(this.font, stack, x + itemOffset, y + itemOffset);
			}
		}

		if (visible.isEmpty()) {
			graphics.drawString(this.font, "No matching items.", gridX + 4, gridY + 4, 0xFF888888, false);
		} else {
			String count = visible.size() + " item(s)";
			graphics.drawString(this.font, count, gridX + COLUMNS * cell - this.font.width(count), gridY - 11,
					0xFF888888, false);
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		Slot hovered = slotAt(mouseX, mouseY);
		if (hovered != null && !hovered.getItem().isEmpty()) {
			graphics.setTooltipForNextFrame(this.font, hovered.getItem(), mouseX, mouseY);
		}
	}

	private int cell() {
		return Math.max(12, Math.min(CELL, (this.width - 20) / COLUMNS));
	}

	private Slot slotAt(double mouseX, double mouseY) {
		int cell = cell();
		int gridX = Math.max(4, this.width / 2 - (COLUMNS * cell) / 2);
		int gridY = 52;
		int rows = Math.max(1, (this.height - 100) / cell);
		if (mouseX < gridX || mouseY < gridY || mouseX >= gridX + COLUMNS * cell || mouseY >= gridY + rows * cell) {
			return null;
		}
		int column = (int) ((mouseX - gridX) / cell);
		int row = (int) ((mouseY - gridY) / cell);
		int index = scroll * COLUMNS + row * COLUMNS + column;
		return index >= 0 && index < visible.size() ? visible.get(index) : null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() == 0) {
			Slot slot = slotAt(event.x(), event.y());
			if (slot != null) {
				selected = slot;
				updateButtons();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		int rows = Math.max(1, (this.height - 100) / cell());
		int maxScroll = Math.max(0, (visible.size() + COLUMNS - 1) / COLUMNS - rows);
		scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount)));
		return true;
	}

	private void quickMove() {
		try {
			Minecraft client = this.minecraft;
			if (client == null || client.player == null || selected == null || !selected.hasItem()) {
				return;
			}
			if (client.player.containerMenu != client.player.inventoryMenu) {
				OmniCubeText.sendError("Open your player inventory before using quick-move.");
				return;
			}
			client.player.connection.send(new ServerboundContainerClickPacket(
					client.player.inventoryMenu.containerId,
					client.player.inventoryMenu.getStateId(),
					(short) selected.index,
					(byte) 0,
					ClickType.QUICK_MOVE,
					new Int2ObjectOpenHashMap<>(),
					HashedStack.EMPTY
			));
			selected = null;
			// The stacks moved, so the filtered and sorted view has to be rebuilt.
			rebuild();
		} catch (Throwable throwable) {
			Diagnostics.exception("inventory_tools.quick_move", throwable);
			OmniCubeText.sendError("Quick-move failed; the server rejected the click.");
		}
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
