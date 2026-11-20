package dev.omnicube.client.compat;

import dev.omnicube.client.diag.Diagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * All Minecraft-version-sensitive calls are intentionally funnelled through this
 * class. When OmniCube is ported to 1.21.x / 26.x, this is the first file to review;
 * the feature code above it should not need to change.
 */
public final class ClientCompat {
	public static final String MINECRAFT_VERSION = "1.21.11";

	private ClientCompat() {
	}

	public static int guiWidth(GuiGraphics graphics) {
		return graphics.guiWidth();
	}

	public static int guiHeight(GuiGraphics graphics) {
		return graphics.guiHeight();
	}

	public static long windowHandle() {
		return Minecraft.getInstance().getWindow().handle();
	}

	public static boolean isKeyDown(int keyCode) {
		try {
			return org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle(), keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
		} catch (Throwable throwable) {
			Diagnostics.exception("client.key_state", throwable);
			return false;
		}
	}

	public static boolean isMouseDown(int button) {
		try {
			return org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
		} catch (Throwable throwable) {
			Diagnostics.exception("client.mouse_state", throwable);
			return false;
		}
	}

	public static void openFile(java.io.File file) {
		try {
			net.minecraft.util.Util.getPlatform().openFile(file);
		} catch (Throwable throwable) {
			Diagnostics.exception("client.open_file", throwable);
			dev.omnicube.OmniCubeMod.LOGGER.warn("[OmniCube] Could not open {}", file, throwable);
		}
	}
}
