package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.block.WandPowerSource;
import com.namith.resonantcaves.energy.FixedRateEnergyStorage;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenStationScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import team.reborn.energy.api.EnergyStorage;

/**
 * A debug/testing energy source: outputs exactly {@link #targetOutput} RF/t, materialized from
 * nothing every tick — no real storage, no depletion, no insertion. Set via the same GUI a real
 * station's upper half uses ({@code StationScreen} in "creative" mode, which hides the
 * meaningless stored-energy line in favour of an "unlimited" label).
 */
public class CreativeStationBlockEntity extends BlockEntity implements EnergyScreenSource, WandPowerSource {
	private static final long DEFAULT_TARGET_OUTPUT = 1000L;

	private long targetOutput = DEFAULT_TARGET_OUTPUT;

	private final EnergyStorage storage = new FixedRateEnergyStorage(() -> this.targetOutput);

	public CreativeStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CREATIVE_STATION, pos, state);
	}

	public EnergyStorage getEnergyStorage() {
		return this.storage;
	}

	@Override
	public long getStoredEnergyForDisplay() {
		return this.targetOutput;
	}

	@Override
	public void setTargetOutput(long value) {
		this.targetOutput = Math.max(0, value);
		this.markDirty();
	}

	@Override
	public long drawEnergyForWand(long maxAmount) {
		try (Transaction tx = Transaction.openOuter()) {
			long extracted = this.storage.extract(maxAmount, tx);
			tx.commit();
			return extracted;
		}
	}

	/** Opens the station GUI in "creative" mode (hides the stored-energy line). */
	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player,
				new OpenStationScreenPayload(this.pos, null, true, this.targetOutput, this.targetOutput, new long[0]));
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLong("TargetOutput", this.targetOutput);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.targetOutput = nbt.contains("TargetOutput") ? nbt.getLong("TargetOutput") : DEFAULT_TARGET_OUTPUT;
	}
}
