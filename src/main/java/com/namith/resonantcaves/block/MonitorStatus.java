package com.namith.resonantcaves.block;

import java.util.Locale;

import net.minecraft.util.StringIdentifiable;

/**
 * Ambient status shown by a monitor block's texture/model — no GUI, just look at it.
 * {@code IDLE} means no adjacent cable was found (or it's carrying no flow).
 */
public enum MonitorStatus implements StringIdentifiable {
	IDLE,
	UNDERUTILIZED,
	OPTIMAL,
	OVERLOADED;

	@Override
	public String asString() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
