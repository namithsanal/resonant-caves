package com.namith.resonantcaves;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Feature 3 — Resonant Ore.
 *
 * <p>Adds the {@code resonant_ore} placed feature (defined as datapack-style JSON under
 * {@code data/resonantcaves/worldgen}) to every overworld biome, at roughly iron ore's rarity.
 * The placement uses relative height anchors ({@code above_bottom}/{@code below_top}), so it
 * keeps generating throughout the underground if Feature 4 later changes the world's height range.
 */
public final class ResonantOreWorldGen {
	private ResonantOreWorldGen() {
	}

	public static void register() {
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Feature.UNDERGROUND_ORES,
				RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(ResonantCaves.MOD_ID, "resonant_ore")));
	}
}
