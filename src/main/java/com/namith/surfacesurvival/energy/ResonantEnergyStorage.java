package com.namith.surfacesurvival.energy;

import java.util.function.IntSupplier;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * Feature 3 — Resonant Ore.
 *
 * <p>A stateless, extract-only energy store: it always reports {@code generationRate} RF
 * available and refuses all insertion. There is no internal buffer, so energy nothing extracts in
 * a given tick is simply never produced — nothing is stored, and nothing needs saving.
 */
public class ResonantEnergyStorage implements EnergyStorage {
	private final IntSupplier generationRate;

	public ResonantEnergyStorage(IntSupplier generationRate) {
		this.generationRate = generationRate;
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
		return Math.min(maxAmount, this.generationRate.getAsInt());
	}

	@Override
	public long getAmount() {
		return this.generationRate.getAsInt();
	}

	@Override
	public long getCapacity() {
		return this.generationRate.getAsInt();
	}
}
