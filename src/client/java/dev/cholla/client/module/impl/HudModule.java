

public final class HudModule extends Module {
	private final Setting.Bool background = add(new Setting.Bool("background", "Element background", true));

	public HudModule() {
	}

	@Override
	protected void onEnable() {
		HudElement.background = background.get();
	}

	@Override
	protected void onDisable() {
		// Hiding is handled by HudManager; nothing to restore here.
	}

	@Override
	public void tick() {
		HudElement.background = background.get();
	}

	public boolean backgroundEnabled() {
		return background.get();
	}
}
