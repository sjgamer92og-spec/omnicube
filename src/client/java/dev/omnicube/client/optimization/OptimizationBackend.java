package dev.omnicube.client.optimization;

import net.minecraft.client.Minecraft;

/**
 * Performance backend contract.
 *
 * <p>The built-in implementation is deliberately conservative and only changes
 * be dropped in by calling {@code PerformanceModule.setBackend(...)} during
 * client initialisation; no feature code needs to change.</p>
 */
public interface OptimizationBackend {
	String id();

	/** Remember the user's original settings before anything is modified. */
	void capture(Minecraft client);

	/** Put every modified option back exactly as it was. */
	void restore(Minecraft client);

	/** Called once per client tick while the performance module is enabled. */
	void tick(Minecraft client, OptimizationSettings settings);
}
