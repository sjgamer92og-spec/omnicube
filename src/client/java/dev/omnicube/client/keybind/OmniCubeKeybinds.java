package dev.omnicube.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import dev.omnicube.OmniCubeMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class OmniCubeKeybinds {
	private static KeyMapping.Category category;
	public static KeyMapping openMenu;
	public static KeyMapping zoom;
	public static KeyMapping hudEditor;
	public static KeyMapping addWaypoint;
	public static KeyMapping copyCoordinates;

	private OmniCubeKeybinds() {
	}

	public static void init() {
		try {
			category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(OmniCubeMod.MOD_ID, "main"));
		} catch (Throwable throwable) {
			// A duplicate registration would only happen with two copies of the mod.
			OmniCubeMod.LOGGER.warn("[OmniCube] Could not register the keybind category, using Misc.", throwable);
			category = KeyMapping.Category.MISC;
		}

		openMenu = register("key.omnicube.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
		zoom = register("key.omnicube.zoom", GLFW.GLFW_KEY_C);
		hudEditor = register("key.omnicube.hud_editor", GLFW.GLFW_KEY_H);
		addWaypoint = register("key.omnicube.add_waypoint", GLFW.GLFW_KEY_B);
		copyCoordinates = register("key.omnicube.copy_coordinates", GLFW.GLFW_KEY_F6);
	}

	private static KeyMapping register(String translationKey, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, category));
	}
}
