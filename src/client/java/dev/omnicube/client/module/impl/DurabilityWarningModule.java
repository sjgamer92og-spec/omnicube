package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Warns when a worn or held item is nearly broken.
 *
 * <p>The inventory is inspected once a second, and a warning is shown at most once
 * per item until it is repaired or replaced, so nothing spams the chat.
 */
public final class DurabilityWarningModule extends Module {
	private static final int CHECK_INTERVAL_TICKS = 20;

	private final Setting.Num threshold = add(new Setting.Num("threshold", "Warn below (%)", 15.0D, 5.0D, 50.0D, 5.0D));
	private final Setting.Bool includeHeld = add(new Setting.Bool("include_held", "Include the held item", true));
	private int tickCounter;
	private final Set<String> warned = new HashSet<>();

	public DurabilityWarningModule() {
		super("durability_warning", "Low Durability Warning",
				"Shows a chat warning when armour or your held item is nearly broken. "
						+ "Each item warns once until it is repaired or replaced.",
				ModuleCategory.PLAYER, false);
	}

	@Override
	protected void onEnable() {
		tickCounter = 0;
		warned.clear();
		publishStatus();
	}

	@Override
	protected void onDisable() {
		warned.clear();
		publishStatus();
	}

	@Override
	public void tick() {
		if (++tickCounter < CHECK_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;
		try {
			Minecraft client = Minecraft.getInstance();
			LocalPlayer player = client.player;
			if (player == null) {
				warned.clear();
				return;
			}
			int percentLimit = threshold.intValue();
			Set<String> stillLow = new HashSet<>();
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (slot == EquipmentSlot.MAINHAND && !includeHeld.get()) {
					continue;
				}
				if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
						&& slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
					continue;
				}
				check(player, player.getItemBySlot(slot), slot.name(), percentLimit, stillLow);
			}
			warned.retainAll(stillLow);
		} catch (Throwable throwable) {
			Diagnostics.exception("durability_warning.tick", throwable);
		}
	}

	private void check(LocalPlayer player, ItemStack stack, String slot, int percentLimit, Set<String> stillLow) {
		if (stack == null || stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
			return;
		}
		int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
		int percent = (int) Math.round(remaining * 100.0D / stack.getMaxDamage());
		if (percent > percentLimit) {
			return;
		}
		String name = stack.getHoverName().getString();
		String key = slot + "|" + name;
		stillLow.add(key);
		if (warned.add(key)) {
			OmniCubeText.sendError("Low durability: " + name + " has " + remaining + " uses left (" + percent + "%).");
			Diagnostics.event("durability_warning", slot + " " + name + " at " + percent + "%");
		}
	}

	public String statusLine() {
		return "Low durability: " + (isEnabled() ? "ON" : "OFF") + " | warns below "
				+ threshold.intValue() + "% | " + warned.size() + " item(s) warned";
	}

	private void publishStatus() {
		try {
			Diagnostics.note("durability_warning", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("durability_warning.status", throwable);
		}
	}
}
