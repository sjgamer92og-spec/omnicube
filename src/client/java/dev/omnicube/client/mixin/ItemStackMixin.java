package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.module.impl.TooltipModule;
import dev.omnicube.client.tooltip.ContainerTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = ItemStack.class, priority = 1500)
public abstract class ItemStackMixin {
	private static final int MAX_PREVIEW_STACKS = 27;

	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true, require = 0)
	private void omnicube$containerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> callbackInfo) {
		try {
			Optional<TooltipComponent> vanilla = callbackInfo.getReturnValue();
			if (vanilla != null && vanilla.isPresent()) {
				return;
			}
			TooltipModule module = OmniCubeClient.tooltips();
			if (module == null || !module.visualPreviewEnabled()) {
				return;
			}
			ItemStack stack = (ItemStack) (Object) this;
			ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
			if (contents == null) {
				return;
			}
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack stored : contents.nonEmptyItemsCopy()) {
				if (stacks.size() >= MAX_PREVIEW_STACKS) {
					break;
				}
				if (stored != null && !stored.isEmpty()) {
					stacks.add(stored);
				}
			}
			if (!stacks.isEmpty()) {
				callbackInfo.setReturnValue(Optional.of(new ContainerTooltip(stacks)));
			}
		} catch (Throwable throwable) {
			// Keep vanilla tooltips working if a stack has unusual container data.
			TooltipModule module = OmniCubeClient.tooltips();
			if (module != null) {
				module.fail(throwable);
			}
		}
	}
}
