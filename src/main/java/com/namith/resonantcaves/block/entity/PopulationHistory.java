package com.namith.resonantcaves.block.entity;

import net.minecraft.nbt.NbtCompound;

/**
 * A fixed-length circular buffer of ints, one sample per Minecraft day (appended at midnight),
 * up to 500 Minecraft days of population history. Shared contract with {@code PopulationGraphPanel}.
 */
public final class PopulationHistory {
	public static final int MAX_DAYS = 500;

	private final int[] samples = new int[MAX_DAYS];
	private int writeIndex;
	private int count;

	public void append(int population) {
		this.samples[this.writeIndex] = population;
		this.writeIndex = (this.writeIndex + 1) % MAX_DAYS;
		this.count = Math.min(this.count + 1, MAX_DAYS);
	}

	public int getCount() {
		return this.count;
	}

	/** Chronological order (oldest first), length = count. */
	public int[] snapshot() {
		int[] result = new int[this.count];
		for (int i = 0; i < this.count; i++) {
			result[i] = this.samples[(this.writeIndex - this.count + i + MAX_DAYS) % MAX_DAYS];
		}
		return result;
	}

	public void writeNbt(NbtCompound nbt, String key) {
		nbt.putIntArray(key, this.samples);
		nbt.putInt(key + "W", this.writeIndex);
		nbt.putInt(key + "C", this.count);
	}

	public void readNbt(NbtCompound nbt, String key) {
		int[] saved = nbt.getIntArray(key);
		if (saved.length == MAX_DAYS) {
			System.arraycopy(saved, 0, this.samples, 0, MAX_DAYS);
		}
		this.writeIndex = nbt.contains(key + "W") ? nbt.getInt(key + "W") % MAX_DAYS : 0;
		this.count = nbt.contains(key + "C") ? Math.min(nbt.getInt(key + "C"), MAX_DAYS) : 0;
	}
}
