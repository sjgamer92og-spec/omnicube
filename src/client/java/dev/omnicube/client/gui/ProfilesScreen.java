package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.config.ProfileManager;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the built-in and saved settings profiles.
 *
 * <p>The profile files are read and written on a background thread; this screen only
 * sends requests and shows the result that comes back on the client thread.
 */
public final class ProfilesScreen extends Screen {
	private static final int ROW_HEIGHT = 22;

	private final Screen parent;
	private final List<String> names = new ArrayList<>();
	private String selected = "";
	private String status = "Pick a profile, or type a name and save your current settings.";
	private int scroll;
	private EditBox nameBox;

	public ProfilesScreen(Screen parent) {
		super(Component.literal("Settings Profiles"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int width = Math.min(300, Math.max(80, this.width - 40));
		int x = centerX - width / 2;
		int footerY = this.height - 30;

		nameBox = new EditBox(this.font, x, footerY, Math.max(60, width - 106), 20,
				Component.literal("Profile name"));
		nameBox.setMaxLength(24);
		nameBox.setValue("");
		addRenderableWidget(nameBox);

		addRenderableWidget(Button.builder(Component.literal("Save as"), button -> save())
				.bounds(x + width - 100, footerY, 100, 20).build());

		int buttonWidth = Math.min(88, Math.max(48, (width - 8) / 2));
		int rowY = this.height - 56;
		addRenderableWidget(Button.builder(Component.literal("Load"), button -> load())
				.bounds(x, rowY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Delete"), button -> delete())
				.bounds(x + buttonWidth + 8, rowY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Rename"), button -> rename())
				.bounds(x + (buttonWidth + 8) * 2, rowY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(x + (buttonWidth + 8) * 3, rowY, buttonWidth, 20).build());

		refresh();
	}

	private void refresh() {
		ProfileManager.namesAsync(list -> {
			names.clear();
			names.addAll(ProfileManager.builtInNames());
			names.addAll(list);
			if (names.isEmpty()) {
				selected = "";
			} else if (selected.isEmpty() || !names.contains(selected)) {
				// The selected profile may have been deleted or renamed; never leave the
				// screen pointing at a name that no longer exists.
				selected = names.getFirst();
			}
		});
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int accent = OmniCubeClient.config().accentArgb();
		int centerX = this.width / 2;
		int width = Math.min(300, Math.max(80, this.width - 40));
		int x = centerX - width / 2;
		int top = 56;
		int bottom = this.height - 64;

		graphics.drawCenteredString(this.font, this.title, centerX, 14, 0xFFF2F2F2);
		graphics.drawCenteredString(this.font, Component.literal(
						"Built-in profiles are applied directly; saved profiles are files in config/omnicube/profiles."),
				centerX, 30, 0xFF9A9A9A);
		graphics.fill(x - 2, top - 2, x + width + 2, bottom + 2, 0x88101010);

		graphics.enableScissor(x, top, x + width, bottom);
		try {
			int y = top - scroll;
			for (String name : names) {
				boolean isBuiltIn = ProfileManager.isBuiltIn(name);
				boolean isSelected = name.equals(selected);
				boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
				if (isSelected || hovered) {
					graphics.fill(x, y, x + width, y + ROW_HEIGHT - 2, isSelected ? 0x447ED957 : 0x22FFFFFF);
				}
				if (isSelected) {
					graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 2, accent);
				}
				graphics.drawString(this.font, name, x + 9, y + 6,
						isSelected ? 0xFFFFFFFF : isBuiltIn ? 0xFFB8E6A6 : 0xFFCDCDCD, false);
				graphics.drawString(this.font, isBuiltIn ? "built-in" : "saved",
						x + width - 10 - this.font.width(isBuiltIn ? "built-in" : "saved"), y + 6,
						0xFF777777, false);
				y += ROW_HEIGHT;
			}
			if (names.isEmpty()) {
				graphics.drawString(this.font, "Loading profiles...", x + 8, top + 8, 0xFFAAAAAA, false);
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("gui.profiles_render", throwable);
		} finally {
			graphics.disableScissor();
		}

		graphics.drawString(this.font, this.font.plainSubstrByWidth(status, this.width - 20), 10, this.height - 70,
				0xFFB0B0B0, false);
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
		int centerX = this.width / 2;
		int width = Math.min(300, Math.max(80, this.width - 40));
		int x = centerX - width / 2;
		int top = 56;
		int bottom = this.height - 64;
		double mouseX = event.x();
		double mouseY = event.y();
		if (mouseX < x || mouseX >= x + width || mouseY < top || mouseY >= bottom) {
			return false;
		}
		int index = (int) ((mouseY - top + scroll) / ROW_HEIGHT);
		if (index >= 0 && index < names.size()) {
			selected = names.get(index);
			if (nameBox != null) {
				nameBox.setValue(ProfileManager.isBuiltIn(selected) ? "" : selected);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		int bottom = this.height - 64;
		int max = Math.max(0, names.size() * ROW_HEIGHT - Math.max(1, bottom - 56));
		scroll = Math.max(0, Math.min(max, scroll - (int) Math.round(verticalAmount * 18.0D)));
		return true;
	}

	private void save() {
		String name = nameBox == null ? "" : nameBox.getValue();
		ProfileManager.saveAsync(name, message -> {
			status = message;
			refresh();
		});
	}

	private void load() {
		if (selected == null || selected.isBlank()) {
			OmniCubeText.sendError("Pick a profile first.");
			return;
		}
		ProfileManager.loadAsync(selected, message -> {
			status = message;
			refresh();
		});
	}

	private void delete() {
		if (selected == null || selected.isBlank()) {
			OmniCubeText.sendError("Pick a profile first.");
			return;
		}
		ProfileManager.deleteAsync(selected, message -> {
			status = message;
			refresh();
		});
	}

	private void rename() {
		String newName = nameBox == null ? "" : nameBox.getValue();
		if (selected == null || selected.isBlank() || newName.isBlank()) {
			OmniCubeText.sendError("Pick a saved profile and type its new name.");
			return;
		}
		ProfileManager.renameAsync(selected, newName, message -> {
			status = message;
			selected = newName;
			refresh();
		});
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
