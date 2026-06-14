package com.namith.resonantcaves;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Feature 2 — Large fleeing herds.
 *
 * <p>Raises the natural spawn group sizes for sheep, cows, pigs, chickens, and horses, turning
 * their existing biome spawns into large herds. The actual "flee from players" behavior for
 * sheep/cows/pigs/chickens is added by {@code FleeingAnimalsMixin}; horses are exempt and remain
 * tame to approach despite spawning in large groups.
 */
public final class HerdSpawning {
	private HerdSpawning() {
	}

	public static void register() {
		growHerd("sheep_herds", EntityType.SHEEP, 15, 25);
		growHerd("cow_herds", EntityType.COW, 10, 20);
		growHerd("pig_herds", EntityType.PIG, 8, 16);
		growHerd("chicken_herds", EntityType.CHICKEN, 20, 40);
		growHerd("horse_herds", EntityType.HORSE, 6, 12);
	}

	/**
	 * Rewrites the min/max group size of every existing spawn entry for {@code entityType}, keeping
	 * each biome's original spawn group and weight so spawn frequency and biome placement are
	 * unchanged — only the group size grows.
	 */
	private static void growHerd(String name, EntityType<?> entityType, int minGroupSize, int maxGroupSize) {
		BiomeModifications.create(Identifier.of(ResonantCaves.MOD_ID, name))
				.add(ModificationPhase.REPLACEMENTS, BiomeSelectors.foundInOverworld(), context -> {
					var spawnSettings = context.getSpawnSettings();
					List<SpawnGroup> groups = new ArrayList<>();
					List<SpawnSettings.SpawnEntry> entries = new ArrayList<>();

					spawnSettings.removeSpawns((group, entry) -> {
						if (entry.type == entityType) {
							groups.add(group);
							entries.add(entry);
							return true;
						}
						return false;
					});

					for (int i = 0; i < entries.size(); i++) {
						SpawnSettings.SpawnEntry entry = entries.get(i);
						spawnSettings.addSpawn(groups.get(i),
								new SpawnSettings.SpawnEntry(entry.type, entry.getWeight().getValue(), minGroupSize, maxGroupSize));
					}
				});
	}
}
