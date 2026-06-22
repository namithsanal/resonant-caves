package com.namith.resonantcaves.energy;

import java.util.function.LongSupplier;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * A stateless, extract-only energy store that always reports {@code rate} RF available and
 * refuses all insertion — energy nothing extracts in a given tick is simply never produced, so
 * there's no internal buffer and nothing needs saving. Shared by Resonant Ore (Feature 3, whose
 * rate is a fixed 0-15 roll persisted on the block entity) and the Creative Station (Feature 8,
 * whose rate is the player-set target output) — both "materialize energy from nothing at a fixed
 * rate," differing only in what decides that rate.
 */
public class FixedRateEnergyStorage implements EnergyStorage {
	private final LongSupplier rate;

	public FixedRateEnergyStorage(LongSupplier rate) {
		this.rate = rate;
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public boolean supportsInsertion() {
		return false;
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		return Math.min(maxAmount, this.rate.getAsLong());
	}

	@Override
	public long getAmount() {
		return this.rate.getAsLong();
	}

	@Override
	public long getCapacity() {
		return this.rate.getAsLong();
	}
}
