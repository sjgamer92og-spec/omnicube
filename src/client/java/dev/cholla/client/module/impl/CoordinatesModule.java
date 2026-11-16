
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

public final class CoordinatesModule extends Module {
	private final Setting.Bool includeDimension = add(new Setting.Bool("include_dimension", "Include dimension", true));

	public CoordinatesModule() {
		super("coordinates", "Coordinates Copy", "Press the copy keybind to copy your exact position and dimension to the clipboard.", ModuleCategory.MISC, true);
	}

	public void copyPosition() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		String text = String.format(Locale.ROOT, "%.2f %.2f %.2f", player.getX(), player.getY(), player.getZ());
			try {
				String dimension = client.level == null ? "" : client.level.dimension().identifier().getPath();
				if (!dimension.isEmpty()) {
					text = text + " (" + dimension + ")";
				}
			} catch (Throwable ignored) {
				// Copy the coordinates even if the dimension is unavailable.
			}
		}
	}
}
