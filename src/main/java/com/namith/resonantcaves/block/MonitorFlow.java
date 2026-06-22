package com.namith.resonantcaves.block;

import java.util.Locale;

import net.minecraft.util.StringIdentifiable;

/**
 * Which way the monitor's arrow texture is rotated, on-screen, to point in the direction energy
 * is actually flowing in the world — {@code NONE} while idle (no arrow shown at all). The cable's
 * real 3D flow direction is projected onto the monitor's own mounting plane (see
 * {@code MonitorBlockEntity#computeFlowRotation}) to pick one of these.
 */
public enum MonitorFlow implements StringIdentifiable {
	NONE,
	UP,
	RIGHT,
	DOWN,
	LEFT;

	@Override
	public String asString() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
