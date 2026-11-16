
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
			}
		} catch (Throwable throwable) {
		}
	}

	public Macro add(String name, String command, int key) {
		Macro macro = new Macro(name, command, key);
		return macro;
	}

	public boolean remove(String name) {
		if (removed) {
		}
		return removed;
	}
}
