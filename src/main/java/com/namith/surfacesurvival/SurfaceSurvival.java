package com.namith.surfacesurvival;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurfaceSurvival implements ModInitializer {
	public static final String MOD_ID = "surfacesurvival";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Surface Survival initializing");

		// Feature 1: stone and deepslate cannot be mined.
		UnbreakableBlocks.register();
	}
}
