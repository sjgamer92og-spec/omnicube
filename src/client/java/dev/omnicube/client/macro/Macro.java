package dev.omnicube.client.macro;

import dev.omnicube.client.diag.Diagnostics;
import org.lwjgl.glfw.GLFW;

public final class Macro {
	public String name = "Macro";
	public String command = "";
	public int key = GLFW.GLFW_KEY_UNKNOWN;
	public boolean enabled = true;

	public Macro() {
	}

	public Macro(String name, String command, int key) {
		this.name = name;
		this.command = command;
		this.key = key;
	}

	public void sanitize() {
		if (name == null || name.isBlank()) {
			name = "Macro";
		}
		if (command == null) {
			command = "";
		}
		if (key < 0) {
			key = GLFW.GLFW_KEY_UNKNOWN;
		}
	}

	public String keyName() {
		if (key <= 0) {
			return "unbound";
		}
		try {
			String name = GLFW.glfwGetKeyName(key, 0);
			return name == null ? "key " + key : name.toUpperCase(java.util.Locale.ROOT);
		} catch (Throwable throwable) {
			Diagnostics.exception("macro.key_name", throwable);
			return "key " + key;
		}
	}
}
