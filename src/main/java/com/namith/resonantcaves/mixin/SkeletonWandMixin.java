package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.goal.WandAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resonant Wand — injects {@link WandAttackGoal} into every skeleton's goal selector, at a
 * priority that preempts vanilla's melee/bow goal whenever the skeleton is holding the wand.
 */
@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonWandMixin extends HostileEntity {
	private SkeletonWandMixin(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"))
	private void resonantcaves$addWandAttackGoal(CallbackInfo ci) {
		this.goalSelector.add(3, new WandAttackGoal((AbstractSkeletonEntity) (Object) this));
	}
}
