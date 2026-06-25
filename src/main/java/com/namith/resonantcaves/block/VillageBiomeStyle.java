package com.namith.resonantcaves.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Vanilla's five village styles, each with its own real structure template catalog (verified
 * directly against the 1.21.1 client jar's {@code data/minecraft/structure/village/<style>/...}
 * contents). {@link #resolve} maps a biome to the closest style using exact matches where vanilla
 * has a dedicated village type, and commonsense aesthetic substitutions otherwise (e.g. badlands
 * biomes get the desert/sandstone look) rather than defaulting everything non-matching to plains.
 */
public enum VillageBiomeStyle {
	PLAINS(
			ids("village/plains/houses/plains_small_house_1",
					"village/plains/houses/plains_small_house_2",
					"village/plains/houses/plains_small_house_3",
					"village/plains/houses/plains_small_house_4",
					"village/plains/houses/plains_small_house_5",
					"village/plains/houses/plains_small_house_6",
					"village/plains/houses/plains_small_house_7",
					"village/plains/houses/plains_small_house_8",
					"village/plains/houses/plains_medium_house_1",
					"village/plains/houses/plains_medium_house_2",
					"village/plains/houses/plains_big_house_1",
					"village/plains/houses/plains_animal_pen_1",
					"village/plains/houses/plains_animal_pen_2",
					"village/plains/houses/plains_animal_pen_3",
					"village/plains/houses/plains_small_farm_1",
					"village/plains/houses/plains_large_farm_1",
					"village/plains/houses/plains_library_1",
					"village/plains/houses/plains_library_2",
					"village/plains/houses/plains_armorer_house_1",
					"village/plains/houses/plains_butcher_shop_1",
					"village/plains/houses/plains_butcher_shop_2",
					"village/plains/houses/plains_cartographer_1",
					"village/plains/houses/plains_fisher_cottage_1",
					"village/plains/houses/plains_fletcher_house_1",
					"village/plains/houses/plains_masons_house_1",
					"village/plains/houses/plains_shepherds_house_1",
					"village/plains/houses/plains_stable_1",
					"village/plains/houses/plains_stable_2",
					"village/plains/houses/plains_tannery_1",
					"village/plains/houses/plains_temple_3",
					"village/plains/houses/plains_temple_4",
					"village/plains/houses/plains_tool_smith_1",
					"village/plains/houses/plains_weaponsmith_1"),
			ids("village/plains/plains_lamp_1",
					"village/plains/town_centers/plains_fountain_01",
					"village/plains/town_centers/plains_meeting_point_1",
					"village/plains/town_centers/plains_meeting_point_2",
					"village/plains/town_centers/plains_meeting_point_3")),

	DESERT(
			ids("village/desert/houses/desert_animal_pen_1",
					"village/desert/houses/desert_animal_pen_2",
					"village/desert/houses/desert_armorer_1",
					"village/desert/houses/desert_butcher_shop_1",
					"village/desert/houses/desert_cartographer_house_1",
					"village/desert/houses/desert_farm_1",
					"village/desert/houses/desert_farm_2",
					"village/desert/houses/desert_fisher_1",
					"village/desert/houses/desert_fletcher_house_1",
					"village/desert/houses/desert_large_farm_1",
					"village/desert/houses/desert_library_1",
					"village/desert/houses/desert_mason_1",
					"village/desert/houses/desert_medium_house_1",
					"village/desert/houses/desert_medium_house_2",
					"village/desert/houses/desert_shepherd_house_1",
					"village/desert/houses/desert_small_house_1",
					"village/desert/houses/desert_small_house_2",
					"village/desert/houses/desert_small_house_3",
					"village/desert/houses/desert_small_house_4",
					"village/desert/houses/desert_small_house_5",
					"village/desert/houses/desert_small_house_6",
					"village/desert/houses/desert_small_house_7",
					"village/desert/houses/desert_small_house_8",
					"village/desert/houses/desert_tannery_1",
					"village/desert/houses/desert_temple_1",
					"village/desert/houses/desert_temple_2",
					"village/desert/houses/desert_tool_smith_1",
					"village/desert/houses/desert_weaponsmith_1"),
			ids("village/desert/desert_lamp_1",
					"village/desert/town_centers/desert_meeting_point_1",
					"village/desert/town_centers/desert_meeting_point_2",
					"village/desert/town_centers/desert_meeting_point_3")),

	SAVANNA(
			ids("village/savanna/houses/savanna_animal_pen_1",
					"village/savanna/houses/savanna_animal_pen_2",
					"village/savanna/houses/savanna_animal_pen_3",
					"village/savanna/houses/savanna_armorer_1",
					"village/savanna/houses/savanna_butchers_shop_1",
					"village/savanna/houses/savanna_butchers_shop_2",
					"village/savanna/houses/savanna_cartographer_1",
					"village/savanna/houses/savanna_fisher_cottage_1",
					"village/savanna/houses/savanna_fletcher_house_1",
					"village/savanna/houses/savanna_large_farm_1",
					"village/savanna/houses/savanna_large_farm_2",
					"village/savanna/houses/savanna_library_1",
					"village/savanna/houses/savanna_mason_1",
					"village/savanna/houses/savanna_medium_house_1",
					"village/savanna/houses/savanna_medium_house_2",
					"village/savanna/houses/savanna_shepherd_1",
					"village/savanna/houses/savanna_small_farm",
					"village/savanna/houses/savanna_small_house_1",
					"village/savanna/houses/savanna_small_house_2",
					"village/savanna/houses/savanna_small_house_3",
					"village/savanna/houses/savanna_small_house_4",
					"village/savanna/houses/savanna_small_house_5",
					"village/savanna/houses/savanna_small_house_6",
					"village/savanna/houses/savanna_small_house_7",
					"village/savanna/houses/savanna_small_house_8",
					"village/savanna/houses/savanna_tannery_1",
					"village/savanna/houses/savanna_temple_1",
					"village/savanna/houses/savanna_temple_2",
					"village/savanna/houses/savanna_tool_smith_1",
					"village/savanna/houses/savanna_weaponsmith_1",
					"village/savanna/houses/savanna_weaponsmith_2"),
			ids("village/savanna/savanna_lamp_post_01",
					"village/savanna/town_centers/savanna_meeting_point_1",
					"village/savanna/town_centers/savanna_meeting_point_2",
					"village/savanna/town_centers/savanna_meeting_point_3",
					"village/savanna/town_centers/savanna_meeting_point_4")),

	TAIGA(
			ids("village/taiga/houses/taiga_animal_pen_1",
					"village/taiga/houses/taiga_armorer_2",
					"village/taiga/houses/taiga_armorer_house_1",
					"village/taiga/houses/taiga_butcher_shop_1",
					"village/taiga/houses/taiga_cartographer_house_1",
					"village/taiga/houses/taiga_fisher_cottage_1",
					"village/taiga/houses/taiga_fletcher_house_1",
					"village/taiga/houses/taiga_large_farm_1",
					"village/taiga/houses/taiga_large_farm_2",
					"village/taiga/houses/taiga_small_farm_1",
					"village/taiga/houses/taiga_library_1",
					"village/taiga/houses/taiga_masons_house_1",
					"village/taiga/houses/taiga_medium_house_1",
					"village/taiga/houses/taiga_medium_house_2",
					"village/taiga/houses/taiga_medium_house_3",
					"village/taiga/houses/taiga_medium_house_4",
					"village/taiga/houses/taiga_shepherds_house_1",
					"village/taiga/houses/taiga_small_house_1",
					"village/taiga/houses/taiga_small_house_2",
					"village/taiga/houses/taiga_small_house_3",
					"village/taiga/houses/taiga_small_house_4",
					"village/taiga/houses/taiga_small_house_5",
					"village/taiga/houses/taiga_tannery_1",
					"village/taiga/houses/taiga_temple_1",
					"village/taiga/houses/taiga_tool_smith_1",
					"village/taiga/houses/taiga_weaponsmith_1",
					"village/taiga/houses/taiga_weaponsmith_2"),
			ids("village/taiga/taiga_lamp_post_1",
					"village/taiga/taiga_decoration_1",
					"village/taiga/taiga_decoration_2",
					"village/taiga/taiga_decoration_3",
					"village/taiga/taiga_decoration_4",
					"village/taiga/taiga_decoration_5",
					"village/taiga/taiga_decoration_6",
					"village/taiga/town_centers/taiga_meeting_point_1",
					"village/taiga/town_centers/taiga_meeting_point_2")),

	SNOWY(
			ids("village/snowy/houses/snowy_animal_pen_1",
					"village/snowy/houses/snowy_animal_pen_2",
					"village/snowy/houses/snowy_armorer_house_1",
					"village/snowy/houses/snowy_armorer_house_2",
					"village/snowy/houses/snowy_butchers_shop_1",
					"village/snowy/houses/snowy_butchers_shop_2",
					"village/snowy/houses/snowy_cartographer_house_1",
					"village/snowy/houses/snowy_farm_1",
					"village/snowy/houses/snowy_farm_2",
					"village/snowy/houses/snowy_fisher_cottage",
					"village/snowy/houses/snowy_fletcher_house_1",
					"village/snowy/houses/snowy_library_1",
					"village/snowy/houses/snowy_masons_house_1",
					"village/snowy/houses/snowy_masons_house_2",
					"village/snowy/houses/snowy_medium_house_1",
					"village/snowy/houses/snowy_medium_house_2",
					"village/snowy/houses/snowy_medium_house_3",
					"village/snowy/houses/snowy_shepherds_house_1",
					"village/snowy/houses/snowy_small_house_1",
					"village/snowy/houses/snowy_small_house_2",
					"village/snowy/houses/snowy_small_house_3",
					"village/snowy/houses/snowy_small_house_4",
					"village/snowy/houses/snowy_small_house_5",
					"village/snowy/houses/snowy_small_house_6",
					"village/snowy/houses/snowy_small_house_7",
					"village/snowy/houses/snowy_small_house_8",
					"village/snowy/houses/snowy_tannery_1",
					"village/snowy/houses/snowy_temple_1",
					"village/snowy/houses/snowy_tool_smith_1",
					"village/snowy/houses/snowy_weapon_smith_1"),
			ids("village/snowy/snowy_lamp_post_01",
					"village/snowy/snowy_lamp_post_02",
					"village/snowy/snowy_lamp_post_03",
					"village/snowy/town_centers/snowy_meeting_point_1",
					"village/snowy/town_centers/snowy_meeting_point_2",
					"village/snowy/town_centers/snowy_meeting_point_3"));

	public final List<Identifier> buildings;
	public final List<Identifier> decorations;

	VillageBiomeStyle(List<Identifier> buildings, List<Identifier> decorations) {
		this.buildings = buildings;
		this.decorations = decorations;
	}

	public Identifier randomBuilding(Random random) {
		return this.buildings.get(random.nextInt(this.buildings.size()));
	}

	public Identifier randomDecoration(Random random) {
		return this.decorations.get(random.nextInt(this.decorations.size()));
	}

	/**
	 * Exact matches where vanilla has a dedicated village style for this biome, then commonsense
	 * aesthetic substitutions (badlands &rarr; desert's sandstone look, exposed highlands &rarr;
	 * taiga's stone-and-spruce look) for biomes outside those five, falling back to plains for
	 * everything else (forest/jungle/swamp/ocean/cave biomes have no dedicated vanilla village style
	 * either, so plains is the genuine commonsense default there, not a lazy fallback).
	 */
	public static VillageBiomeStyle resolve(RegistryKey<Biome> biomeKey) {
		if (biomeKey.equals(BiomeKeys.DESERT) || biomeKey.equals(BiomeKeys.BADLANDS)
				|| biomeKey.equals(BiomeKeys.ERODED_BADLANDS) || biomeKey.equals(BiomeKeys.WOODED_BADLANDS)) {
			return DESERT;
		}
		if (biomeKey.equals(BiomeKeys.SAVANNA) || biomeKey.equals(BiomeKeys.SAVANNA_PLATEAU)
				|| biomeKey.equals(BiomeKeys.WINDSWEPT_SAVANNA)) {
			return SAVANNA;
		}
		if (biomeKey.equals(BiomeKeys.TAIGA) || biomeKey.equals(BiomeKeys.OLD_GROWTH_PINE_TAIGA)
				|| biomeKey.equals(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA) || biomeKey.equals(BiomeKeys.WINDSWEPT_HILLS)
				|| biomeKey.equals(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS) || biomeKey.equals(BiomeKeys.STONY_PEAKS)
				|| biomeKey.equals(BiomeKeys.FROZEN_PEAKS) || biomeKey.equals(BiomeKeys.JAGGED_PEAKS)) {
			return TAIGA;
		}
		if (biomeKey.equals(BiomeKeys.SNOWY_TAIGA) || biomeKey.equals(BiomeKeys.SNOWY_PLAINS)
				|| biomeKey.equals(BiomeKeys.ICE_SPIKES) || biomeKey.equals(BiomeKeys.GROVE)
				|| biomeKey.equals(BiomeKeys.SNOWY_SLOPES)) {
			return SNOWY;
		}
		return PLAINS;
	}

	private static List<Identifier> ids(String... paths) {
		List<Identifier> list = new ArrayList<>(paths.length);
		for (String path : paths) {
			list.add(Identifier.of("minecraft", path));
		}
		return List.copyOf(list);
	}
}
