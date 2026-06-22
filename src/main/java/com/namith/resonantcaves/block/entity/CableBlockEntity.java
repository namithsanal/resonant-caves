package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.block.EnergyTier;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * A cable, holding no energy itself — it's a pure conduit. Per tick, {@link CableNetworkTicker}
 * gathers every same-tier cable directly connected to this one into a network and moves energy
 * from adjacent sources to adjacent sinks through it (adapted from Tech Reborn's
 * {@code CableBlockEntity}/{@code CableTickManager}). {@code lastFlowDirection}/{@code
 * lastFlowAmount} record this cable's own most recent direct transfer, read by adjacent monitor
 * blocks.
 */
public class CableBlockEntity extends BlockEntity {
	private final EnergyTier tier;
	private long networkTickStamp = -1;
	private Direction lastFlowDirection;
	private long lastFlowAmount;

	public CableBlockEntity(BlockPos pos, BlockState state, EnergyTier tier) {
		super(ModBlockEntities.CABLE, pos, state);
		this.tier = tier;
	}

	public EnergyTier getTier() {
		return this.tier;
	}

	long getNetworkTickStamp() {
		return this.networkTickStamp;
	}

	void setNetworkTickStamp(long stamp) {
		this.networkTickStamp = stamp;
	}

	public Direction getLastFlowDirection() {
		return this.lastFlowDirection;
	}

	public long getLastFlowAmount() {
		return this.lastFlowAmount;
	}

	/**
	 * Broadcast to every cable in the network each tick (not just the ones directly touching a
	 * source/sink) — both the amount and the direction. This mod's pooled-transfer model only ever
	 * computes one transfer per network per tick, so there's exactly one meaningful flow
	 * direction (the sink's) for the whole network, not a distinct one per cable; broadcasting it
	 * uniformly is what makes every monitor along the same stretch agree on which way the arrow
	 * points, instead of edge cables showing their own local connection's direction while relay
	 * cables fall back to a default.
	 */
	void setNetworkFlow(long amount, Direction direction) {
		this.lastFlowAmount = amount;
		this.lastFlowDirection = direction;
	}

	public static void tick(World world, BlockPos pos, BlockState state, CableBlockEntity entity) {
		if (world.isClient) {
			return;
		}
		CableNetworkTicker.handleCableTick(entity);
	}
}
