package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.goal.SeekAndDetonateGoal;
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
 * pathfinds toward and detonates near blocks tagged {@code resonantcaves:creeper_attracting_blocks}.
 *
 * <p>Also raises the per-spawn-attempt creeper cap (Feature 6): {@link net.minecraft.entity.mob.MobEntity#getLimitPerChunk()}
 * defaults to 4 and is unrelated to {@code SpawnSettings.SpawnEntry}'s min/max group size — without this
 * override, {@code SpawnHelper.spawnEntitiesInChunk} stops after 4 creepers regardless of the 10-15
 * group size configured in {@code HerdSpawning}, so packs never exceeded vanilla's usual size.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperTargetingMixin extends HostileEntity {
	private CreeperTargetingMixin(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"))
	private void resonantcaves$addSeekAndDetonateGoal(CallbackInfo ci) {
		this.goalSelector.add(4, new SeekAndDetonateGoal((CreeperEntity) (Object) this, 1.0));
	}

	@Override
	public int getLimitPerChunk() {
		return 15;
	}
}
