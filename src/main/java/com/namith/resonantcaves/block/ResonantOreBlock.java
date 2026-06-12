package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.ResonantOreBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Feature 3 — Resonant Ore. A bedrock-hard ore block (see {@link ModBlocks}) carrying a
 * {@link ResonantOreBlockEntity} that stores this node's passive RF generation rate.
 */
public class ResonantOreBlock extends BlockWithEntity {
	public static final MapCodec<ResonantOreBlock> CODEC = createCodec(ResonantOreBlock::new);

	public ResonantOreBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<ResonantOreBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ResonantOreBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}
}
