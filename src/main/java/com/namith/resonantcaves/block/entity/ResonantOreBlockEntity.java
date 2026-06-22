package com.namith.resonantcaves.block.entity;

import java.util.Random;

import com.namith.resonantcaves.energy.FixedRateEnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import team.reborn.energy.api.EnergyStorage;

/**
 * Feature 3 — Resonant Ore.
 *
 * <p>Stores a single {@code generationRate} (0-15 RF/t), rolled once from a triangular
 * distribution peaking around 4 (so most nodes are modest and high-output nodes are rare) and
 * seeded deterministically from the block's position, so the same node always rolls the same
 * value even before its first save. The value is also persisted to NBT.
 */
public class ResonantOreBlockEntity extends BlockEntity {
	private static final double MIN_RATE = 0.0;
	private static final double MODE_RATE = 4.0;
	private static final double MAX_RATE = 15.0;

	private final EnergyStorage energyStorage = new FixedRateEnergyStorage(this::getGenerationRate);
	private int generationRate;

	public ResonantOreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RESONANT_ORE, pos, state);
		this.generationRate = rollGenerationRate(pos);
	}

	public int getGenerationRate() {
		return this.generationRate;
	}

	public EnergyStorage getEnergyStorage() {
		return this.energyStorage;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("GenerationRate", this.generationRate);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.generationRate = nbt.getInt("GenerationRate");
	}

	private static int rollGenerationRate(BlockPos pos) {
		double u = new Random(pos.asLong()).nextDouble();
		double split = (MODE_RATE - MIN_RATE) / (MAX_RATE - MIN_RATE);
		double value = u < split
				? MIN_RATE + Math.sqrt(u * (MAX_RATE - MIN_RATE) * (MODE_RATE - MIN_RATE))
				: MAX_RATE - Math.sqrt((1 - u) * (MAX_RATE - MIN_RATE) * (MAX_RATE - MODE_RATE));
		return (int) Math.round(value);
	}
}
