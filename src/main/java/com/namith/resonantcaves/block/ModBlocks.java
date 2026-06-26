package com.namith.resonantcaves.block;

import com.namith.resonantcaves.ResonantCaves;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Feature 3 — Resonant Ore.
 *
 * <p>Registers the stone and deepslate ore variants — both bedrock-hard (see
 * {@code AbstractBlock.Settings#strength(-1.0F, 3600000.0F)}, just like {@code minecraft:bedrock})
 * so they can never be mined, in any game mode, with no extra mixin needed. Only the stone variant
 * gets a {@link BlockItem}: since neither block can ever be obtained as an item in survival, one
 * creative-inventory entry is enough to identify the ore.
 */
public final class ModBlocks {
	private ModBlocks() {
	}

	public static final Block RESONANT_ORE = register("resonant_ore",
			new ResonantOreBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.STONE_GRAY)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.strength(-1.0F, 3600000.0F)
					.luminance(state -> 5)
					.dropsNothing()));

	public static final Block DEEPSLATE_RESONANT_ORE = register("deepslate_resonant_ore",
			new ResonantOreBlock(AbstractBlock.Settings.copyShallow(RESONANT_ORE)
					.mapColor(MapColor.DEEPSLATE_GRAY)
					.sounds(BlockSoundGroup.DEEPSLATE)));

	public static final Item RESONANT_ORE_ITEM = Registry.register(Registries.ITEM,
			Identifier.of(ResonantCaves.MOD_ID, "resonant_ore"),
			new BlockItem(RESONANT_ORE, new Item.Settings()));

	// Energy infrastructure (cables) — three tiers, worst-to-best IRON/COPPER/GOLD. See EnergyTier
	// for the loss-per-cable/minimum-throughput numbers that distinguish them.
	public static final Block IRON_CABLE = register("iron_cable",
			new CableBlock(EnergyTier.IRON, AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(2.0F, 6.0F)
					.sounds(BlockSoundGroup.METAL)
					.nonOpaque()));

	public static final Block COPPER_CABLE = register("copper_cable",
			new CableBlock(EnergyTier.COPPER, AbstractBlock.Settings.copyShallow(IRON_CABLE)
					.mapColor(MapColor.ORANGE)));

	public static final Block GOLD_CABLE = register("gold_cable",
			new CableBlock(EnergyTier.GOLD, AbstractBlock.Settings.copyShallow(IRON_CABLE)
					.mapColor(MapColor.GOLD)));

	public static final Item IRON_CABLE_ITEM = registerBlockItem("iron_cable", IRON_CABLE);
	public static final Item COPPER_CABLE_ITEM = registerBlockItem("copper_cable", COPPER_CABLE);
	public static final Item GOLD_CABLE_ITEM = registerBlockItem("gold_cable", GOLD_CABLE);

	// Stations — single block, one output face (see StationBlock).
	public static final Block IRON_STATION = register("iron_station",
			new StationBlock(EnergyTier.IRON, AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(3.5F, 6.0F)
					.sounds(BlockSoundGroup.METAL)));

	public static final Block COPPER_STATION = register("copper_station",
			new StationBlock(EnergyTier.COPPER, AbstractBlock.Settings.copyShallow(IRON_STATION)
					.mapColor(MapColor.ORANGE)));

	public static final Block GOLD_STATION = register("gold_station",
			new StationBlock(EnergyTier.GOLD, AbstractBlock.Settings.copyShallow(IRON_STATION)
					.mapColor(MapColor.GOLD)));

	public static final Item IRON_STATION_ITEM = registerBlockItem("iron_station", IRON_STATION);
	public static final Item COPPER_STATION_ITEM = registerBlockItem("copper_station", COPPER_STATION);
	public static final Item GOLD_STATION_ITEM = registerBlockItem("gold_station", GOLD_STATION);

	// Monitor — ambient flow/status display, see MonitorBlock/MonitorBlockEntity.
	public static final Block ENERGY_MONITOR = register("energy_monitor",
			new MonitorBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.LIGHT_GRAY)
					.strength(2.0F, 6.0F)
					.sounds(BlockSoundGroup.METAL)
					.nonOpaque()));

	public static final Item ENERGY_MONITOR_ITEM = registerBlockItem("energy_monitor", ENERGY_MONITOR);

	// Creative Station — a debug/testing energy source, not craftable; same creative-tab-only,
	// /give-able distribution convention as vanilla's Light Block/Barrier.
	public static final Block CREATIVE_STATION = register("creative_station",
			new CreativeStationBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.PINK)
					.strength(3.5F, 6.0F)
					.sounds(BlockSoundGroup.METAL)));

	public static final Item CREATIVE_STATION_ITEM = registerBlockItem("creative_station", CREATIVE_STATION);

	// Creative Village Core — debug/creative-only; places a plains village on demand.
	public static final Block CREATIVE_VILLAGE_CORE = register("creative_village_core",
			new CreativeVillageCoreBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.EMERALD_GREEN)
					.strength(3.5F, 6.0F)
					.sounds(BlockSoundGroup.STONE)));

	public static final Item CREATIVE_VILLAGE_CORE_ITEM = registerBlockItem("creative_village_core", CREATIVE_VILLAGE_CORE);

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, Identifier.of(ResonantCaves.MOD_ID, name), block);
	}

	private static Item registerBlockItem(String name, Block block) {
		return Registry.register(Registries.ITEM,
				Identifier.of(ResonantCaves.MOD_ID, name),
				new BlockItem(block, new Item.Settings()));
	}

	public static void register() {
		RegistryKey<ItemGroup> natural = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "natural"));
		ItemGroupEvents.modifyEntriesEvent(natural).register(entries -> entries.prepend(RESONANT_ORE_ITEM));

		RegistryKey<ItemGroup> redstone = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "redstone"));
		ItemGroupEvents.modifyEntriesEvent(redstone).register(entries -> {
			entries.add(IRON_CABLE_ITEM);
			entries.add(COPPER_CABLE_ITEM);
			entries.add(GOLD_CABLE_ITEM);
			entries.add(IRON_STATION_ITEM);
			entries.add(COPPER_STATION_ITEM);
			entries.add(GOLD_STATION_ITEM);
			entries.add(ENERGY_MONITOR_ITEM);
			entries.add(CREATIVE_STATION_ITEM);
			entries.add(CREATIVE_VILLAGE_CORE_ITEM);
		});
	}
}
