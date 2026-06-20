package com.namith.resonantcaves;

import com.namith.resonantcaves.block.ModBlocks;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import team.reborn.energy.api.EnergyStorage;

public class ResonantCaves implements ModInitializer {
	public static final String MOD_ID = "resonantcaves";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Resonant Caves initializing");

		// Feature 1: stone and deepslate cannot be mined.
		UnbreakableBlocks.register();

		// Feature 6: creeper pack spawning.
		CreeperPackSpawning.register();

		// Feature 3: Resonant Ore — naturally-occurring unbreakable RF source.
		ModBlocks.register();
		ModBlockEntities.register();
		ResonantOreWorldGen.register();
		EnergyStorage.SIDED.registerForBlockEntity(
				(blockEntity, direction) -> blockEntity.getEnergyStorage(),
				ModBlockEntities.RESONANT_ORE);

		// Feature 7: Resonant Helmet — gold-tier armor with a Night Vision + hostile-mob radar effect.
		// Not craftable; zombies sometimes spawn wearing one and always drop it on death.
		ModItems.register();
		NightVisionRefresher.register();
		ZombieHelmetSpawning.register();
	}
}
