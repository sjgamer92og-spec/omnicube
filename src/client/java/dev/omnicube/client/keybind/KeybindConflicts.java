package dev.omnicube.client.keybind;

import dev.omnicube.client.diag.Diagnostics;
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
			if (client == null || client.options == null) {
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
			add(ours, OmniCubeKeybinds.swapTool);

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
								+ other.getTranslatedKeyMessage().getString() + " from " + owner(other);
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
		} catch (Throwable throwable) {
			// A keybind warning must never stop the config screen opening.
			Diagnostics.exception("keybind_conflicts", throwable);
		}
		return conflicts;
	}

	/** Names the mod that owns a conflicting keybind, using its category namespace. */
	private static String owner(KeyMapping mapping) {
		try {
			KeyMapping.Category category = mapping.getCategory();
			if (category == null) {
				return "an unknown mod";
			}
			String namespace = category.id().getNamespace();
			String label = category.label().getString();
			if ("minecraft".equals(namespace)) {
				return "Minecraft (" + label + ")";
			}
			return namespace + " (" + label + ")";
		} catch (Throwable throwable) {
			Diagnostics.exception("keybind_conflicts.owner", throwable);
			return "an unknown mod";
		}
	}

	private static void add(List<KeyMapping> mappings, KeyMapping mapping) {
		if (mapping != null) {
			mappings.add(mapping);
		}
	}
}
