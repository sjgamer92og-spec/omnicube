package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.impl.SplashModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the Mojang studios logo screen with OmniCube's own splash: a slowly
 * rotating wireframe cube over a glowing grid, the wordmark, and a segmented
 * progress bar. Everything is plain rectangles and text, so it needs no
 * textures and costs nothing once the world has loaded. It only takes over
 * while the game is still loading; as soon as vanilla starts fading the
 * overlay out, rendering is handed back so the world appears normally.
 */
@Mixin(value = LoadingOverlay.class, priority = 1500)
public abstract class LoadingOverlayMixin {
	@Shadow
	private float currentProgress;
	@Shadow
	private long fadeInStart;
	@Shadow
	private long fadeOutStart;
	@Shadow
	private boolean fadeIn;

	private static final int MINT = 0xFF66F2C1;
	private static final int TEAL = 0xFF4FD8D6;
	private static final int GLOW = 0x2FE9B0;
	private static final String[] TIPS = {
			"Right Shift opens the OmniCube menu",
			"/omnicube diag runs a full self-check",
			"Every feature has a toggle - switch off what you do not use",
			"Settings profiles swap a whole setup in one click",
			"The Low-end preset trims the heavy extras for you"
	};

	private static String omnicube$version;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$drawSplash(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
			CallbackInfo callbackInfo) {
		SplashModule module = OmniCubeClient.splash();
		if (module == null || !module.isEnabled()) {
			return;
		}
		// While the overlay fades out, vanilla shows the game screen underneath.
		if (fadeOutStart >= 0L) {
			return;
		}
		try {
			long now = Util.getMillis();
			float alpha = 1.0F;
			if (fadeIn) {
				alpha = fadeInStart < 0L ? 0.0F : Mth.clamp((now - fadeInStart) / 500.0F, 0.0F, 1.0F);
			}
			drawSplash(Minecraft.getInstance(), graphics, alpha, currentProgress, module);
			callbackInfo.cancel();
		} catch (Throwable throwable) {
			// If anything fails, vanilla draws its own loading screen instead.
			Diagnostics.exception("splash.render", throwable);
		}
	}

	private static void drawSplash(Minecraft client, GuiGraphics graphics, float alpha, float progress,
			SplashModule module) {
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		Font font = client.font;
		boolean animate = module.animations();
		long now = animate ? Util.getMillis() : 0L;
		float clamped = Mth.clamp(progress, 0.0F, 1.0F);
		int centerX = width / 2;

		drawBackdrop(graphics, width, height, alpha, animate, now);

		int cubeY = (int) (height * 0.37F);
		float cubeSize = Mth.clamp(Math.min(width, height) * 0.085F, 22.0F, 58.0F);
		drawAura(graphics, centerX, cubeY, cubeSize, alpha);
		drawCube(graphics, centerX, cubeY, cubeSize, alpha, animate, now);

		// Wordmark with a soft mint glow behind the letters.
		String title = "OMNICUBE";
		float titleScale = Mth.clamp(width / 270.0F, 1.5F, 3.0F);
		int titleWidth = font.width(title);
		float titleY = cubeY + cubeSize * 1.55F + 14.0F;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, titleY);
		graphics.pose().scale(titleScale, titleScale);
		graphics.drawString(font, title, -titleWidth / 2 - 1, 0, argb(0x5566F2C1, alpha * 0.9F), false);
		graphics.drawString(font, title, -titleWidth / 2 + 1, 0, argb(0x5566F2C1, alpha * 0.9F), false);
		graphics.drawString(font, title, -titleWidth / 2, 0, argb(0xFFFFFFFF, alpha), true);
		graphics.pose().popMatrix();

		int underlineY = Math.round(titleY + 9.0F * titleScale + 10.0F);
		int underlineWidth = (int) (titleWidth * titleScale) + 28;
		graphics.fill(centerX - underlineWidth / 2, underlineY, centerX + underlineWidth / 2, underlineY + 2,
				argb(0x3366F2C1, alpha));
		graphics.fill(centerX - underlineWidth / 4, underlineY, centerX + underlineWidth / 4, underlineY + 2,
				argb(MINT, alpha));
		if (animate) {
			int sparkX = centerX - underlineWidth / 2
					+ (int) ((now / 7L) % Math.max(1, underlineWidth));
			graphics.fill(sparkX, underlineY - 2, sparkX + 6, underlineY + 4, argb(0xCCFFFFFF, alpha));
		}

		String subtitle = "Minecraft 1.21.11   |   OmniCube v" + version();
		graphics.drawString(font, subtitle, (width - font.width(subtitle)) / 2, underlineY + 10,
				argb(0xFF93A9BE, alpha), true);

		int barWidth = Math.min(340, width - 72);
		int barX = (width - barWidth) / 2;
		int barY = (int) (height * 0.72F);
		int barHeight = 6;
		String status;
		if (module.showProgress()) {
			drawSegmentedBar(graphics, barX, barY, barWidth, barHeight, clamped, alpha, animate, now);
			status = phaseName(clamped) + "   " + Math.round(clamped * 100.0F) + "%";
		} else {
			int dots = (int) ((now / 350L) % 4L);
			status = "Loading" + ".".repeat(dots);
		}
		graphics.drawString(font, status, (width - font.width(status)) / 2, barY + barHeight + 12,
				argb(0xFFD7E1EA, alpha), true);

		if (module.showTips()) {
			int index = animate ? (int) ((now / 4200L) % TIPS.length) : 0;
			String tip = TIPS[index];
			graphics.drawString(font, tip, (width - font.width(tip)) / 2, height - 24,
					argb(0xFF6B8095, alpha), true);
			graphics.fill(centerX - 14, height - 12, centerX + 14, height - 10, argb(0x3366F2C1, alpha));
		}
	}

	private static void drawBackdrop(GuiGraphics graphics, int width, int height, float alpha, boolean animate,
			long now) {
		graphics.fillGradient(0, 0, width, height, argb(0xFF0A1420, alpha), argb(0xFF020409, alpha));
		graphics.nextStratum();

		// Soft vignette built from a few stacked edge strips.
		int edge = Math.max(24, width / 6);
		for (int step = 1; step <= 4; step++) {
			int edgeWidth = Math.max(1, edge * (5 - step) / 4);
			graphics.fill(0, 0, edgeWidth, height, argb(0x22000000, alpha));
			graphics.fill(width - edgeWidth, 0, width, height, argb(0x22000000, alpha));
		}

		// Drifting grid in the upper half, fading out before the progress bar.
		int horizon = (int) (height * 0.66F);
		int spacing = Math.max(28, width / 22);
		int drift = animate ? (int) ((now / 70L) % spacing) : 0;
		for (int x = -spacing + drift; x < width; x += spacing) {
			graphics.fill(x, 0, x + 1, horizon, argb(0x0AFFFFFF, alpha));
		}
		for (int y = horizon; y > 0; y -= spacing) {
			graphics.fill(0, y, width, y + 1, argb(0x07FFFFFF, alpha));
		}

		// Top accent line with a light sweeping along it.
		graphics.fill(0, 0, width, 2, argb(MINT, alpha));
		if (animate) {
			int sweep = (int) ((now / 5L) % (width + 180L)) - 90;
			graphics.fill(sweep, 0, sweep + 90, 2, argb(0xBBFFFFFF, alpha));
		}
	}

	private static void drawAura(GuiGraphics graphics, int centerX, int centerY, float size, float alpha) {
		float radiusX = size * 4.4F;
		float radiusY = size * 2.9F;
		for (int y = (int) (centerY - radiusY); y < centerY + radiusY; y += 4) {
			float offset = (y - centerY) / radiusY;
			float span = radiusX * (float) Math.sqrt(Math.max(0.0, 1.0 - offset * offset));
			int strength = Math.round((1.0F - Math.abs(offset)) * 0x24);
			if (strength <= 1) {
				continue;
			}
			graphics.fill((int) (centerX - span), y, (int) (centerX + span), y + 4,
					argb((strength << 24) | GLOW, alpha));
		}
	}

	private static void drawCube(GuiGraphics graphics, int centerX, int centerY, float size, float alpha,
			boolean animate, long now) {
		double seconds = now / 1000.0D;
		double yaw = animate ? seconds * 0.55D : 0.65D;
		double pitch = animate ? 0.52D + Math.sin(seconds * 0.5D) * 0.18D : 0.52D;
		double cosYaw = Math.cos(yaw);
		double sinYaw = Math.sin(yaw);
		double cosPitch = Math.cos(pitch);
		double sinPitch = Math.sin(pitch);

		float[] screenX = new float[8];
		float[] screenY = new float[8];
		float[] depth = new float[8];
		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 1 ? 1.0D : -1.0D;
			double y = (i & 2) == 2 ? 1.0D : -1.0D;
			double z = (i & 4) == 4 ? 1.0D : -1.0D;
			double rotatedX = x * cosYaw + z * sinYaw;
			double rotatedZ = -x * sinYaw + z * cosYaw;
			double rotatedY = y * cosPitch - rotatedZ * sinPitch;
			double finalZ = y * sinPitch + rotatedZ * cosPitch;
			float perspective = (float) (3.2D / (3.2D - finalZ));
			screenX[i] = centerX + (float) (rotatedX * size * perspective);
			screenY[i] = centerY + (float) (rotatedY * size * perspective);
			depth[i] = (float) finalZ;
		}

		for (int i = 0; i < 8; i++) {
			for (int bit = 1; bit <= 4; bit <<= 1) {
				if ((i & bit) != 0) {
					continue;
				}
				int j = i | bit;
				float averageDepth = (depth[i] + depth[j]) * 0.5F;
				int color = averageDepth >= 0.0F ? argb(0xFF9CFFE0, alpha) : argb(0x5C8FE6CF, alpha);
				line(graphics, screenX[i], screenY[i], screenX[j], screenY[j], color);
			}
		}
		for (int i = 0; i < 8; i++) {
			int color = depth[i] >= 0.0F ? argb(0xFFFFFFFF, alpha) : argb(0x889CFFE0, alpha);
			int x = Math.round(screenX[i]);
			int y = Math.round(screenY[i]);
			graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
		}
	}

	private static void drawSegmentedBar(GuiGraphics graphics, int barX, int barY, int barWidth, int barHeight,
			float progress, float alpha, boolean animate, long now) {
		graphics.fill(barX - 3, barY - 3, barX + barWidth + 3, barY + barHeight + 3, argb(0x1FFFFFFF, alpha));
		int segments = Math.max(12, Math.min(64, barWidth / 6));
		int filledSegments = Math.round(progress * segments);
		float shine = animate ? ((now / 1400.0F) % 1.0F) : -1.0F;
		for (int i = 0; i < segments; i++) {
			float start = i / (float) segments;
			int left = barX + Math.round(start * barWidth);
			int right = barX + Math.round((i + 1) / (float) segments * barWidth) - 1;
			if (right <= left) {
				right = left + 1;
			}
			int color;
			if (i < filledSegments) {
				boolean lit = shine >= 0.0F && Math.abs(start - shine) < 0.075F;
				color = lit ? argb(0xFFFFFFFF, alpha) : argb((i % 2 == 0 ? MINT : TEAL), alpha);
			} else {
				color = argb(0x1CFFFFFF, alpha);
			}
			graphics.fill(left, barY, right, barY + barHeight, color);
		}
		if (progress > 0.0F) {
			int tipX = barX + (int) (Mth.clamp(progress, 0.0F, 1.0F) * barWidth);
			graphics.fill(tipX - 4, barY - 3, tipX + 4, barY + barHeight + 3, argb(0x4466F2C1, alpha));
			graphics.fill(tipX - 1, barY - 1, tipX + 1, barY + barHeight + 1, argb(0xEEFFFFFF, alpha));
		}
	}

	/** Straight 1px line drawn as a run of tiny rectangles; only used while loading. */
	private static void line(GuiGraphics graphics, float fromX, float fromY, float toX, float toY, int color) {
		float deltaX = toX - fromX;
		float deltaY = toY - fromY;
		int steps = Math.min(72, (int) Math.max(Math.abs(deltaX), Math.abs(deltaY)));
		if (steps <= 0) {
			graphics.fill(Math.round(fromX), Math.round(fromY), Math.round(fromX) + 1, Math.round(fromY) + 1, color);
			return;
		}
		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(fromX + deltaX * t);
			int y = Math.round(fromY + deltaY * t);
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	private static String phaseName(float progress) {
		if (progress < 0.2F) {
			return "Assembling the cube";
		}
		if (progress < 0.55F) {
			return "Loading resources";
		}
		if (progress < 0.85F) {
			return "Polishing the interface";
		}
		return "Almost there";
	}

	private static int argb(int color, float alpha) {
		int a = Math.round(((color >>> 24) & 0xFF) * Mth.clamp(alpha, 0.0F, 1.0F));
		return (a << 24) | (color & 0xFFFFFF);
	}

	private static String version() {
		if (omnicube$version == null) {
			try {
				omnicube$version = FabricLoader.getInstance().getModContainer("omnicube")
						.map(container -> container.getMetadata().getVersion().getFriendlyString())
						.orElse("dev");
			} catch (Throwable throwable) {
				omnicube$version = "dev";
			}
		}
		return omnicube$version;
	}
}
