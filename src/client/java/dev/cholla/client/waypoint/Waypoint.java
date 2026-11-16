
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
}
