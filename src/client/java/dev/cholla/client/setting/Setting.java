
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Settings are intentionally tiny and dependency-free so a config file written by
 */
public abstract class Setting<T> {
	private final String id;
	private final String name;
	private final T defaultValue;
	protected T value;

	protected Setting(String id, String name, T defaultValue) {
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public T get() {
		return value;
	}

	public void set(T newValue) {
		if (newValue != null) {
			this.value = newValue;
		}
	}

	public void reset() {
		this.value = defaultValue;
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement json);

	public abstract String displayValue();

	public boolean adjustable() {
		return false;
	}

	public void adjust(int direction) {
		// Only numeric settings react to the GUI arrows.
	}

	public static final class Bool extends Setting<Boolean> {
		public Bool(String id, String name, boolean defaultValue) {
			super(id, name, defaultValue);
		}

		@Override
		public JsonElement toJson() {
			return new JsonPrimitive(get());
		}

		@Override
		public void fromJson(JsonElement json) {
			try {
				if (json != null && json.isJsonPrimitive()) {
					set(json.getAsBoolean());
				}
			} catch (RuntimeException ignored) {
				// A malformed value falls back to the default instead of breaking the config.
			}
		}

		@Override
		public String displayValue() {
			return get() ? "ON" : "OFF";
		}
	}

	public static final class Num extends Setting<Double> {
		private final double min;
		private final double max;
		private final double step;
		private final boolean wholeNumbers;

		public Num(String id, String name, double defaultValue, double min, double max, double step) {
			super(id, name, defaultValue);
			this.min = min;
			this.max = max;
			this.step = step <= 0.0D ? 1.0D : step;
			this.wholeNumbers = this.step >= 1.0D;
		}

		public double min() {
			return min;
		}

		public double max() {
			return max;
		}

		public double step() {
			return step;
		}

		public int intValue() {
			return (int) Math.round(get());
		}

		@Override
		public void set(Double newValue) {
			if (newValue == null || newValue.isNaN() || newValue.isInfinite()) {
				return;
			}
			super.set(Math.max(min, Math.min(max, newValue)));
		}

		@Override
		public JsonElement toJson() {
			return new JsonPrimitive(get());
		}

		@Override
		public void fromJson(JsonElement json) {
			try {
				if (json != null && json.isJsonPrimitive()) {
					set(json.getAsDouble());
				}
			} catch (RuntimeException ignored) {
				// Keep the default on malformed input.
			}
		}

		@Override
		public String displayValue() {
			return wholeNumbers ? Long.toString(Math.round(get())) : String.format(java.util.Locale.ROOT, "%.2f", get());
		}

		@Override
		public boolean adjustable() {
			return true;
		}

		@Override
		public void adjust(int direction) {
			set(get() + direction * step);
		}
	}
}
