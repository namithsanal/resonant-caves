package com.namith.resonantcaves.goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.namith.resonantcaves.item.ModItems;
import com.namith.resonantcaves.item.WandAttacks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.util.math.Box;

/**
 * A wand-equipped Snow Golem's ranged attack: seeks the nearest hostile mob within
 * {@link #SEARCH_RADIUS} blocks and hitscan-attacks it, but only while a Station is within range
 * and visible (see {@link WandAttacks#fire}, called with no fallback). Deliberately an
 * independent entity scan rather than vanilla's {@code targetSelector}/follow-range (which
 * defaults to 16 blocks for every snow golem) — reusing that would silently extend every
 * non-wand golem's targeting range too. Modeled on {@link SeekAndDetonateGoal}'s
 * search/cooldown/pursue/act shape. Priority 0 in {@code initGoals}, strictly ahead of vanilla's
 * {@code ProjectileAttackGoal} (priority 1, which doesn't check held item at all), so this goal
 * always wins over snowball-throwing whenever it can start.
 */
public class GolemWandSeekGoal extends Goal {
	private static final double SEARCH_RADIUS = 64.0;
	private static final int RESCAN_COOLDOWN = 40;
	private static final int PATH_RECALC_INTERVAL = 20;
	private static final int ATTACK_COOLDOWN_TICKS = 40;
	private static final double ATTACK_RANGE_SQUARED = SEARCH_RADIUS * SEARCH_RADIUS;

	private final SnowGolemEntity golem;
	private LivingEntity target;
	private int rescanCooldown;
	private int pathRecalcCooldown;
	private int attackCooldown;

	public GolemWandSeekGoal(SnowGolemEntity golem) {
		this.golem = golem;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!this.golem.getMainHandStack().isOf(ModItems.RESONANT_WAND)) {
			return false;
		}
		if (this.rescanCooldown > 0) {
			this.rescanCooldown--;
			return false;
		}
		this.target = findNearestHostile();
		if (this.target == null) {
			this.rescanCooldown = RESCAN_COOLDOWN;
			return false;
		}
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.golem.getMainHandStack().isOf(ModItems.RESONANT_WAND)
				&& this.target != null && this.target.isAlive();
	}

	@Override
	public void start() {
		this.pathRecalcCooldown = 0;
		this.attackCooldown = 0;
	}

	@Override
	public void stop() {
		this.target = null;
		this.golem.getNavigation().stop();
		this.rescanCooldown = RESCAN_COOLDOWN;
	}

	@Override
	public void tick() {
		this.golem.getLookControl().lookAt(this.target, 30.0F, 30.0F);

		if (--this.pathRecalcCooldown <= 0) {
			this.pathRecalcCooldown = PATH_RECALC_INTERVAL;
			this.golem.getNavigation().startMovingTo(this.target, 1.0);
		}

		if (this.golem.squaredDistanceTo(this.target) <= ATTACK_RANGE_SQUARED
				&& --this.attackCooldown <= 0
				&& this.golem.getVisibilityCache().canSee(this.target)) {
			WandAttacks.AttackResult result = WandAttacks.fire(this.golem, false, WandAttacks.EXTENDED_STATION_SEARCH_RADIUS, true);
			if (result.fired()) {
				this.attackCooldown = ATTACK_COOLDOWN_TICKS;
			}
		}
	}

	private LivingEntity findNearestHostile() {
		Box box = this.golem.getBoundingBox().expand(SEARCH_RADIUS);
		List<LivingEntity> hostiles = this.golem.getWorld().getEntitiesByClass(LivingEntity.class, box,
				entity -> entity instanceof Monster && entity.isAlive());
		return hostiles.stream().min(Comparator.comparingDouble(this.golem::squaredDistanceTo)).orElse(null);
	}
}
