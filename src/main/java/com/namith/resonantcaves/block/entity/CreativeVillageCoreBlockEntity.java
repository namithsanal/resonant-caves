package com.namith.resonantcaves.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Debug/testing variant of the Village Core: tops its own storage off to full immediately before
 * each daily checkpoint runs, so the whole house/villager/decoration growth loop can be observed
 * with no cable network, no Stations, and no risk of an unintended deficit.
 */
public class CreativeVillageCoreBlockEntity extends VillageCoreBlockEntity {
	public CreativeVillageCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CREATIVE_VILLAGE_CORE, pos, state);
	}

	@Override
	protected void beforeDailyCheckpoint() {
		this.fillToCapacity();
	}
}
