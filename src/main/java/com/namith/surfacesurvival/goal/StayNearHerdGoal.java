package com.namith.surfacesurvival.goal;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Feature 2 — large fleeing herds.
 *
 * <p>A lightweight "stay together" cue: when an animal has wandered more than
 * {@code REGROUP_DISTANCE} blocks from the nearest animal of its own type, it walks back toward
 * that neighbor until it's within {@code STOP_DISTANCE} blocks. This is not flocking — each
 * animal only ever reacts to its single nearest same-type neighbor — but it counteracts the
 * drift from independent wandering and post-flee scattering, so large spawn groups keep reading
 * as herds over time instead of dispersing.
 */
public class StayNearHerdGoal extends Goal {
	private static final double SEARCH_RADIUS = 32.0;
	private static final double SEARCH_HEIGHT = 10.0;
	private static final double REGROUP_DISTANCE_SQUARED = 16.0 * 16.0;
	private static final double STOP_DISTANCE_SQUARED = 6.0 * 6.0;

	private final AnimalEntity mob;
	private final double speed;
	@Nullable
	private AnimalEntity neighbor;
	private int delay;

	public StayNearHerdGoal(AnimalEntity mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		this.setControls(EnumSet.of(Control.MOVE));
	}

	@Override
	public boolean canStart() {
		AnimalEntity closest = null;
		double closestDistance = Double.MAX_VALUE;

		for (AnimalEntity other : this.mob.getWorld()
				.getNonSpectatingEntities(this.mob.getClass(), this.mob.getBoundingBox().expand(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS))) {
			if (other == this.mob) {
				continue;
			}

			double distance = this.mob.squaredDistanceTo(other);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = other;
			}
		}

		if (closest == null || closestDistance < REGROUP_DISTANCE_SQUARED) {
			return false;
		}

		this.neighbor = closest;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.neighbor != null && this.neighbor.isAlive() && this.mob.squaredDistanceTo(this.neighbor) > STOP_DISTANCE_SQUARED;
	}

	@Override
	public void start() {
		this.delay = 0;
	}

	@Override
	public void stop() {
		this.neighbor = null;
	}

	@Override
	public void tick() {
		if (--this.delay <= 0) {
			this.delay = this.getTickCount(10);
			if (this.neighbor != null) {
				this.mob.getNavigation().startMovingTo(this.neighbor, this.speed);
			}
		}
	}
}
