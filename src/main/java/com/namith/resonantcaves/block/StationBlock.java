package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.block.entity.StationBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A single-block station: one face (the {@code FACING} side, pointing toward the player who
 * placed it, like a furnace) only releases energy, throttled by {@code targetOutput}; every other
 * face only accepts it. Right-clicking opens {@code StationScreen} for the live stored-energy
 * readout and a precise output setter. Replaced the original 2-tall lower/upper design — it
 * didn't carry its visual complexity well enough to be worth it.
 */
public class StationBlock extends BlockWithEntity {
	public static final MapCodec<StationBlock> CODEC = createCodec(settings -> new StationBlock(EnergyTier.IRON, settings));
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	private final EnergyTier tier;

	public StationBlock(EnergyTier tier, AbstractBlock.Settings settings) {
		super(settings);
		this.tier = tier;
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
	}

	public EnergyTier getTier() {
		return this.tier;
	}

	@Override
	protected MapCodec<StationBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new StationBlockEntity(pos, state, this.tier);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, ModBlockEntities.STATION, StationBlockEntity::tick);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof StationBlockEntity station) {
			station.openScreen(serverPlayer);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
