package dev.omnicube.client.macro;

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

	public String keyName() {
		if (key <= 0) {
			return "unbound";
		}
		String name = GLFW.glfwGetKeyName(key, 0);
		return name == null ? "key " + key : name.toUpperCase(java.util.Locale.ROOT);
	}
}
