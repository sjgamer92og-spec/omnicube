package dev.omnicube;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. This class must stay free of client-only imports so the mod
 * keeps loading cleanly on a dedicated server if it is ever installed there.
 */
public final class OmniCubeMod implements ModInitializer {
	public static final String MOD_ID = "omnicube";
	public static final String MOD_NAME = "OmniCube";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] common entrypoint loaded", MOD_NAME);
	}
}
