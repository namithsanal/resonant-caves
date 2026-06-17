package com.namith.resonantcaves;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

/**
 * Feature 7 follow-up — the Resonant Helmet is no longer craftable; instead, plain zombies have a
 * small chance to spawn already wearing one. The equipment drop chance is set above 1.0, mirroring
 * vanilla's pillager-captain banner ({@code PatrolEntity}), which makes {@code MobEntity.dropEquipment}
 * drop it unconditionally on death regardless of what killed the zombie.
 */
public final class ZombieHelmetSpawning {
	private static final String ROLLED_TAG = "resonantcaves_zombie_helmet_roll";
	private static final float SPAWN_CHANCE = 0.05F;

	private ZombieHelmetSpawning() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof ZombieEntity zombie) || zombie.getClass() != ZombieEntity.class
					|| !zombie.addCommandTag(ROLLED_TAG)) {
				return;
			}

			Random random = world.getRandom();
			if (random.nextFloat() < SPAWN_CHANCE) {
				zombie.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.RESONANT_HELMET));
				zombie.setEquipmentDropChance(EquipmentSlot.HEAD, 2.0F);
			}
		});
	}
}
