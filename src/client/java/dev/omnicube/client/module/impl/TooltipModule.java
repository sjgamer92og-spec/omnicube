package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public final class TooltipModule extends Module {
	private final Setting.Bool containerPreview = add(new Setting.Bool("container_preview", "Container text preview", false));
	private final Setting.Bool visualPreview = add(new Setting.Bool("visual_container_preview", "Container item grid", true));
	private final Setting.Bool itemId = add(new Setting.Bool("item_id", "Show item ID", false));
	private final Setting.Bool durability = add(new Setting.Bool("durability", "Durability numbers", true));
	private final Setting.Bool foodValues = add(new Setting.Bool("food_values", "Food values", true));
	private final Setting.Num maxLines = add(new Setting.Num("max_container_lines", "Container lines", 8.0D, 1.0D, 27.0D, 1.0D));

	public TooltipModule() {
		super("tooltips", "Better Tooltips", "Adds shulker/container previews, durability, food values and optional item IDs to tooltips.", ModuleCategory.MISC, true);
	}

	public boolean visualPreviewEnabled() {
		return isEnabled() && visualPreview.get();
	}

	public void appendTooltip(ItemStack stack, List<Component> lines) {
		if (!isEnabled() || stack.isEmpty()) {
			return;
		}

		if (itemId.get()) {
			try {
				var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
				lines.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
			} catch (Throwable ignored) {
				// Registry access can be odd during a reload; the tooltip just stays clean.
			}
		}

		if (durability.get() && stack.isDamageableItem()) {
			int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
			lines.add(Component.literal("Durability: " + remaining + " / " + stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
		}

		if (foodValues.get()) {
			try {
				FoodProperties food = stack.get(DataComponents.FOOD);
				if (food != null) {
					lines.add(Component.literal("Food: " + food.nutrition() + " (" + String.format(java.util.Locale.ROOT, "%.1f", food.saturation()) + " saturation)")
							.withStyle(ChatFormatting.GRAY));
				}
			} catch (Throwable ignored) {
				// Not every stack exposes a food component.
			}
		}

		if (containerPreview.get()) {
			try {
				ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
				if (contents == null) {
					return;
				}
				int limit = maxLines.intValue();
				int shown = 0;
				int total = 0;
				lines.add(Component.literal("Contents:").withStyle(ChatFormatting.GOLD));
				for (ItemStack stored : contents.nonEmptyItems()) {
					total++;
					if (shown < limit) {
						lines.add(Component.literal("  " + stored.getCount() + "x " + stored.getHoverName().getString())
								.withStyle(ChatFormatting.GRAY));
						shown++;
					}
				}
				if (total == 0) {
					lines.add(Component.literal("  (empty)").withStyle(ChatFormatting.DARK_GRAY));
				} else if (total > shown) {
					lines.add(Component.literal("  +" + (total - shown) + " more").withStyle(ChatFormatting.DARK_GRAY));
				}
			} catch (Throwable ignored) {
				// Container data is optional and may be malformed on a server.
			}
		}
	}
}
