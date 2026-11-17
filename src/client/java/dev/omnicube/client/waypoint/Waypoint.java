package dev.omnicube.client.waypoint;

public final class Waypoint {
	public String name = "Waypoint";
	public double x;
	public double y;
	public double z;
	public String dimension = "";
	public int color = 0xFF7ED957;

	public Waypoint() {
	}

	public Waypoint(String name, double x, double y, double z, String dimension, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color;
	}

	public void sanitize() {
		if (name == null || name.isBlank()) {
			name = "Waypoint";
		}
		if (dimension == null) {
			dimension = "";
		}
		if (!Double.isFinite(x)) {
			x = 0.0D;
		}
		if (!Double.isFinite(y)) {
			y = 0.0D;
		}
		if (!Double.isFinite(z)) {
			z = 0.0D;
		}
	}
}
