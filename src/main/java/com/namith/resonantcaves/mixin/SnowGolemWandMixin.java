package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.goal.GolemWandSeekGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resonant Wand — injects {@link GolemWandSeekGoal} into every snow golem's goal selector, at a
 * priority that preempts vanilla's snowball-throwing goal whenever the golem is holding the wand.
 */
@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemWandMixin extends GolemEntity {
	protected SnowGolemWandMixin(EntityType<? extends GolemEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"))
	private void resonantcaves$addWandSeekGoal(CallbackInfo ci) {
		this.goalSelector.add(0, new GolemWandSeekGoal((SnowGolemEntity) (Object) this));
	}
}
