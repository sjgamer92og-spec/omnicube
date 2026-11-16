
public enum ModuleCategory {
	RENDER("Render"),
	HUD("HUD"),
	PLAYER("Player"),
	WORLD("World"),
	CHAT("Chat"),
	PERFORMANCE("Performance"),
	MISC("Misc");

	private final String label;

	ModuleCategory(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
