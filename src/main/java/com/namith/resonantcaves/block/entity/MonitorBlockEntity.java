package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.block.EnergyTier;
import com.namith.resonantcaves.block.MonitorBlock;
import com.namith.resonantcaves.block.MonitorFlow;
import com.namith.resonantcaves.block.MonitorStatus;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenMonitorScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Mounted flush against one face of a cable (see {@code MonitorBlock}'s {@code FACING}) —
 * {@link #getAttachedCable} reads that exact neighbor, no scanning. Graphs the cable's throughput
 * and shows a rotating status/direction arrow. Right-clicking opens a GUI with a line graph of
 * the same history.
 */
public class MonitorBlockEntity extends BlockEntity {
	private static final int TICK_INTERVAL = 20;
	/** 3 in-game days (24,000 ticks each) sampled once per second (every 20 ticks). */
	private static final int HISTORY_LENGTH = 3600;

	private int tickCounter;
	private final long[] history = new long[HISTORY_LENGTH];
	private int historyWriteIndex;

	public MonitorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MONITOR, pos, state);
	}

	@Nullable
	private CableBlockEntity getAttachedCable() {
		if (this.world == null) {
			return null;
		}
		BlockPos attachedPos = this.pos.offset(this.getCachedState().get(MonitorBlock.FACING).getOpposite());
		return this.world.getBlockEntity(attachedPos) instanceof CableBlockEntity cable ? cable : null;
	}

	public static void tick(World world, BlockPos pos, BlockState state, MonitorBlockEntity entity) {
		if (world.isClient) {
			return;
		}
		entity.tickCounter++;
		if (entity.tickCounter < TICK_INTERVAL) {
			return;
		}
		entity.tickCounter = 0;

		MonitorStatus status;
		MonitorFlow flow;
		long sample;

		CableBlockEntity cable = entity.getAttachedCable();
		if (cable != null) {
			sample = cable.getLastFlowAmount();
			status = computeStatus(cable.getTier(), sample);
			flow = status == MonitorStatus.IDLE ? MonitorFlow.NONE
					: computeFlowRotation(cable.getLastFlowDirection(), state.get(MonitorBlock.FACING));
		} else {
			sample = 0;
			status = MonitorStatus.IDLE;
			flow = MonitorFlow.NONE;
		}

		entity.history[entity.historyWriteIndex] = sample;
		entity.historyWriteIndex = (entity.historyWriteIndex + 1) % HISTORY_LENGTH;
		entity.markDirty();

		BlockState newState = state.with(MonitorBlock.STATUS, status).with(MonitorBlock.FLOW, flow);
		if (newState != state) {
			world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
		}
	}

	/** Sends the GUI-open payload for this monitor's graph, seeded with its full history. */
	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		CableBlockEntity cable = this.getAttachedCable();
		EnergyTier tier = cable != null ? cable.getTier() : EnergyTier.IRON;
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player, new OpenMonitorScreenPayload(
				this.pos, tier, this.getHistorySnapshot(), this.getLatestFlowDirection()));
	}

	/** The most recently written sample, used for the once-per-second incremental update. */
	public long getLatestSample() {
		int lastIndex = (this.historyWriteIndex - 1 + HISTORY_LENGTH) % HISTORY_LENGTH;
		return this.history[lastIndex];
	}

	/**
	 * The attached cable's most recent direct-edge transfer direction (the neighbor it last pulled
	 * from/pushed to), or {@code null} if it's a relay cable with no direct source/sink of its own.
	 */
	@Nullable
	public Direction getLatestFlowDirection() {
		CableBlockEntity cable = this.getAttachedCable();
		return cable != null ? cable.getLastFlowDirection() : null;
	}

	/** The full history buffer in chronological order (oldest first), for the initial GUI-open payload. */
	public long[] getHistorySnapshot() {
		long[] snapshot = new long[HISTORY_LENGTH];
		for (int i = 0; i < HISTORY_LENGTH; i++) {
			snapshot[i] = this.history[(this.historyWriteIndex + i) % HISTORY_LENGTH];
		}
		return snapshot;
	}

	/**
	 * Red below this tier's own comfortable minimum (never true for {@code IRON}, whose minimum is
	 * 0); blue at or above the next tier up's minimum (never true for {@code GOLD}, which has no
	 * next tier) — i.e. blue means "you're pushing this tier as hard as the next tier up is rated
	 * for, an upgrade is worth it." Uses the current sample directly, not a historical average.
	 */
	private static MonitorStatus computeStatus(EnergyTier tier, long flow) {
		if (flow <= 0) {
			return MonitorStatus.IDLE;
		}
		if (flow < tier.minThroughput) {
			return MonitorStatus.UNDERUTILIZED;
		}
		EnergyTier next = tier.nextTier();
		if (next != null && flow >= next.minThroughput) {
			return MonitorStatus.OVERLOADED;
		}
		return MonitorStatus.OPTIMAL;
	}

	/**
	 * Projects the cable's real 3D flow direction onto the monitor's own mounting plane, so the
	 * arrow texture can be rotated to actually point that way as seen by someone looking at the
	 * monitor. {@code monitorFacing} is the outward normal of that plane. For a wall mount
	 * (horizontal {@code monitorFacing}), "right" as seen by the viewer is
	 * {@code monitorFacing.rotateYCounterclockwise()} and "up" is always world-up — confirmed by a
	 * playtest after the initial (clockwise) guess rendered backwards. For a floor/ceiling mount
	 * (vertical {@code monitorFacing}, attached to the top or underside of a cable), the plane is
	 * horizontal instead, so the 4 cardinal directions become the screen directions; a playtest
	 * pinned down the exact convention by testing an east-west and a north-south cable separately
	 * on a bottom-mounted monitor: {@code EAST}=right on screen for *both* top- and bottom-mounted
	 * (no left/right mirroring — an earlier guess that mirrored this was wrong), but {@code
	 * NORTH}/{@code SOUTH} *do* flip between the two ({@code NORTH}=up/{@code SOUTH}=down for
	 * top-mounted, reversed for bottom-mounted) — physically sensible, since tilting your head from
	 * looking down to looking up flips your sense of "up the screen" without rotating your body
	 * (and thus without flipping left/right). A direction with no component in the mounting plane
	 * (straight up/down past a floor/ceiling mount, or directly behind/in front of a wall mount)
	 * falls back to {@code UP}, as does an unknown ({@code null}) direction from a relay cable with
	 * no source/sink of its own.
	 */
	private static MonitorFlow computeFlowRotation(@Nullable Direction flowDirection, Direction monitorFacing) {
		if (flowDirection == null) {
			return MonitorFlow.UP;
		}
		if (monitorFacing.getAxis() == Direction.Axis.Y) {
			// Mounted flat on top of a cable (FACING=UP, viewed from above) or its underside
			// (FACING=DOWN, viewed from below): the mounting plane is horizontal, so the 4 cardinal
			// directions become the 2D screen directions instead of world up/down.
			if (flowDirection.getAxis() == Direction.Axis.Y) {
				return MonitorFlow.UP;
			}
			boolean topMounted = monitorFacing == Direction.UP;
			if (flowDirection == Direction.NORTH) {
				return topMounted ? MonitorFlow.UP : MonitorFlow.DOWN;
			}
			if (flowDirection == Direction.SOUTH) {
				return topMounted ? MonitorFlow.DOWN : MonitorFlow.UP;
			}
			return flowDirection == Direction.EAST ? MonitorFlow.RIGHT : MonitorFlow.LEFT;
		}
		if (flowDirection == Direction.UP) {
			return MonitorFlow.UP;
		}
		if (flowDirection == Direction.DOWN) {
			return MonitorFlow.DOWN;
		}
		Direction right = monitorFacing.rotateYCounterclockwise();
		if (flowDirection == right) {
			return MonitorFlow.RIGHT;
		}
		if (flowDirection == right.getOpposite()) {
			return MonitorFlow.LEFT;
		}
		return MonitorFlow.UP;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLongArray("History", this.history);
		nbt.putInt("HistoryWriteIndex", this.historyWriteIndex);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		long[] saved = nbt.getLongArray("History");
		if (saved.length == HISTORY_LENGTH) {
			System.arraycopy(saved, 0, this.history, 0, HISTORY_LENGTH);
		}
		this.historyWriteIndex = nbt.contains("HistoryWriteIndex") ? nbt.getInt("HistoryWriteIndex") % HISTORY_LENGTH : 0;
	}
}
