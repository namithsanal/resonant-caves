package com.namith.resonantcaves.block;

/**
 * Implemented by station block entities to let the Resonant Wand draw energy from whichever
 * one is nearest, regardless of facing — unlike {@code EnergyStorage.SIDED}, this ignores any
 * per-face output throttle (e.g. a real station's {@code targetOutput}), since the wand isn't
 * plugged into a designated output face.
 */
public interface WandPowerSource {
	/** Draws up to {@code maxAmount} RF. Returns the amount actually drawn (may be less, or zero). */
	long drawEnergyForWand(long maxAmount);
}
