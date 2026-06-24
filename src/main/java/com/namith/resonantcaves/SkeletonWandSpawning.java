package com.namith.resonantcaves;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

/**
 * Plain skeletons sometimes spawn holding the Resonant Wand instead of their usual bow. Mirrors
 * {@link ZombieHelmetSpawning}'s exact pattern: a one-time roll gated by a command tag, and a
 * guaranteed (chance {@code > 1.0F}) drop on death.
 */
public final class SkeletonWandSpawning {
	private static final String ROLLED_TAG = "resonantcaves_skeleton_wand_roll";
	private static final float SPAWN_CHANCE = 0.05F;

	private SkeletonWandSpawning() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof SkeletonEntity skeleton) || skeleton.getClass() != SkeletonEntity.class
					|| !skeleton.addCommandTag(ROLLED_TAG)) {
				return;
			}

			Random random = world.getRandom();
			if (random.nextFloat() < SPAWN_CHANCE) {
				skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.RESONANT_WAND));
				skeleton.setEquipmentDropChance(EquipmentSlot.MAINHAND, 2.0F);
			}
		});
	}
}
