package com.namith.resonantcaves.block.entity;

/**
 * Implemented by any block entity whose station-style GUI (live stored-energy readout + a
 * precise output setter) can be opened — {@code StationBlockEntity} and the Creative Station's
 * block entity — so {@code ModNetworking}'s periodic push doesn't need to know about either
 * concrete type.
 */
public interface EnergyScreenSource {
	long getStoredEnergyForDisplay();

	void setTargetOutput(long value);
}
