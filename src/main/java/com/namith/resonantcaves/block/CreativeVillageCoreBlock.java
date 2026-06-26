package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;

/**
 * Creative-only counterpart to {@link VillageCoreBlock}. Uses the same block entity type
 * ({@code VILLAGE_CORE}), same Storage Drawers interaction, and the same GUI — plus a "Simulate
 * Day" button (see {@code VillageCoreScreen}). No RF requirement: the block entity's
 * {@code processMidnight()} skips all energy checks when it sees this block type.
 *
 * <p>Not breakable by survival players ({@code UnbreakableBlocks} exempts Creative Village Core, so
 * creative admins can still pick it up).
 */
public class CreativeVillageCoreBlock extends VillageCoreBlock {
	public static final MapCodec<CreativeVillageCoreBlock> CODEC = createCodec(CreativeVillageCoreBlock::new);

	public CreativeVillageCoreBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<CreativeVillageCoreBlock> getCodec() {
		return CODEC;
	}
}
