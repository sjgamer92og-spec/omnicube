package dev.omnicube.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ContainerTooltip(List<ItemStack> stacks) implements TooltipComponent {
	public ContainerTooltip {
		stacks = List.copyOf(stacks);
	}
}
