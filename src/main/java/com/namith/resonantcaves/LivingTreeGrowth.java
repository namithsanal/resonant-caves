package com.namith.resonantcaves;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
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
 * Feature 8 — Living Tree growth/shrink loop, implemented as an L-system (per the brainstorm's
 * "fractal cloning" recommendation): every active tip remembers the direction it's currently
 * heading, and growth either continues that direction (self-similar extension, more likely for
 * low-branchChance species like spruce) or rolls a fresh species-weighted direction (a branch/kink).
 * Every {@link #CHECK_INTERVAL_TICKS}, each tracked root compares the RF it received since the last
 * check against the rate its current size would need ({@code 2^(size-1)}, the inverse of the
 * brainstorm's settled "size = log2(RF) + 1") and grows or shrinks by exactly one log. Checking only
 * periodically (rather than reacting every tick) is what makes the response track *sustained* power
 * rather than momentary spikes/dips, with no separate hysteresis bookkeeping needed for that.
 */
public final class LivingTreeGrowth {
	private static final int CHECK_INTERVAL_TICKS = 20; // 1s — TODO: revert to 200 (10s) after testing
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

	// If a player chops down part of the tree directly (rather than it dying off via shrink()),
	// drop the missing positions so the size count and tip set stay accurate.
	private static void pruneMissingLogs(ServerWorld world, LivingTreeState.TreeData data) {
		Iterator<BlockPos> it = data.logs.iterator();
		while (it.hasNext()) {
			BlockPos pos = it.next();
			if (!pos.equals(data.rootPos) && !world.getBlockState(pos).isOf(data.logBlock)) {
				it.remove();
				data.tipDirections.remove(pos);
				data.parents.remove(pos);
			}
		}
	}

	private static void checkAndGrowOrShrink(ServerWorld world, LivingTreeState.TreeData data) {
		int size = data.logs.size();
		// Grow/shrink thresholds are deliberately NOT the same value. targetRf doubles every size
		// step, so a single shared threshold means almost any constant inflow lands between two
		// thresholds and the tree flaps a log on and off every single check, forever, never
		// accumulating enough logs to grow further or show any branching. Using the PREVIOUS
		// size's threshold as the shrink trigger gives each size a stable band of inflow values
		// it's happy at, matching the brainstorm's "size = log2(RF) + 1" on average without ever
		// being exactly on a knife's edge.
		long growThreshold = 1L << Math.min(size - 1, 62);
		long shrinkThreshold = size > 1 ? 1L << Math.min(size - 2, 62) : 0L;
		long inflow = data.inflowSinceLastCheck;
		data.inflowSinceLastCheck = 0;

		LivingTreeSpecies.Params species = LivingTreeSpecies.get(data.logBlock);
		if (species == null) {
			return;
		}

		if (inflow > growThreshold) {
			grow(world, data, species);
		} else if (size > 1 && inflow < shrinkThreshold) {
			shrink(world, data, species);
		}
	}

	private static void grow(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		if (data.tipDirections.isEmpty()) {
			return;
		}
		Random random = world.getRandom();
		// Uniform pick across all active tips — NOT biased toward the tallest one. An earlier
		// version always preferred the highest tip, which meant one branch raced upward forever
		// while every other branch got starved of turns and never grew past a stub. Giving every
		// tip an equal chance is what actually produces a spreading canopy.
		List<BlockPos> tipList = new ArrayList<>(data.tipDirections.keySet());
		BlockPos tip = tipList.get(random.nextInt(tipList.size()));
		Direction currentDirection = data.tipDirections.get(tip);
		// Low-branchChance species (e.g. spruce) mostly continue straight; high-branchChance ones
		// (e.g. jungle) kink/reroll a fresh direction more often — this is the "F -> F[+F]F" rule.
		float continueChance = 1.0F - species.branchChance();

		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			Direction direction = (attempt == 0 && random.nextFloat() < continueChance)
					? currentDirection
					: randomDirection(random, species);
			BlockPos candidate = tip.offset(direction);
			if (world.getBlockState(candidate).isAir()) {
				world.setBlockState(candidate, species.logBlock().getDefaultState());
				data.logs.add(candidate);
				data.parents.put(candidate, tip);
				boolean branch = random.nextFloat() < species.branchChance();
				if (branch) {
					// Parent keeps growing too, from a freshly rolled direction — a true fork.
					data.tipDirections.put(tip, randomDirection(random, species));
				} else {
					data.tipDirections.remove(tip);
				}
				data.tipDirections.put(candidate, direction);
				return;
			}
		}
	}

	private static void shrink(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		List<BlockPos> candidates = new ArrayList<>(data.tipDirections.keySet());
		candidates.remove(data.rootPos);
		if (candidates.isEmpty()) {
			return;
		}
		Random random = world.getRandom();
		BlockPos victim = candidates.get(random.nextInt(candidates.size()));

		world.breakBlock(victim, true, null, 512);
		data.logs.remove(victim);
		data.tipDirections.remove(victim);
		BlockPos parent = data.parents.remove(victim);
		// If the parent wasn't already an active tip (i.e. growth had continued single-threaded
		// through it), removing its only child reopens it as the new frontier — the brainstorm's
		// "the tree remembers what happened" dieback, regrowing in a possibly different direction.
		if (parent != null && data.logs.contains(parent) && !data.tipDirections.containsKey(parent)) {
			data.tipDirections.put(parent, randomDirection(random, species));
		}
		// Leaves are disabled for now (see regenerateLeaves) while the log skeleton is being tuned.
	}

	// upwardBias picks UP outright; the remainder splits between DOWN (drooping, e.g. mangrove)
	// and a level HORIZONTAL step, weighted by horizontalSpread.
	private static Direction randomDirection(Random random, LivingTreeSpecies.Params species) {
		float roll = random.nextFloat();
		if (roll < species.upwardBias()) {
			return Direction.UP;
		}
		float remaining = 1.0F - species.upwardBias();
		if (roll < species.upwardBias() + remaining * species.horizontalSpread()) {
			return Direction.DOWN;
		}
		return HORIZONTALS[random.nextInt(HORIZONTALS.length)];
	}

	private static void regenerateLeaves(ServerWorld world, LivingTreeState.TreeData data, LivingTreeSpecies.Params species) {
		for (BlockPos pos : data.leaves) {
			if (world.getBlockState(pos).isOf(species.leafBlock())) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
		}
		data.leaves.clear();

		for (BlockPos tip : data.tipDirections.keySet()) {
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
