package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.macro.Macro;
import dev.omnicube.client.module.impl.MacroModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MacroScreen extends Screen {
	private static final int MAX_ROWS = 7;
	private int maxRows = MAX_ROWS;
	private final Screen parent;
	private EditBox nameField;
	private EditBox commandField;
	private Button bindButton;
	private Button addButton;
	private int pendingKey = GLFW.GLFW_KEY_UNKNOWN;
	private boolean awaitingBind;
	private String status = "Name, command and an optional key.";

	public MacroScreen(Screen parent) {
		super(Component.literal("Macros"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		pendingKey = GLFW.GLFW_KEY_UNKNOWN;
		awaitingBind = false;

		MacroModule module = OmniCubeClient.macros();
		List<Macro> macros = OmniCubeClient.config().macros;
		int width = Math.min(520, Math.max(120, this.width - 24));
		int x = (this.width - width) / 2;
		int formTop = this.height - 82;
		maxRows = Math.max(1, Math.min(MAX_ROWS, (formTop - 54) / 24));
		int y = 54;

		for (int i = 0; i < Math.min(maxRows, macros.size()); i++) {
			Macro macro = macros.get(i);
			if (macro == null) {
				continue;
			}
			addRenderableWidget(Button.builder(Component.literal("Run"), button -> {
				if (module != null) {
					module.run(macro);
				}
			}).bounds(x + width - 96, y, 44, 18).build());

			addRenderableWidget(Button.builder(Component.literal("Delete"), button -> {
				if (module != null) {
					module.remove(macro.name);
				}
				refresh();
			}).bounds(x + width - 48, y, 48, 18).build());
			y += 24;
		}

		int formY = this.height - 82;
		int nameWidth = Math.max(60, Math.min(120, width / 2 - 4));
		nameField = new EditBox(this.font, x, formY, nameWidth, 18, Component.literal("Name"));
		nameField.setValue("");
		addRenderableWidget(nameField);

		commandField = new EditBox(this.font, x + nameWidth + 6, formY, Math.max(40, width - nameWidth - 6),
				18, Component.literal("/command"));
		commandField.setValue("");
		addRenderableWidget(commandField);

		int secondRow = this.height - 58;
		int bindWidth = Math.max(60, Math.min(130, width / 2));
		bindButton = Button.builder(Component.literal("Bind: none"), button -> {
			awaitingBind = true;
			button.setMessage(Component.literal("Press a key..."));
		}).bounds(x, secondRow, bindWidth, 18).build();
		addRenderableWidget(bindButton);

		addButton = Button.builder(Component.literal("Add"), button -> addMacro())
				.bounds(x + bindWidth + 6, secondRow, Math.max(40, width - bindWidth - 6), 18).build();
		addRenderableWidget(addButton);

		int doneWidth = Math.min(80, Math.max(40, this.width / 4));
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(this.width - doneWidth - 12, this.height - 28, doneWidth, 20).build());
	}

	private void refresh() {
		// Rebuilding widgets from inside a button callback would mutate the widget
		// list while it is being iterated, so defer it to the next client task.
		if (this.minecraft != null) {
			this.minecraft.execute(this::rebuildWidgets);
		}
	}

	private void addMacro() {
		MacroModule module = OmniCubeClient.macros();
		if (module == null) {
			status = "Macro module unavailable.";
			return;
		}
		String name = nameField == null ? "" : nameField.getValue().strip();
		String command = commandField == null ? "" : commandField.getValue().strip();
		if (name.isEmpty() || command.isEmpty()) {
			status = "A name and a command are required.";
			return;
		}
		module.add(name, command, pendingKey);
		status = "Added '" + name + "'.";
		refresh();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int accent = OmniCubeClient.config().accentArgb();
		int width = Math.min(520, Math.max(120, this.width - 24));
		int x = (this.width - width) / 2;

		graphics.drawCenteredString(this.font, this.title.copy().append(Component.literal("  (" + OmniCubeClient.config().macros.size() + ")")),
				this.width / 2, 14, 0xFFF2F2F2);
		graphics.drawString(this.font, "Macros run a chat or slash command when their key is pressed in-game.",
				x, 30, 0xFF9A9A9A, false);
		graphics.drawString(this.font, "Commands starting with / are sent as commands; anything else is sent as chat.",
				x, 40, 0xFF777777, false);
		graphics.fill(x, 49, x + width, 50, 0x33FFFFFF);

		List<Macro> macros = OmniCubeClient.config().macros;
		int y = 54;
		for (int i = 0; i < Math.min(maxRows, macros.size()); i++) {
			Macro macro = macros.get(i);
			if (macro == null) {
				continue;
			}
			graphics.fill(x, y, x + width, y + 20, i % 2 == 0 ? 0x22FFFFFF : 0x11101010);
			graphics.drawString(this.font, macro.name, x + 6, y + 3, 0xFFEDEDED, false);
			graphics.drawString(this.font, macro.keyName(), x + 6, y + 11, accent, false);

			String command = macro.command;
			int maxWidth = Math.max(40, width - 220);
			while (this.font.width(command) > maxWidth && command.length() > 3) {
				command = command.substring(0, command.length() - 2);
			}
			graphics.drawString(this.font, command, x + 110, y + 6, 0xFFAAAAAA, false);
			y += 24;
		}

		if (macros.isEmpty()) {
			graphics.drawCenteredString(this.font, "No macros yet — add one below.",
					this.width / 2, 70, 0xFF888888);
		}

		int formTop = this.height - 82;
		graphics.fill(x, formTop - 6, x + width, formTop - 5, 0x33FFFFFF);

		String trimmedStatus = status;
		int statusWidth = Math.max(40, width - 100);
		while (this.font.width(trimmedStatus) > statusWidth && trimmedStatus.length() > 3) {
			trimmedStatus = trimmedStatus.substring(0, trimmedStatus.length() - 2);
		}
		graphics.drawString(this.font, trimmedStatus, x, this.height - 38, 0xFFAAAAAA, false);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (awaitingBind) {
			awaitingBind = false;
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				if (bindButton != null) {
					bindButton.setMessage(Component.literal("Bind: none"));
				}
				return true;
			}
			pendingKey = event.key();
			if (bindButton != null) {
				String keyName = GLFW.glfwGetKeyName(pendingKey, 0);
				bindButton.setMessage(Component.literal("Bind: " + (keyName == null ? "key " + pendingKey : keyName.toUpperCase(java.util.Locale.ROOT))));
			}
			return true;
		}
		return super.keyPressed(event);
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
}
