package com.namith.surfacesurvival.mixin;

import com.namith.surfacesurvival.goal.SeekAndDetonateGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 5 — injects {@link SeekAndDetonateGoal} into every creeper's goal selector so it
 * pathfinds toward and detonates near blocks tagged {@code surfacesurvival:creeper_attracting_blocks}.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperTargetingMixin extends HostileEntity {
	private CreeperTargetingMixin(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"))
	private void surfacesurvival$addSeekAndDetonateGoal(CallbackInfo ci) {
		this.goalSelector.add(4, new SeekAndDetonateGoal((CreeperEntity) (Object) this, 1.0));
	}
}
