package com.namith.resonantcaves;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.random.Random;

/**
 * Feature 6 — Creeper pack spawning.
 *
 * <p>Vanilla's group-size spawn mechanic ({@code SpawnSettings.SpawnEntry}'s min/max group size)
 * places each pack member via its own ±5-block random walk from the original roll position, which
 * mostly fails {@code canSpawn} in this mod's narrow, unbreakable-walled cave tunnels — so raising
 * the creeper group size there produced few, widely-scattered extra creepers and no visible packs.
 *
 * <p>Instead, every naturally-spawned creeper becomes the "leader" of a tight pack: on
 * {@code ServerEntityEvents.ENTITY_LOAD}, 9-14 more creepers are spawned within {@link #SPREAD}
 * blocks of it, each placed only where {@code World.isSpaceEmpty} confirms there's room. All pack
 * members (including the leader) are tagged with {@link #PACK_TAG} so chunk reloads of existing
 * creepers don't repeatedly spawn new packs around them.
 */
public final class CreeperPackSpawning {
	private static final String PACK_TAG = "resonantcaves_creeper_pack";
	private static final int MIN_PACK_SIZE = 10;
	private static final int MAX_PACK_SIZE = 15;
	private static final double SPREAD = 3.0;
	private static final int PLACEMENT_ATTEMPTS = 6;

	private CreeperPackSpawning() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof CreeperEntity leader) || !leader.addCommandTag(PACK_TAG)) {
				return;
			}

			Random random = world.getRandom();
			int packSize = MIN_PACK_SIZE + random.nextInt(MAX_PACK_SIZE - MIN_PACK_SIZE + 1);

			for (int i = 1; i < packSize; i++) {
				CreeperEntity member = EntityType.CREEPER.create(world);
				if (member == null) {
					continue;
				}

				for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
					double x = leader.getX() + (random.nextDouble() * 2 - 1) * SPREAD;
					double z = leader.getZ() + (random.nextDouble() * 2 - 1) * SPREAD;
					member.refreshPositionAndAngles(x, leader.getY(), z, random.nextFloat() * 360.0F, 0.0F);
					if (world.isSpaceEmpty(member, member.getBoundingBox())) {
						member.addCommandTag(PACK_TAG);
						member.initialize(world, world.getLocalDifficulty(member.getBlockPos()), SpawnReason.NATURAL, null);
						world.spawnEntity(member);
						break;
					}
				}
			}
		});
	}
}
