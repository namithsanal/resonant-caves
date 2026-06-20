package com.namith.resonantcaves;

import com.namith.resonantcaves.block.ModBlocks;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.energy.LivingTreeEnergyStorage;
import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
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

		// Feature 2: large herds of sheep, cows, pigs, chickens, and horses.
		HerdSpawning.register();

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

		// Feature 8: Living Tree — a vanilla log on grass, fed RF by a cable, grows into a tree
		// of its own species and shrinks (dropping logs normally) when power isn't sustained.
		EnergyStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, direction) -> {
			Block below = world.getBlockState(pos.down()).getBlock();
			if (!(world instanceof ServerWorld serverWorld) || !(below == Blocks.GRASS_BLOCK || below == Blocks.DIRT)) {
				return null;
			}
			LivingTreeState treeState = LivingTreeState.getOrCreate(serverWorld);
			LivingTreeState.TreeData data = treeState.getOrCreateTree(pos.toImmutable(), state.getBlock());
			return new LivingTreeEnergyStorage(data);
		}, LivingTreeSpecies.rootableLogs());
		LivingTreeGrowth.register();
	}
}
