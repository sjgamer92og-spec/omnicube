package dev.omnicube.client.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class KeybindConflicts {
	private KeybindConflicts() {
	}

	public static List<String> find() {
		List<String> conflicts = new ArrayList<>();
		try {
			Minecraft client = Minecraft.getInstance();
			if (client.options == null) {
				return conflicts;
			}
			List<KeyMapping> ours = new ArrayList<>();
			add(ours, OmniCubeKeybinds.openMenu);
			add(ours, OmniCubeKeybinds.zoom);
			add(ours, OmniCubeKeybinds.hudEditor);
			add(ours, OmniCubeKeybinds.addWaypoint);
			add(ours, OmniCubeKeybinds.copyCoordinates);
			add(ours, OmniCubeKeybinds.freeLook);
			add(ours, OmniCubeKeybinds.copyChat);
			add(ours, OmniCubeKeybinds.inventoryTools);
			add(ours, OmniCubeKeybinds.chatSearch);

			for (int i = 0; i < ours.size(); i++) {
				KeyMapping mine = ours.get(i);
				if (mine == null || mine.isUnbound()) {
					continue;
				}
				for (KeyMapping other : client.options.keyMappings) {
					if (other == mine || other.isUnbound() || ours.contains(other)) {
						continue;
					}
					if (mine.same(other)) {
						String line = mine.getTranslatedKeyMessage().getString() + " is also used by "
								+ other.getTranslatedKeyMessage().getString();
						if (!conflicts.contains(line)) {
							conflicts.add(line);
						}
					}
				}
				for (int j = i + 1; j < ours.size(); j++) {
					KeyMapping other = ours.get(j);
					if (other != null && !other.isUnbound() && mine.same(other)) {
						conflicts.add("Two OmniCube actions share " + mine.getTranslatedKeyMessage().getString());
					}
				}
			}
		} catch (Throwable ignored) {
			// A keybind warning must never stop the config screen opening.
		}
		return conflicts;
	}

	private static void add(List<KeyMapping> mappings, KeyMapping mapping) {
		if (mapping != null) {
			mappings.add(mapping);
		}
	}

}
