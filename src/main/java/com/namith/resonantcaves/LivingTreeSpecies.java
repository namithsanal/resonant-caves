package com.namith.resonantcaves;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Map;

/**
 * Feature 8 — per-log-type growth parameters for the living tree mechanic. Each of the 8 standard
 * overworld log species grows with a different branch chance / upward bias / horizontal spread,
 * so a root started from a different log type produces a visibly different tree shape.
 */
public final class LivingTreeSpecies {
	public record Params(Block logBlock, Block leafBlock, float branchChance, float upwardBias, float horizontalSpread) {
	}

	private static final Map<Block, Params> BY_LOG = Map.ofEntries(
			Map.entry(Blocks.OAK_LOG, new Params(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 0.35F, 0.6F, 0.4F)),
			Map.entry(Blocks.SPRUCE_LOG, new Params(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 0.15F, 0.9F, 0.1F)),
			Map.entry(Blocks.BIRCH_LOG, new Params(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 0.25F, 0.7F, 0.3F)),
			Map.entry(Blocks.JUNGLE_LOG, new Params(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, 0.5F, 0.5F, 0.6F)),
			Map.entry(Blocks.ACACIA_LOG, new Params(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.4F, 0.4F, 0.7F)),
			Map.entry(Blocks.DARK_OAK_LOG, new Params(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, 0.45F, 0.5F, 0.5F)),
			Map.entry(Blocks.MANGROVE_LOG, new Params(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, 0.5F, 0.3F, 0.8F)),
			Map.entry(Blocks.CHERRY_LOG, new Params(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, 0.3F, 0.6F, 0.5F)));

	private LivingTreeSpecies() {
	}

	public static boolean isRootableLog(Block block) {
		return BY_LOG.containsKey(block);
	}

	public static Params get(Block logBlock) {
		return BY_LOG.get(logBlock);
	}

	public static Block[] rootableLogs() {
		return BY_LOG.keySet().toArray(new Block[0]);
	}
}
