package com.namith.surfacesurvival.block.entity;

import com.namith.surfacesurvival.SurfaceSurvival;
import com.namith.surfacesurvival.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Feature 3 — Resonant Ore. Registers the block entity type shared by both ore variants.
 */
public final class ModBlockEntities {
	private ModBlockEntities() {
	}

	public static final BlockEntityType<ResonantOreBlockEntity> RESONANT_ORE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(SurfaceSurvival.MOD_ID, "resonant_ore"),
			BlockEntityType.Builder.create(ResonantOreBlockEntity::new, ModBlocks.RESONANT_ORE, ModBlocks.DEEPSLATE_RESONANT_ORE).build(null));

	public static void register() {
	}
}
