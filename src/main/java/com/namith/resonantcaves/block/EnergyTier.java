package com.namith.resonantcaves.block;

import org.jetbrains.annotations.Nullable;

/**
 * Shared tier data for cables and stations, worst-to-best: {@code IRON} is the cheap/no-frills
 * tier, {@code GOLD} the premium one. Cable stats drive {@code CableNetworkTicker}'s loss
 * formula; {@code stationLeakRatePerSecond} drives {@code StationBlockEntity}'s continuous
 * passive drain.
 */
public enum EnergyTier {
	IRON(0, 0.05, 0.005),
	COPPER(100, 0.02, 0.0025),
	GOLD(10000, 0.005, 0.001);

	/** Comfortable throughput (RF/t) below which this cable's loss is steeply penalized. 0 = no minimum. */
	public final long minThroughput;
	/** Flat energy loss fraction per cable segment in the network, before the underutilization penalty. */
	public final double lossPerCable;
	/** Fraction of a station's stored energy lost per second, regardless of insert/extract activity. */
	public final double stationLeakRatePerSecond;

	EnergyTier(long minThroughput, double lossPerCable, double stationLeakRatePerSecond) {
		this.minThroughput = minThroughput;
		this.lossPerCable = lossPerCable;
		this.stationLeakRatePerSecond = stationLeakRatePerSecond;
	}

	/**
	 * Combines this tier's flat per-cable loss (compounded once per cable in the network, using
	 * network size as a distance proxy) with a steep penalty for demand below
	 * {@link #minThroughput} (no penalty if there is no minimum, i.e. on {@code IRON}).
	 */
	public double lossFraction(long throughputThisTick, int networkCableCount) {
		double underutilization = (this.minThroughput == 0)
				? 1.0
				: Math.min(1.0, throughputThisTick / (double) this.minThroughput);
		double baseEfficiency = Math.pow(1.0 - this.lossPerCable, networkCableCount);
		return 1.0 - (baseEfficiency * underutilization);
	}

	/** The next tier up (e.g. {@code IRON.nextTier() == COPPER}), or {@code null} for {@code GOLD}. */
	@Nullable
	public EnergyTier nextTier() {
		EnergyTier[] values = values();
		int next = this.ordinal() + 1;
		return next < values.length ? values[next] : null;
	}
}
