package com.namith.resonantcaves.goal;

import java.util.EnumSet;

import com.namith.resonantcaves.item.ModItems;
import com.namith.resonantcaves.item.WandAttacks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.AbstractSkeletonEntity;

/**
 * A wand-holding skeleton's ranged attack — preempts vanilla's melee/bow goal slot (both added at
 * priority 4 by {@code AbstractSkeletonEntity.updateAttackType()}; the wand fails its
 * {@code isOf(Items.BOW)} check, so vanilla already routes a wand-holding skeleton to melee on its
 * own, and this goal's higher priority is what actually preempts that). Modeled on vanilla's
 * {@code BowAttackGoal} shape, simplified since the attack is instant (no arrow pull-charge state).
 */
public class WandAttackGoal extends Goal {
	private static final int ATTACK_COOLDOWN_TICKS = 40;

	private final AbstractSkeletonEntity skeleton;
	private int cooldown;

	public WandAttackGoal(AbstractSkeletonEntity skeleton) {
		this.skeleton = skeleton;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.skeleton.getTarget();
		return target != null && target.isAlive() && this.skeleton.getMainHandStack().isOf(ModItems.RESONANT_WAND);
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
	}

	@Override
	public void stop() {
		this.skeleton.getNavigation().stop();
		this.cooldown = 0;
	}

	@Override
	public void tick() {
		LivingEntity target = this.skeleton.getTarget();
		this.skeleton.getLookControl().lookAt(target, 30.0F, 30.0F);
		this.skeleton.getNavigation().startMovingTo(target, 1.0);

		if (--this.cooldown <= 0 && this.skeleton.getVisibilityCache().canSee(target)) {
			WandAttacks.fire(this.skeleton, true, WandAttacks.SKELETON_STATION_SEARCH_RADIUS, false);
			this.cooldown = ATTACK_COOLDOWN_TICKS;
		}
	}
}
