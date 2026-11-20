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
 * Replaces the Mojang studios logo screen with OmniCube's own splash. It only
 * takes over while the game is still loading; as soon as vanilla starts fading
 * the overlay out, rendering is handed back so the world appears normally.
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

		graphics.fillGradient(0, 0, width, height, argb(0xFF16202C, alpha), argb(0xFF05070B, alpha));
		graphics.nextStratum();

		int accent = argb(0xFF7ED957, alpha);
		graphics.fill(0, 0, width, 2, accent);
		graphics.fill(0, height - 2, width, height, argb(0x402FBF6A, alpha));

		String title = "OMNICUBE";
		float scale = Math.min(3.5F, Math.max(1.5F, width / 150.0F));
		int titleWidth = font.width(title);
		float titleY = height * 0.34F;
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F, titleY);
		graphics.pose().scale(scale, scale);
		graphics.drawString(font, title, -titleWidth / 2, 0, argb(0xFFFFFFFF, alpha), true);
		graphics.pose().popMatrix();

		int underlineY = (int) (titleY + 9.0F * scale + 8.0F);
		graphics.fill(width / 2 - 36, underlineY, width / 2 + 36, underlineY + 1, accent);

		String subtitle = "Minecraft 1.21.11  |  v" + version();
		graphics.drawString(font, subtitle, (width - font.width(subtitle)) / 2,
				underlineY + 10, argb(0xFF9FB3C8, alpha), true);

		int barWidth = Math.min(240, width - 48);
		int barX = (width - barWidth) / 2;
		int barY = (int) (height * 0.64F);
		String status;
		if (module.showProgress()) {
			graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 4, argb(0x40FFFFFF, alpha));
			int filled = Mth.clamp((int) (barWidth * progress), 0, barWidth);
			graphics.fill(barX, barY, barX + filled, barY + 3, accent);
			status = "Loading resources  " + Math.round(Mth.clamp(progress, 0.0F, 1.0F) * 100.0F) + "%";
		} else {
			int dots = (int) ((Util.getMillis() / 350L) % 4L);
			status = "Loading" + ".".repeat(dots);
		}
		graphics.drawString(font, status, (width - font.width(status)) / 2, barY + 10,
				argb(0xFFD7E1EA, alpha), true);

		if (module.showTips()) {
			String tip = "Right Shift opens the OmniCube menu";
			graphics.drawString(font, tip, (width - font.width(tip)) / 2, height - 20,
					argb(0xFF5E7183, alpha), true);
		}
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
