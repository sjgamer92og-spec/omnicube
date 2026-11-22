package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/**
 * A few cheap visual toggles for low-end machines.
 *
 * <p>Everything here maps to an existing vanilla option, so nothing has to hook a
 * render pass for particles, weather or camera shake. The previous values are
 * captured when the module is enabled and restored when it is disabled, and the
 * particle option is left alone while "Reduce particles" is off, so the module
 * never fights the user's own video settings.
 */
public final class VisualTogglesModule extends Module {
	private final Setting.Bool reduceParticles = add(new Setting.Bool("reduce_particles", "Reduce particles", false));
	private final Setting.Bool hideWeather = add(new Setting.Bool("hide_weather", "Hide rain and snow", false));
	private final Setting.Bool noViewBob = add(new Setting.Bool("no_view_bob", "Disable view bobbing", false));
	private final Setting.Bool noDamageTilt = add(new Setting.Bool("no_damage_tilt", "Disable damage tilt", false));

	private ParticleStatus savedParticles;
	private boolean savedBobView;
	private double savedDamageTilt;
	private boolean captured;
	private boolean lastReduceParticles;
	private boolean lastNoViewBob;
	private boolean lastNoDamageTilt;

	public VisualTogglesModule() {
		super("visual_toggles", "Visual Toggles",
				"Turns off particle effects, rain and snow, view bobbing and the hurt camera tilt. "
						+ "Each switch is separate; the original video settings come back when the module is turned off.",
				ModuleCategory.RENDER, false);
	}

	/** Read from the weather render hook; cheap and allocation-free. */
	public boolean weatherHidden() {
		return isEnabled() && hideWeather.get();
	}

	@Override
	protected void onEnable() {
		try {
			capture();
			apply(true);
			publishStatus();
		} catch (Throwable throwable) {
			Diagnostics.exception("visual_toggles.enable", throwable);
			markFailed("Failed: " + Diagnostics.describe(throwable) + " in VisualTogglesModule.onEnable() (see stack trace)");
		}
	}

	@Override
	protected void onDisable() {
		restore();
		publishStatus();
	}

	@Override
	public void tick() {
		if (!captured) {
			capture();
		}
		boolean particles = reduceParticles.get();
		boolean bob = noViewBob.get();
		boolean tilt = noDamageTilt.get();
		if (particles != lastReduceParticles || bob != lastNoViewBob || tilt != lastNoDamageTilt) {
			apply(false);
			publishStatus();
		}
	}

	private void capture() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null) {
				return;
			}
			savedParticles = client.options.particles().get();
			savedBobView = client.options.bobView().get();
			savedDamageTilt = client.options.damageTiltStrength().get();
			captured = true;
			Diagnostics.event("visual_toggles", "captured video options: particles " + savedParticles
					+ ", bob " + savedBobView + ", tilt " + savedDamageTilt);
		} catch (Throwable throwable) {
			Diagnostics.exception("visual_toggles.capture", throwable);
		}
	}

	private void apply(boolean announce) {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null) {
				return;
			}
			boolean particles = reduceParticles.get();
			boolean bob = noViewBob.get();
			boolean tilt = noDamageTilt.get();
			if (particles) {
				client.options.particles().set(ParticleStatus.MINIMAL);
			} else if (savedParticles != null) {
				client.options.particles().set(savedParticles);
			}
			client.options.bobView().set(!bob && savedBobView);
			client.options.damageTiltStrength().set(tilt ? 0.0D : savedDamageTilt);
			lastReduceParticles = particles;
			lastNoViewBob = bob;
			lastNoDamageTilt = tilt;
			if (announce) {
				Diagnostics.event("visual_toggles", "applied visual toggles");
			}
		} catch (Throwable throwable) {
			Diagnostics.exception("visual_toggles.apply", throwable);
		}
	}

	private void restore() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (!captured || client == null || client.options == null) {
				return;
			}
			if (savedParticles != null) {
				client.options.particles().set(savedParticles);
			}
			client.options.bobView().set(savedBobView);
			client.options.damageTiltStrength().set(savedDamageTilt);
			Diagnostics.event("visual_toggles", "restored the original video options");
		} catch (Throwable throwable) {
			Diagnostics.exception("visual_toggles.restore", throwable);
		} finally {
			captured = false;
			lastReduceParticles = false;
			lastNoViewBob = false;
			lastNoDamageTilt = false;
		}
	}

	/**
	 * Restores the player's original video options when leaving the world or quitting,
	 * without turning the module off. The next tick captures them again and re-applies
	 * the toggles, so options.txt never keeps OmniCube's temporary values.
	 */
	public void restoreForSession() {
		restore();
	}

	/** Safe low-end values, applied by the Low-end preset before the module is enabled. */
	public void applyLowEndValues() {
		reduceParticles.set(true);
		hideWeather.set(false);
		noViewBob.set(true);
		noDamageTilt.set(true);
	}

	public String statusLine() {
		StringBuilder builder = new StringBuilder("Visual toggles: ");
		builder.append(isEnabled() ? "ON" : "OFF");
		if (isEnabled()) {
			builder.append(" | particles ").append(reduceParticles.get() ? "minimal" : "vanilla");
			builder.append(", weather ").append(hideWeather.get() ? "hidden" : "shown");
			builder.append(", view bob ").append(noViewBob.get() ? "off" : "on");
			builder.append(", damage tilt ").append(noDamageTilt.get() ? "off" : "on");
		}
		return builder.toString();
	}

	private void publishStatus() {
		try {
			Diagnostics.note("visual_toggles", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("visual_toggles.status", throwable);
		}
	}
}
