package dev.omnicube.client.module.impl;

import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;
import dev.omnicube.client.util.OmniCubeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * One keypress puts the best hotbar tool for the block you are looking at into your
 * hand. Only the nine hotbar slots are compared, only when the key is pressed, and
 * the block comes from the existing crosshair pick, so there is no scanning.
 */
public final class ToolSwapModule extends Module {
	private final Setting.Bool announce = add(new Setting.Bool("announce", "Message in chat", false));

	public ToolSwapModule() {
		super("tool_swap", "Tool Swap Helper",
				"Press the Tool Swap keybind (unbound by default) to select the best tool in your hotbar "
						+ "for the block you are looking at.",
				ModuleCategory.PLAYER, false);
	}

	@Override
	protected void onEnable() {
		publishStatus();
	}

	@Override
	protected void onDisable() {
		publishStatus();
	}

	/** Called from the keybind handler. Returns true when a different slot was selected. */
	public boolean swapToBestTool(Minecraft client) {
		if (!isEnabled() || client == null || client.player == null || client.level == null) {
			return false;
		}
		try {
			HitResult hit = client.hitResult;
			if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
				message("Look at a block first.");
				return false;
			}
			BlockState state = client.level.getBlockState(blockHit.getBlockPos());
			LocalPlayer player = client.player;
			int current = player.getInventory().getSelectedSlot();
			int best = -1;
			float bestSpeed = 1.0F;
			String bestName = "";
			for (int slot = 0; slot < 9; slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.isEmpty()) {
					continue;
				}
				float speed = stack.getDestroySpeed(state);
				if (speed > bestSpeed) {
					bestSpeed = speed;
					best = slot;
					bestName = stack.getHoverName().getString();
				}
			}
			if (best < 0 || best == current) {
				message(best < 0 ? "No better tool in your hotbar." : "Already holding the best tool.");
				return false;
			}
			player.getInventory().setSelectedSlot(best);
			Diagnostics.event("tool_swap", "selected slot " + best + " (" + bestName + ") at speed " + bestSpeed);
			message("Switched to " + bestName + ".");
			return true;
		} catch (Throwable throwable) {
			Diagnostics.exception("tool_swap.swap", throwable);
			OmniCubeText.sendError("Tool swap failed; see the diagnostics log.");
			return false;
		}
	}

	private void message(String text) {
		if (announce.get()) {
			OmniCubeText.send(text);
		}
	}

	public String statusLine() {
		return "Tool swap: " + (isEnabled() ? "ON" : "OFF");
	}

	private void publishStatus() {
		try {
			Diagnostics.note("tool_swap", statusLine());
		} catch (Throwable throwable) {
			Diagnostics.exception("tool_swap.status", throwable);
		}
	}
}
