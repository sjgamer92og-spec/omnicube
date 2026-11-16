
/**
 * Normalized (0..1) top-left position so the layout survives resolution and GUI
 * scale changes.
 */
public final class HudPos {
	public double x;
	public double y;
	public boolean enabled;

	public HudPos() {
		this(0.01D, 0.01D, true);
	}

	public HudPos(double x, double y, boolean enabled) {
		this.x = clamp(x);
		this.y = clamp(y);
		this.enabled = enabled;
	}

	public static double clamp(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return 0.0D;
		}
		return Math.max(0.0D, Math.min(1.0D, value));
	}
}
