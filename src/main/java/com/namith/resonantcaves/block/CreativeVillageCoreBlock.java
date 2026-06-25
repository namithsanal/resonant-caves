package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.CreativeVillageCoreBlockEntity;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.block.entity.VillageCoreBlockEntity;
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
 * Debug/testing variant of the Village Core: always has a full tank, so the growth loop can be
 * watched with no cable network — same distribution convention as the Creative Station (not
 * craftable, creative-inventory-only).
 */
public class CreativeVillageCoreBlock extends BlockWithEntity {
	public static final MapCodec<CreativeVillageCoreBlock> CODEC = createCodec(CreativeVillageCoreBlock::new);

	public CreativeVillageCoreBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<CreativeVillageCoreBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeVillageCoreBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, ModBlockEntities.CREATIVE_VILLAGE_CORE, VillageCoreBlockEntity::tick);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof CreativeVillageCoreBlockEntity core) {
			core.openScreen(serverPlayer);
		}
		return ActionResult.SUCCESS;
	}
}
