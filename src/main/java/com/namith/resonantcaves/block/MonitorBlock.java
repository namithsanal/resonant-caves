package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.block.entity.MonitorBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * A thin, item-frame-like plate that mounts flush against one face of a cable (including its top
 * or bottom) and reads that exact neighbor's data via {@code FACING} — no scanning, no ambiguity
 * about which block it reads. The ambient {@code STATUS}/{@code FLOW} faces update every tick;
 * right-clicking additionally opens a GUI with a line graph of the cable's throughput history.
 */
public class MonitorBlock extends BlockWithEntity {
	public static final MapCodec<MonitorBlock> CODEC = createCodec(MonitorBlock::new);
	public static final EnumProperty<MonitorStatus> STATUS = EnumProperty.of("status", MonitorStatus.class);
	public static final EnumProperty<MonitorFlow> FLOW = EnumProperty.of("flow", MonitorFlow.class);
	public static final DirectionProperty FACING = Properties.FACING;

	// A thin plate (2/16 thick) flush against whichever side FACING points away from.
	private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 14, 16, 16, 16);
	private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 2);
	private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 2, 16, 16);
	private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
	private static final VoxelShape UP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 2, 16);
	private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(0, 14, 0, 16, 16, 16);

	public MonitorBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState()
				.with(STATUS, MonitorStatus.IDLE).with(FLOW, MonitorFlow.NONE).with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<MonitorBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		BlockPos attachedPos = ctx.getBlockPos().offset(side.getOpposite());
		if (!isMountable(ctx.getWorld().getBlockState(attachedPos).getBlock())) {
			return null;
		}
		return this.getDefaultState().with(FACING, side);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos attachedPos = pos.offset(state.get(FACING).getOpposite());
		return isMountable(world.getBlockState(attachedPos).getBlock());
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction == state.get(FACING).getOpposite() && !isMountable(neighborState.getBlock())) {
			return Blocks.AIR.getDefaultState();
		}
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}

	private static boolean isMountable(Block block) {
		return block instanceof CableBlock;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new MonitorBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, ModBlockEntities.MONITOR, MonitorBlockEntity::tick);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(STATUS, FLOW, FACING);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
			monitor.openScreen(serverPlayer);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFor(state.get(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFor(state.get(FACING));
	}

	private static VoxelShape shapeFor(Direction facing) {
		return switch (facing) {
			case SOUTH -> SOUTH_SHAPE;
			case EAST -> EAST_SHAPE;
			case WEST -> WEST_SHAPE;
			case UP -> UP_SHAPE;
			case DOWN -> DOWN_SHAPE;
			default -> NORTH_SHAPE;
		};
	}
}
