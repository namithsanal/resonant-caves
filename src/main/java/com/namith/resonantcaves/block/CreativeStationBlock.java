package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.CreativeStationBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A debug/testing block that outputs energy at whatever rate is configured, materialized from
 * nothing — not craftable, creative-inventory-only (same distribution convention as vanilla's
 * Light Block/Barrier: visible in the creative tab and {@code /give}-able, never craftable).
 */
public class CreativeStationBlock extends BlockWithEntity {
	public static final MapCodec<CreativeStationBlock> CODEC = createCodec(CreativeStationBlock::new);

	public CreativeStationBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<CreativeStationBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeStationBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return null;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof CreativeStationBlockEntity station) {
			station.openScreen(serverPlayer);
		}
		return ActionResult.SUCCESS;
	}
}
