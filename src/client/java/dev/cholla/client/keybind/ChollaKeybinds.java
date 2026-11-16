
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

	private static KeyMapping.Category category;
	public static KeyMapping openMenu;
	public static KeyMapping zoom;
	public static KeyMapping hudEditor;
	public static KeyMapping addWaypoint;
	public static KeyMapping copyCoordinates;

	}

	public static void init() {
		try {
		} catch (Throwable throwable) {
			// A duplicate registration would only happen with two copies of the mod.
			category = KeyMapping.Category.MISC;
		}

	}

	private static KeyMapping register(String translationKey, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, category));
	}
}
