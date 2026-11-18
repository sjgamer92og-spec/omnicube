package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatSearchScreen extends Screen {
	private final Screen parent;
	private EditBox search;
	private List<String> results = new ArrayList<>();
	private String query = "";
	private String status = "Type to search chat history.";

	public ChatSearchScreen(Screen parent) {
		super(Component.literal("Chat Search"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = Math.max(10, this.width / 2 - 180);
		int width = Math.min(360, this.width - 20);
		search = new EditBox(this.font, x, 30, width, 18, Component.literal("Search chat"));
		search.setValue(query);
		search.setResponder(value -> {
			query = value == null ? "" : value;
			rebuild();
		});
		addRenderableWidget(search);
		int doneWidth = Math.min(64, Math.max(16, width / 3));
		int doneX = x + width - doneWidth;
		int clearWidth = Math.min(70, doneX - x - 4);
		if (clearWidth >= 14) {
			addRenderableWidget(Button.builder(Component.literal("Clear"), button -> {
				search.setValue("");
				rebuild();
			}).bounds(x, this.height - 28, clearWidth, 20).build());
		}
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(doneX, this.height - 28, doneWidth, 20).build());
		rebuild();
	}

	private void rebuild() {
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		List<String> next = new ArrayList<>();
		List<String> history = OmniCubeClient.chat() == null ? List.of() : OmniCubeClient.chat().history();
		for (String line : history) {
			if (line == null || line.isBlank()) {
				continue;
			}
			var streamer = OmniCubeClient.streamer();
			if (streamer != null && streamer.hideChat()) {
				line = streamer.redactString(line);
			}
			if (needle.isEmpty() || line.toLowerCase(Locale.ROOT).contains(needle)) {
				next.add(line);
				if (next.size() >= 80) {
					break;
				}
			}
		}
		results = next;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xEE101010);
		int accent = OmniCubeClient.config().accentArgb();
		graphics.drawString(this.font, "CHAT SEARCH", 12, 8, accent, true);
		graphics.drawString(this.font, "Click a line to copy it. History is local and capped.", 12, 18, 0xFF888888, false);

		int x = Math.max(10, this.width / 2 - 180);
		int width = Math.min(360, this.width - 20);
		int y = 56;
		int maxRows = Math.max(1, (this.height - 96) / 14);
		for (int i = 0; i < Math.min(maxRows, results.size()); i++) {
			String line = results.get(i);
			boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 13;
			graphics.fill(x, y, x + width, y + 13, hovered ? 0x44FFFFFF : 0x22101010);
			graphics.drawString(this.font, this.font.plainSubstrByWidth(line, width - 8), x + 4, y + 2,
					hovered ? 0xFFFFFFFF : 0xFFCCCCCC, false);
			y += 14;
		}
		if (results.isEmpty()) {
			graphics.drawString(this.font, "No matching messages.", x, 58, 0xFF888888, false);
		}
		graphics.drawString(this.font, status, x, this.height - 42, 0xFF888888, false);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0) {
			return false;
		}
		int x = Math.max(10, this.width / 2 - 180);
		int width = Math.min(360, this.width - 20);
		int y = 56;
		int maxRows = Math.max(1, (this.height - 96) / 14);
		for (int i = 0; i < Math.min(maxRows, results.size()); i++) {
			if (event.x() >= x && event.x() < x + width && event.y() >= y && event.y() < y + 13) {
				OmniCubeText.copyToClipboard(results.get(i));
				status = "Copied.";
				return true;
			}
			y += 14;
		}
		return false;
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
