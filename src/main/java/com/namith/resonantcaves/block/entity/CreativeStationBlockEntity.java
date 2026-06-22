package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.block.EnergyTier;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenStationScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
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
public class CreativeStationBlockEntity extends BlockEntity implements EnergyScreenSource {
	private static final long DEFAULT_TARGET_OUTPUT = 1000L;

	private long targetOutput = DEFAULT_TARGET_OUTPUT;

	private final EnergyStorage storage = new EnergyStorage() {
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
			return Math.min(maxAmount, CreativeStationBlockEntity.this.targetOutput);
		}

		@Override
		public long getAmount() {
			return CreativeStationBlockEntity.this.targetOutput;
		}

		@Override
		public long getCapacity() {
			return CreativeStationBlockEntity.this.targetOutput;
		}
	};

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

	/** Opens the station GUI in "creative" mode (hides the stored-energy line). */
	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player,
				new OpenStationScreenPayload(this.pos, EnergyTier.GOLD, true, this.targetOutput, this.targetOutput, new long[0]));
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
