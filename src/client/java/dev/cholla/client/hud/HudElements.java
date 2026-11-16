
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class HudElements {
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

	private HudElements() {
	}

	public static void register(HudManager hud) {
		hud.register(new FpsHud());
		hud.register(new CoordinatesHud());
		hud.register(new BiomeHud());
		hud.register(new FacingHud());
		hud.register(new PingHud());
		hud.register(new SpeedHud());
		hud.register(new EntityCountHud());
		hud.register(new ServerHud());
		hud.register(new MemoryHud());
		hud.register(new SessionHud());
		hud.register(new ClockHud());
		hud.register(new TimerHud());
		hud.register(new TntHud());
		hud.register(new WaypointHud());
		hud.register(new ExperienceHud());
		hud.register(new PotionHud());
		hud.register(new ArmorHud());
		hud.register(new DurabilityHud());
		hud.register(new KeystrokesHud());
		hud.register(new CpsHud());
	}

	private static List<String> fallback() {
		return List.of("Waiting for a world...");
	}

	private static final class FpsHud extends HudElement {
		private FpsHud() {
			super("hud_fps", "FPS", "Frames per second and frame time.", 0.006D, 0.006D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			try {
				int fps = client.getFps();
				long frameNs = client.getFrameTimeNs();
				return List.of("FPS  " + fps, String.format(Locale.ROOT, "Frame  %.1f ms", frameNs / 1_000_000.0D));
			} catch (Throwable throwable) {
				return fallback();
			}
		}
	}

	private static final class CoordinatesHud extends HudElement {
		private CoordinatesHud() {
			super("hud_coords", "Coordinates", "Exact position plus chunk coordinates.", 0.006D, 0.075D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			BlockPos pos = player.blockPosition();
			return List.of(
					String.format(Locale.ROOT, "XYZ  %.1f / %.1f / %.1f", player.getX(), player.getY(), player.getZ()),
					"Chunk  " + (pos.getX() >> 4) + " / " + (pos.getZ() >> 4)
			);
		}
	}

	private static final class BiomeHud extends HudElement {
		private BiomeHud() {
			super("hud_biome", "Biome & Dimension", "Current biome and dimension identifiers.", 0.006D, 0.13D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null || client.level == null) {
				return fallback();
			}
			String dimension;
			String biome;
			try {
				dimension = client.level.dimension().identifier().getPath();
			} catch (Throwable throwable) {
				dimension = "unknown";
			}
			try {
				biome = client.level.getBiome(player.blockPosition()).unwrapKey()
						.map(key -> key.identifier().getPath().replace('_', ' '))
						.orElse("unknown");
			} catch (Throwable throwable) {
				biome = "unknown";
			}
			return List.of("Dimension  " + dimension, "Biome  " + biome);
		}
	}

	private static final class FacingHud extends HudElement {
		private FacingHud() {
			super("hud_facing", "Facing", "Cardinal direction and raw yaw.", 0.006D, 0.185D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			String cardinal = switch (player.getDirection()) {
				case NORTH -> "North (-Z)";
				case SOUTH -> "South (+Z)";
				case WEST -> "West (-X)";
				case EAST -> "East (+X)";
				default -> player.getDirection().getName();
			};
			return List.of(cardinal, String.format(Locale.ROOT, "Yaw  %.1f", player.getYRot()));
		}
	}

	private static final class PingHud extends HudElement {
		private PingHud() {
			super("hud_ping", "Ping", "Latency reported by the current server.", 0.006D, 0.24D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			if (client.getConnection() == null) {
				return List.of("Ping  local");
			}
			try {
				PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
				return List.of("Ping  " + (info == null ? "-" : info.getLatency() + " ms"));
			} catch (Throwable throwable) {
				return List.of("Ping  -");
			}
		}
	}

	private static final class SpeedHud extends HudElement {
		private SpeedHud() {
			super("hud_speed", "Speed", "Horizontal movement speed in blocks per second.", 0.006D, 0.295D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			double speed = player.getDeltaMovement().horizontalDistance() * 20.0D;
			return List.of(String.format(Locale.ROOT, "Speed  %.1f b/s", speed));
		}
	}

	private static final class EntityCountHud extends HudElement {
		private EntityCountHud() {
			super("hud_entities", "Nearby Entities", "Rendered entities within 48 blocks.", 0.006D, 0.345D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null || client.level == null) {
				return fallback();
			}
			int count = 0;
			for (var entity : client.level.entitiesForRendering()) {
				if (entity.distanceToSqr(player.getX(), player.getY(), player.getZ()) <= 48.0D * 48.0D) {
					count++;
				}
			}
			return List.of("Entities  " + count);
		}
	}

	private static final class ServerHud extends HudElement {
		private ServerHud() {
			super("hud_server", "Server", "Address of the server you are connected to.", 0.006D, 0.395D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			if (client.getConnection() == null) {
				return fallback();
			}
			try {
				ServerData data = client.getConnection().getServerData();
				if (data == null) {
					return List.of("Server  singleplayer");
				}
				return List.of("Server  " + data.ip);
			} catch (Throwable throwable) {
				return List.of("Server  -");
			}
		}
	}

	private static final class MemoryHud extends HudElement {
		private MemoryHud() {
			super("hud_memory", "Memory", "Java heap usage.", 0.79D, 0.006D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			Runtime runtime = Runtime.getRuntime();
			long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
			long max = runtime.maxMemory() / (1024L * 1024L);
			return List.of("Memory  " + used + " / " + max + " MB");
		}
	}

	private static final class SessionHud extends HudElement {
		private static long joinedAt;
		private static boolean inWorld;

		private SessionHud() {
			super("hud_session", "Session", "How long you have been in this world.", 0.79D, 0.055D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			if (client.player == null) {
				inWorld = false;
				return List.of("Session  -");
			}
			if (!inWorld) {
				inWorld = true;
				joinedAt = System.currentTimeMillis();
			}
		}
	}

	private static final class ClockHud extends HudElement {
		private ClockHud() {
			super("hud_clock", "Clock", "Real time and in-game time of day.", 0.79D, 0.105D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			String gameTime = "-";
			if (client.level != null) {
				long dayTime = client.level.getDayTime();
				long day = dayTime / 24000L;
				long ticks = dayTime % 24000L;
				int hours = (int) ((ticks / 1000L + 6L) % 24L);
				int minutes = (int) ((ticks % 1000L) * 60L / 1000L);
				gameTime = String.format(Locale.ROOT, "Day %d  %02d:%02d", day, hours, minutes);
			}
			return List.of(LocalTime.now().format(CLOCK), gameTime);
		}
	}

	private static final class TimerHud extends HudElement {
		private TimerHud() {
		}

		@Override
		public List<String> lines(Minecraft client) {
					Stopwatch.isRunning() ? "running" : "stopped");
		}
	}

	private static final class TntHud extends HudElement {
		private TntHud() {
			super("hud_tnt", "TNT Fuse", "Countdown of the nearest primed TNT.", 0.79D, 0.215D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null || client.level == null) {
				return fallback();
			}
			PrimedTnt nearest = null;
			double best = Double.MAX_VALUE;
			for (var entity : client.level.entitiesForRendering()) {
				if (!(entity instanceof PrimedTnt tnt)) {
					continue;
				}
				double distance = tnt.distanceToSqr(player.getX(), player.getY(), player.getZ());
				if (distance < best) {
					best = distance;
					nearest = tnt;
				}
			}
			if (nearest == null) {
				return List.of("TNT  none nearby");
			}
			double seconds = Math.max(0.0D, nearest.getFuse()) / 20.0D;
			return List.of(String.format(Locale.ROOT, "TNT  %.1fs", seconds),
					String.format(Locale.ROOT, "%.0f blocks away", Math.sqrt(best)));
		}
	}

	private static final class WaypointHud extends HudElement {
		private WaypointHud() {
			super("hud_waypoint", "Waypoint Compass", "Direction and distance to the active or nearest waypoint.", 0.79D, 0.27D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			if (module == null || !module.isEnabled()) {
				return List.of("Waypoints  disabled");
			}
			var active = module.active();
			if (active.isEmpty()) {
			}
			Waypoint waypoint = active.get();
			double dx = waypoint.x - player.getX();
			double dz = waypoint.z - player.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz + (waypoint.y - player.getY()) * (waypoint.y - player.getY()));
			double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
			double difference = ((targetYaw - player.getYRot()) % 360.0D + 540.0D) % 360.0D - 180.0D;
			String direction = difference > 45.0D ? "left" : difference < -45.0D ? "right" : "ahead";
			if (Math.abs(difference) > 135.0D) {
				direction = "behind";
			}
		}
	}

	private static final class ExperienceHud extends HudElement {
		private ExperienceHud() {
			super("hud_experience", "Experience", "Experience level and progress toward the next level.", 0.79D, 0.335D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			return List.of("XP  level " + player.experienceLevel,
					String.format(Locale.ROOT, "Progress  %.0f%%", player.experienceProgress * 100.0F));
		}
	}

	private static final class PotionHud extends HudElement {
		private PotionHud() {
			super("hud_potions", "Potion Effects", "Active status effects and their remaining time.", 0.79D, 0.36D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			List<String> lines = new ArrayList<>();
			int shown = 0;
			for (MobEffectInstance effect : player.getActiveEffects()) {
				if (shown >= 5) {
					lines.add("  + more");
					break;
				}
				String name;
				try {
					name = effect.getEffect().value().getDisplayName().getString();
				} catch (Throwable throwable) {
					name = "Effect";
				}
				int seconds = Math.max(0, effect.getDuration() / 20);
				shown++;
			}
			if (lines.isEmpty()) {
				return List.of("Effects  none");
			}
			lines.add(0, "Effects");
			return lines;
		}
	}

	private static final class ArmorHud extends HudElement {
		private ArmorHud() {
			super("hud_armor", "Armor", "Durability of each equipped armor piece.", 0.79D, 0.51D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			List<String> lines = new ArrayList<>();
			lines.add("Armor");
			addPiece(lines, "Head", player.getItemBySlot(EquipmentSlot.HEAD));
			addPiece(lines, "Body", player.getItemBySlot(EquipmentSlot.CHEST));
			addPiece(lines, "Legs", player.getItemBySlot(EquipmentSlot.LEGS));
			addPiece(lines, "Feet", player.getItemBySlot(EquipmentSlot.FEET));
			if (lines.size() == 1) {
				lines.add("  none");
			}
			return lines;
		}

		private void addPiece(List<String> lines, String slot, ItemStack stack) {
			if (stack == null || stack.isEmpty()) {
				return;
			}
			if (!stack.isDamageableItem()) {
				lines.add("  " + slot + "  " + stack.getHoverName().getString());
				return;
			}
			int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
			int percent = stack.getMaxDamage() <= 0 ? 0 : (int) Math.round(remaining * 100.0D / stack.getMaxDamage());
			lines.add("  " + slot + "  " + percent + "%");
		}
	}

	private static final class DurabilityHud extends HudElement {
		private DurabilityHud() {
			super("hud_durability", "Held Item", "Durability of the item in your main hand.", 0.79D, 0.63D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			LocalPlayer player = client.player;
			if (player == null) {
				return fallback();
			}
			ItemStack stack = player.getMainHandItem();
			if (stack.isEmpty()) {
				return List.of("Hand  empty");
			}
			if (!stack.isDamageableItem()) {
				return List.of(stack.getHoverName().getString(), "Not damageable");
			}
			int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
			return List.of(stack.getHoverName().getString(), "Durability  " + remaining + " / " + stack.getMaxDamage());
		}
	}

	private static final class CpsHud extends HudElement {
		private final Deque<Long> leftClicks = new ArrayDeque<>();
		private final Deque<Long> rightClicks = new ArrayDeque<>();
		private boolean wasLeft;
		private boolean wasRight;

		private CpsHud() {
			super("hud_cps", "CPS", "Left and right clicks per second.", 0.79D, 0.885D);
		}

		@Override
		public List<String> lines(Minecraft client) {
			long now = System.currentTimeMillis();
			boolean left = ClientCompat.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			boolean right = ClientCompat.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			if (left && !wasLeft) {
				leftClicks.addLast(now);
			}
			if (right && !wasRight) {
				rightClicks.addLast(now);
			}
			wasLeft = left;
			wasRight = right;
			while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) {
				leftClicks.removeFirst();
			}
			while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) {
				rightClicks.removeFirst();
			}
			return List.of("CPS  " + leftClicks.size() + " / " + rightClicks.size());
		}
	}

	private static final class KeystrokesHud extends HudElement {
		private KeystrokesHud() {
			super("hud_keystrokes", "Keystrokes", "A compact WASD / space / shift input display.", 0.79D, 0.78D);
			setMeasuredSize(52, 52);
		}

		@Override
		public List<String> lines(Minecraft client) {
			return List.of();
		}

		@Override
		public void render(GuiGraphics graphics, Minecraft client, int screenX, int screenY) {
			try {
				int key = 16;
				int gap = 2;
				boolean w = ClientCompat.isKeyDown(GLFW.GLFW_KEY_W);
				boolean a = ClientCompat.isKeyDown(GLFW.GLFW_KEY_A);
				boolean s = ClientCompat.isKeyDown(GLFW.GLFW_KEY_S);
				boolean d = ClientCompat.isKeyDown(GLFW.GLFW_KEY_D);
				boolean space = ClientCompat.isKeyDown(GLFW.GLFW_KEY_SPACE);
				boolean shift = ClientCompat.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ClientCompat.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);

				box(graphics, client, screenX + key + gap, screenY, key, "W", w);
				box(graphics, client, screenX, screenY + key + gap, key, "A", a);
				box(graphics, client, screenX + key + gap, screenY + key + gap, key, "S", s);
				box(graphics, client, screenX + (key + gap) * 2, screenY + key + gap, key, "D", d);
				box(graphics, client, screenX, screenY + (key + gap) * 2, (key + gap) * 2 - gap, "SPACE", space);
				box(graphics, client, screenX + (key + gap) * 2, screenY + (key + gap) * 2, key, "SHIFT", shift);
			} catch (Throwable ignored) {
				// If input polling is unavailable, the keystrokes element simply stays blank.
			}
		}

		private void box(GuiGraphics graphics, Minecraft client, int x, int y, int width, String label, boolean pressed) {
			int background = pressed ? 0xCC7ED957 : 0x66000000;
			int border = pressed ? 0xFF7ED957 : 0x55FFFFFF;
			graphics.fill(x, y, x + width, y + 16, background);
			graphics.fill(x, y, x + width, y + 1, border);
			graphics.fill(x, y + 15, x + width, y + 16, border);
			graphics.fill(x, y, x + 1, y + 16, border);
			graphics.fill(x + width - 1, y, x + width, y + 16, border);
			int textWidth = client.font.width(label);
			graphics.drawString(client.font, label, x + (width - textWidth) / 2, y + 4, pressed ? 0xFF101010 : 0xFFFFFFFF, true);
		}

		@Override
		public int width(Minecraft client) {
			return 16 * 3 + 2 * 2;
		}

		@Override
		public int height(Minecraft client) {
			return 16 * 3 + 2 * 2;
		}
	}
}
