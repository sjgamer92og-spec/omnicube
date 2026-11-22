package dev.omnicube.client.module.impl;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.compat.ClientCompat;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.macro.Macro;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashSet;
import java.util.Set;

public final class MacroModule extends Module {
	private final Setting.Bool feedback = add(new Setting.Bool("feedback", "Chat feedback", false));
	private final Set<Integer> heldKeys = new HashSet<>();

	public MacroModule() {
		super("macros", "Macros", "Runs a saved command whenever its physical key is pressed. Manage them in the Macros screen.", ModuleCategory.MISC, true);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null) {
			heldKeys.clear();
			return;
		}

		Set<Integer> stillDown = new HashSet<>();
		for (Macro macro : OmniCubeClient.config().macros) {
			if (macro == null || !macro.enabled || macro.key <= 0 || macro.command == null || macro.command.isBlank()) {
				continue;
			}
			boolean down = ClientCompat.isKeyDown(macro.key);
			if (down) {
				stillDown.add(macro.key);
			}
			if (down && !heldKeys.contains(macro.key)) {
				run(macro);
			}
		}
		heldKeys.clear();
		heldKeys.addAll(stillDown);
	}

	public void run(Macro macro) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || macro.command.isBlank()) {
			return;
		}
		String command = macro.command.startsWith("/") ? macro.command.substring(1) : macro.command;
		try {
			if (macro.command.startsWith("/")) {
				player.connection.sendCommand(command);
			} else {
				player.connection.sendChat(command);
			}
			if (feedback.get()) {
				OmniCubeText.send("Ran macro '" + macro.name + "'");
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("macros.run", throwable);
			OmniCubeText.sendError("Macro '" + macro.name + "' could not be sent.");
		}
	}

	public Macro add(String name, String command, int key) {
		// Re-using a macro name updates it; duplicates made add/remove/run ambiguous.
		String clean = name == null || name.isBlank() ? "Macro" : name.strip();
		for (Macro existing : OmniCubeClient.config().macros) {
			if (existing != null && clean.equalsIgnoreCase(existing.name)) {
				existing.command = command;
				existing.key = key;
				existing.enabled = true;
				OmniCubeClient.saveConfig();
				return existing;
			}
		}
		Macro macro = new Macro(clean, command, key);
		OmniCubeClient.config().macros.add(macro);
		OmniCubeClient.saveConfig();
		return macro;
	}

	public boolean remove(String name) {
		boolean removed = OmniCubeClient.config().macros.removeIf(macro -> macro.name != null && macro.name.equalsIgnoreCase(name));
		if (removed) {
			OmniCubeClient.saveConfig();
		}
		return removed;
	}
}
