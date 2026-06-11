package com.namith.surfacesurvival.goal;

import java.util.EnumSet;
import java.util.Optional;

import com.namith.surfacesurvival.SurfaceSurvival;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Feature 5 — creepers seek out and detonate near tech-mod blocks.
 *
 * <p>Scans for the nearest block in {@link #CREEPER_ATTRACTING_BLOCKS}, paths to it with vanilla
 * navigation, and calls the creeper's normal {@code ignite()} once close enough — the existing
 * fuse/explosion code does the rest, so this is an ordinary power-3 creeper explosion.
 */
public class SeekAndDetonateGoal extends Goal {
	public static final TagKey<Block> CREEPER_ATTRACTING_BLOCKS =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of(SurfaceSurvival.MOD_ID, "creeper_attracting_blocks"));

	private static final int SEARCH_RADIUS_HORIZONTAL = 24;
	private static final int SEARCH_RADIUS_VERTICAL = 6;
	private static final int RESCAN_COOLDOWN = 100;
	private static final int PATH_RECALC_INTERVAL = 20;
	private static final int MAX_PURSUIT_TICKS = 600;
	private static final double IGNITE_DISTANCE_SQUARED = 2.25;

	private final CreeperEntity creeper;
	private final double speed;
	private BlockPos targetPos;
	private int rescanCooldown;
	private int pathRecalcCooldown;
	private int pursuitTicks;

	public SeekAndDetonateGoal(CreeperEntity creeper, double speed) {
		this.creeper = creeper;
		this.speed = speed;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.creeper.isIgnited()) {
			return false;
		}
		if (this.rescanCooldown > 0) {
			this.rescanCooldown--;
			return false;
		}
		Optional<BlockPos> found = findNearbyTarget();
		if (found.isEmpty()) {
			this.rescanCooldown = RESCAN_COOLDOWN;
			return false;
		}
		this.targetPos = found.get();
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (this.creeper.isIgnited() || this.targetPos == null || this.pursuitTicks > MAX_PURSUIT_TICKS) {
			return false;
		}
		return this.creeper.getWorld().getBlockState(this.targetPos).isIn(CREEPER_ATTRACTING_BLOCKS);
	}

	@Override
	public void start() {
		this.pathRecalcCooldown = 0;
		this.pursuitTicks = 0;
	}

	@Override
	public void stop() {
		this.targetPos = null;
		this.creeper.getNavigation().stop();
		this.rescanCooldown = RESCAN_COOLDOWN;
	}

	@Override
	public void tick() {
		this.pursuitTicks++;
		double targetX = this.targetPos.getX() + 0.5;
		double targetY = this.targetPos.getY() + 0.5;
		double targetZ = this.targetPos.getZ() + 0.5;

		this.creeper.getLookControl().lookAt(targetX, targetY, targetZ);

		if (--this.pathRecalcCooldown <= 0) {
			this.pathRecalcCooldown = PATH_RECALC_INTERVAL;
			this.creeper.getNavigation().startMovingTo(targetX, targetY, targetZ, this.speed);
		}

		if (this.creeper.getBlockPos().getSquaredDistance(this.targetPos) <= IGNITE_DISTANCE_SQUARED) {
			this.creeper.ignite();
		}
	}

	private Optional<BlockPos> findNearbyTarget() {
		World world = this.creeper.getWorld();
		BlockPos origin = this.creeper.getBlockPos();
		return BlockPos.findClosest(origin, SEARCH_RADIUS_HORIZONTAL, SEARCH_RADIUS_VERTICAL,
				pos -> world.getBlockState(pos).isIn(CREEPER_ATTRACTING_BLOCKS));
	}
}
