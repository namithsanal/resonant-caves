package com.namith.resonantcaves.block.entity;

import net.minecraft.nbt.NbtCompound;

/**
 * A fixed-length circular buffer of longs, sampled once per second (3 in-game days = 3,600
 * samples), with NBT persistence and a chronological snapshot. Shared by {@code
 * MonitorBlockEntity} (cable throughput) and {@code StationBlockEntity} (stored energy) — both
 * sample "some long value, once a second, into a 3-day rolling window" and previously
 * reimplemented this independently.
 */
public final class SampleHistory {
	public static final int LENGTH = 3600;

	private final long[] samples = new long[LENGTH];
	private int writeIndex;

	public void append(long sample) {
		this.samples[this.writeIndex] = sample;
		this.writeIndex = (this.writeIndex + 1) % LENGTH;
	}

	/** The most recently appended sample. */
	public long getLatest() {
		int lastIndex = (this.writeIndex - 1 + LENGTH) % LENGTH;
		return this.samples[lastIndex];
	}

	/** Chronological order (oldest first), for an initial GUI-open payload. */
	public long[] snapshot() {
		long[] result = new long[LENGTH];
		for (int i = 0; i < LENGTH; i++) {
			result[i] = this.samples[(this.writeIndex + i) % LENGTH];
		}
		return result;
	}

	public void writeNbt(NbtCompound nbt, String key) {
		nbt.putLongArray(key, this.samples);
		nbt.putInt(key + "WriteIndex", this.writeIndex);
	}

	public void readNbt(NbtCompound nbt, String key) {
		long[] saved = nbt.getLongArray(key);
		if (saved.length == LENGTH) {
			System.arraycopy(saved, 0, this.samples, 0, LENGTH);
		}
		this.writeIndex = nbt.contains(key + "WriteIndex") ? nbt.getInt(key + "WriteIndex") % LENGTH : 0;
	}
}
