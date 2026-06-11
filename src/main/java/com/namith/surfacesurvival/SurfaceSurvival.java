package com.namith.surfacesurvival;

import com.namith.surfacesurvival.block.ModBlocks;
import com.namith.surfacesurvival.block.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import team.reborn.energy.api.EnergyStorage;

public class SurfaceSurvival implements ModInitializer {
	public static final String MOD_ID = "surfacesurvival";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Surface Survival initializing");

		// Feature 1: stone and deepslate cannot be mined.
		UnbreakableBlocks.register();

		// Feature 2: large herds of sheep, cows, pigs, chickens, and horses.
		HerdSpawning.register();

		// Feature 3: Resonant Ore — naturally-occurring unbreakable RF source.
		ModBlocks.register();
		ModBlockEntities.register();
		ResonantOreWorldGen.register();
		EnergyStorage.SIDED.registerForBlockEntity(
				(blockEntity, direction) -> blockEntity.getEnergyStorage(),
				ModBlockEntities.RESONANT_ORE);
	}
}
