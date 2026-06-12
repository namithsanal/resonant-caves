package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.goal.StayNearHerdGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 2 — large fleeing herds. Sheep, cows, pigs, and chickens sprint away when a player
 * comes within ~32 blocks, and drift back toward their nearest same-type neighbor when the herd
 * scatters (see {@link StayNearHerdGoal}). Horses spawn in large herds (see HerdSpawning) but are
 * exempt from both goals, so they remain tameable as usual.
 */
@Mixin({SheepEntity.class, CowEntity.class, PigEntity.class, ChickenEntity.class})
public abstract class FleeingAnimalsMixin extends AnimalEntity {
	private FleeingAnimalsMixin(EntityType<? extends AnimalEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"))
	private void resonantcaves$addFleeFromPlayerGoal(CallbackInfo ci) {
		this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 32.0F, 1.5, 2.0));
		this.goalSelector.add(2, new StayNearHerdGoal(this, 1.0));
	}
}
