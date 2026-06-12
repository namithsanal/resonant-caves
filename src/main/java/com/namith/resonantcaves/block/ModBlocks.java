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

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, Identifier.of(ResonantCaves.MOD_ID, name), block);
	}

	public static void register() {
		RegistryKey<ItemGroup> natural = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "natural"));
		ItemGroupEvents.modifyEntriesEvent(natural).register(entries -> entries.prepend(RESONANT_ORE_ITEM));
	}
}
