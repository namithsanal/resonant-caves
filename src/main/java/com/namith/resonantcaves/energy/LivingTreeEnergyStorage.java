package com.namith.resonantcaves.energy;

import com.namith.resonantcaves.LivingTreeState;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * Feature 8 — Living Tree. The mirror image of {@link ResonantEnergyStorage}: insertion-only,
 * refuses extraction. Accepted RF isn't buffered anywhere — it's tallied directly onto the tree's
 * {@code inflowSinceLastCheck} counter, which {@code LivingTreeGrowth} reads and resets every
 * check interval to decide whether the tree should grow or shrink.
 */
public class LivingTreeEnergyStorage implements EnergyStorage {
	private final LivingTreeState.TreeData treeData;

	public LivingTreeEnergyStorage(LivingTreeState.TreeData treeData) {
		this.treeData = treeData;
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		treeData.inflowSinceLastCheck += maxAmount;
		return maxAmount;
	}

	@Override
	public boolean supportsInsertion() {
		return true;
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public boolean supportsExtraction() {
		return false;
	}

	@Override
	public long getAmount() {
		return treeData.inflowSinceLastCheck;
	}

	@Override
	public long getCapacity() {
		return Long.MAX_VALUE;
	}
}
