package com.namith.resonantcaves;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Feature 8 — Living Tree growth/shrink loop. Every {@link #CHECK_INTERVAL_TICKS}, each tracked
 * root compares the RF it received since the last check against the rate its current size would
 * need ({@code 2^(size-1)}, the inverse of the brainstorm's settled "size = log2(RF) + 1") and
 * grows or shrinks by exactly one log. Checking only periodically (rather than reacting to every
 * tick) is what makes the response feel like it's tracking *sustained* power rather than momentary
 * spikes/dips — no separate hysteresis bookkeeping is needed for that.
 */
public final class LivingTreeGrowth {
	private static final int CHECK_INTERVAL_TICKS = 200; // 10s
	private static final int PLACEMENT_ATTEMPTS = 6;
	private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	private LivingTreeGrowth() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(LivingTreeGrowth::tick);
	}

	private static void tick(MinecraftServer server) {
		boolean checkNow = server.getTicks() % CHECK_INTERVAL_TICKS == 0;
		for (ServerWorld world : server.getWorlds()) {
			LivingTreeState state = LivingTreeState.getOrCreate(world);
			Iterator<Map.Entry<BlockPos, LivingTreeState.TreeData>> it = state.getTrees().entrySet().iterator();
			List<LivingTreeState.TreeData> toProcess = new ArrayList<>();
			while (it.hasNext()) {
				LivingTreeState.TreeData data = it.next().getValue();
				if (!world.getBlockState(data.rootPos).isOf(data.logBlock)) {
					it.remove();
					state.markDirty();
					continue;
				}
				pruneMissingLogs(world, data);
				if (checkNow) {
					toProcess.add(data);
				}
			}
			for (LivingTreeState.TreeData data : toProcess) {
				checkAndGrowOrShrink(world, data);
			}
		}
	}

	private static void pruneMissingLogs(ServerWorld world, LivingTreeState.TreeData data) {
		data.logs.removeIf(pos -> !pos.equals(data.rootPos) && !world.getBlockState(pos).isOf(data.logBlock));
	}

	private static void checkAndGrowOrShrink(ServerWorld world, LivingTreeState.TreeData data) {
		int size = data.logs.size();
		long targetRf = 1L << Math.min(size - 1, 62);
		long inflow = data.inflowSinceLastCheck;
		data.inflowSinceLastCheck = 0;

		LivingTreeSpecies.Params species = LivingTreeSpecies.get(data.logBlock);
		if (species == null) {
			return;
		}

		if (inflow > targetRf) {
			grow(world, data, species);
		} else if (inflow < targetRf && size > 1) {
			shrink(world, data, species);
		}
	}

	private static void grow(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		Random random = world.getRandom();
		List<BlockPos> tips = computeTips(data.logs);
		if (tips.isEmpty()) {
			return;
		}
		BlockPos tip = tips.get(random.nextInt(tips.size()));

		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			BlockPos candidate = tip.add(randomGrowthOffset(random, species));
			if (world.getBlockState(candidate).isAir()) {
				world.setBlockState(candidate, species.logBlock().getDefaultState());
				data.logs.add(candidate);
				regenerateLeaves(world, data, species);
				return;
			}
		}
	}

	private static void shrink(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		List<BlockPos> tips = computeTips(data.logs);
		tips.remove(data.rootPos);
		if (tips.isEmpty()) {
			return;
		}
		// "Newest" tip = the one with the highest index in the insertion-ordered logs list.
		BlockPos newestTip = tips.get(0);
		int bestIndex = -1;
		for (BlockPos tip : tips) {
			int index = data.logs.indexOf(tip);
			if (index > bestIndex) {
				bestIndex = index;
				newestTip = tip;
			}
		}

		world.breakBlock(newestTip, true, null, 512);
		data.logs.remove(newestTip);
		regenerateLeaves(world, data, species);
	}

	/** Tips = logs with at most one of their 6 face-adjacent neighbors also being a log of this tree. */
	private static List<BlockPos> computeTips(List<BlockPos> logs) {
		List<BlockPos> tips = new ArrayList<>();
		for (BlockPos pos : logs) {
			int neighbors = 0;
			for (Direction direction : Direction.values()) {
				if (logs.contains(pos.offset(direction))) {
					neighbors++;
				}
			}
			if (neighbors <= 1) {
				tips.add(pos);
			}
		}
		return tips;
	}

	private static BlockPos randomGrowthOffset(Random random, LivingTreeSpecies.Params species) {
		if (random.nextFloat() < species.upwardBias()) {
			return BlockPos.ORIGIN.up();
		}
		Direction horizontal = HORIZONTALS[random.nextInt(HORIZONTALS.length)];
		BlockPos offset = BlockPos.ORIGIN.offset(horizontal);
		if (random.nextFloat() < species.horizontalSpread()) {
			offset = offset.down();
		}
		return offset;
	}

	private static void regenerateLeaves(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		for (BlockPos pos : data.leaves) {
			if (world.getBlockState(pos).isOf(species.leafBlock())) {
				world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
			}
		}
		data.leaves.clear();

		for (BlockPos tip : computeTips(data.logs)) {
			for (BlockPos mutablePos : BlockPos.iterate(tip.add(-1, -1, -1), tip.add(1, 1, 1))) {
				BlockPos pos = mutablePos.toImmutable();
				if (!pos.equals(tip) && world.getBlockState(pos).isAir()) {
					world.setBlockState(pos, species.leafBlock().getDefaultState());
					data.leaves.add(pos);
				}
			}
		}
	}
}
