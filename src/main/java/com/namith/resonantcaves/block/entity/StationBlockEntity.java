package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.block.EnergyTier;
import com.namith.resonantcaves.block.StationBlock;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenStationScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.LimitingEnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * A single-block station: one shared {@link #realStorage} pool, with only the {@code FACING}
 * side allowed to extract (throttled by {@link #targetOutput}) and every other side allowed to
 * insert. No capacity cap, no max-output cap; the only loss is the continuous per-tier leak
 * applied on every tick. Also keeps its own stored-energy history (sampled once per second,
 * same cadence as the leak), graphed directly in {@code StationScreen} — the Energy Monitor block
 * graphs cable throughput only, not station storage; see {@code MonitorBlockEntity}.
 */
public class StationBlockEntity extends BlockEntity implements EnergyScreenSource {
	private static final int LEAK_TICK_INTERVAL = 20;

	private final EnergyTier tier;
	private final SimpleEnergyStorage realStorage = new SimpleEnergyStorage(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE) {
		@Override
		protected void onFinalCommit() {
			StationBlockEntity.this.markDirty();
		}
	};
	private static final long DEFAULT_TARGET_OUTPUT = 1000L;

	private long targetOutput = DEFAULT_TARGET_OUTPUT;
	private int leakTickCounter;
	private final SampleHistory history = new SampleHistory();

	public StationBlockEntity(BlockPos pos, BlockState state, EnergyTier tier) {
		super(ModBlockEntities.STATION, pos, state);
		this.tier = tier;
	}

	public EnergyTier getTier() {
		return this.tier;
	}

	@Override
	public long getStoredEnergyForDisplay() {
		return this.realStorage.amount;
	}

	/** The full history buffer in chronological order (oldest first), for the initial GUI-open payload. */
	public long[] getHistorySnapshot() {
		return this.history.snapshot();
	}

	public long getTargetOutput() {
		return this.targetOutput;
	}

	/** The output ({@code FACING}) side is extract-only, throttled by {@link #targetOutput}; every other side is insert-only. */
	public EnergyStorage getEnergyStorage(Direction side) {
		Direction facing = this.getCachedState().get(StationBlock.FACING);
		if (side == facing) {
			return new LimitingEnergyStorage(this.realStorage, 0, this.targetOutput);
		}
		return new LimitingEnergyStorage(this.realStorage, Long.MAX_VALUE, 0);
	}

	@Override
	public void setTargetOutput(long value) {
		this.targetOutput = Math.max(0, value);
		this.markDirty();
	}

	/** Opens this station's GUI (live stored-energy readout, history graph, and precise output setter) for the player. */
	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player, new OpenStationScreenPayload(
				this.pos, this.tier, false, this.getStoredEnergyForDisplay(), this.targetOutput, this.getHistorySnapshot()));
	}

	public static void tick(World world, BlockPos pos, BlockState state, StationBlockEntity entity) {
		if (world.isClient) {
			return;
		}
		entity.leakTickCounter++;
		if (entity.leakTickCounter < LEAK_TICK_INTERVAL) {
			return;
		}
		entity.leakTickCounter = 0;

		long amount = entity.realStorage.amount;
		if (amount > 0) {
			long leaked = (long) Math.ceil(amount * entity.tier.stationLeakRatePerSecond);
			entity.realStorage.amount = Math.max(0, amount - leaked);
		}

		entity.history.append(entity.realStorage.amount);
		entity.markDirty();
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLong("StoredEnergy", this.realStorage.amount);
		nbt.putLong("TargetOutput", this.targetOutput);
		this.history.writeNbt(nbt, "History");
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.realStorage.amount = nbt.getLong("StoredEnergy");
		this.targetOutput = nbt.contains("TargetOutput") ? nbt.getLong("TargetOutput") : DEFAULT_TARGET_OUTPUT;
		this.history.readNbt(nbt, "History");
	}
}
