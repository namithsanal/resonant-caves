package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.ResonantCaves;
import com.namith.resonantcaves.block.CableBlock;
import com.namith.resonantcaves.block.ModBlocks;
import com.namith.resonantcaves.block.StationBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers the block entity types shared across each tiered block family (ore, cable, ...).
 */
public final class ModBlockEntities {
	private ModBlockEntities() {
	}

	public static final BlockEntityType<ResonantOreBlockEntity> RESONANT_ORE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "resonant_ore"),
			BlockEntityType.Builder.create(ResonantOreBlockEntity::new, ModBlocks.RESONANT_ORE, ModBlocks.DEEPSLATE_RESONANT_ORE).build(null));

	public static final BlockEntityType<CableBlockEntity> CABLE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "cable"),
			BlockEntityType.Builder.create(
					(pos, state) -> new CableBlockEntity(pos, state, ((CableBlock) state.getBlock()).getTier()),
					ModBlocks.IRON_CABLE, ModBlocks.COPPER_CABLE, ModBlocks.GOLD_CABLE).build(null));

	public static final BlockEntityType<StationBlockEntity> STATION = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "station"),
			BlockEntityType.Builder.create(
					(pos, state) -> new StationBlockEntity(pos, state, ((StationBlock) state.getBlock()).getTier()),
					ModBlocks.IRON_STATION, ModBlocks.COPPER_STATION, ModBlocks.GOLD_STATION).build(null));

	public static final BlockEntityType<MonitorBlockEntity> MONITOR = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "energy_monitor"),
			BlockEntityType.Builder.create(MonitorBlockEntity::new, ModBlocks.ENERGY_MONITOR).build(null));

	public static final BlockEntityType<CreativeStationBlockEntity> CREATIVE_STATION = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "creative_station"),
			BlockEntityType.Builder.create(CreativeStationBlockEntity::new, ModBlocks.CREATIVE_STATION).build(null));

	public static final BlockEntityType<CreativeVillageCoreBlockEntity> CREATIVE_VILLAGE_CORE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(ResonantCaves.MOD_ID, "creative_village_core"),
			BlockEntityType.Builder.create(CreativeVillageCoreBlockEntity::new, ModBlocks.CREATIVE_VILLAGE_CORE).build(null));

	public static void register() {
		CableNetworkTicker.register();
	}
}
