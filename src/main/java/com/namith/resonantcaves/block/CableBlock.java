package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.CableBlockEntity;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * A cable block for one {@link EnergyTier}. Behaviour lives in {@code CableBlockEntity} /
 * {@code CableNetworkTicker} (adapted from Tech Reborn's cable implementation) — this class is
 * the block shell, tier identity, and the connected-model look: it reuses vanilla's own
 * {@code north}/{@code east}/{@code south}/{@code west}/{@code up}/{@code down} boolean
 * properties (the same ones {@code FenceBlock}/{@code GlassPaneBlock} use) so the multipart
 * blockstate can draw a thin wire with arms only toward connected neighbors, instead of a solid
 * cube.
 */
public class CableBlock extends BlockWithEntity {
	public static final MapCodec<CableBlock> CODEC = createCodec(settings -> new CableBlock(EnergyTier.IRON, settings));

	// A small centered core plus one arm per connected direction — only this much of the block is
	// solid, so the hitbox matches the thin wire model instead of the full block (and, since the
	// resulting shape isn't a full cube, vanilla's default getOpacity/isTransparent automatically
	// lets light pass through too).
	private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(6, 6, 6, 10, 10, 10);
	private static final VoxelShape[] ARM_SHAPES = {
			Block.createCuboidShape(6, 6, 0, 10, 10, 6), // NORTH
			Block.createCuboidShape(10, 6, 6, 16, 10, 10), // EAST
			Block.createCuboidShape(6, 6, 10, 10, 10, 16), // SOUTH
			Block.createCuboidShape(0, 6, 6, 6, 10, 10), // WEST
			Block.createCuboidShape(6, 10, 6, 10, 16, 10), // UP
			Block.createCuboidShape(6, 0, 6, 10, 6, 10), // DOWN
	};
	private static final VoxelShape[] SHAPES_BY_CONNECTION_MASK = buildShapes();

	private static VoxelShape[] buildShapes() {
		VoxelShape[] shapes = new VoxelShape[1 << ARM_SHAPES.length];
		for (int mask = 0; mask < shapes.length; mask++) {
			VoxelShape shape = CORE_SHAPE;
			for (int bit = 0; bit < ARM_SHAPES.length; bit++) {
				if ((mask & (1 << bit)) != 0) {
					shape = VoxelShapes.union(shape, ARM_SHAPES[bit]);
				}
			}
			shapes[mask] = shape;
		}
		return shapes;
	}

	private static int connectionMask(BlockState state) {
		int mask = 0;
		if (state.get(Properties.NORTH)) mask |= 1;
		if (state.get(Properties.EAST)) mask |= 2;
		if (state.get(Properties.SOUTH)) mask |= 4;
		if (state.get(Properties.WEST)) mask |= 8;
		if (state.get(Properties.UP)) mask |= 16;
		if (state.get(Properties.DOWN)) mask |= 32;
		return mask;
	}

	private final EnergyTier tier;

	public CableBlock(EnergyTier tier, AbstractBlock.Settings settings) {
		super(settings);
		this.tier = tier;
		this.setDefaultState(this.stateManager.getDefaultState()
				.with(Properties.NORTH, false)
				.with(Properties.EAST, false)
				.with(Properties.SOUTH, false)
				.with(Properties.WEST, false)
				.with(Properties.UP, false)
				.with(Properties.DOWN, false));
	}

	public EnergyTier getTier() {
		return this.tier;
	}

	@Override
	protected MapCodec<CableBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CableBlockEntity(pos, state, this.tier);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, ModBlockEntities.CABLE, CableBlockEntity::tick);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = this.getDefaultState();
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.offset(direction);
			state = state.with(propertyFor(direction), connectsTo(world, neighborPos, world.getBlockState(neighborPos), direction));
		}
		return state;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return state.with(propertyFor(direction), connectsTo(world, neighborPos, neighborState, direction));
	}

	/** Connects to a same-tier cable, or to anything that exposes an {@link EnergyStorage} on this side. */
	private boolean connectsTo(WorldAccess world, BlockPos neighborPos, BlockState neighborState, Direction direction) {
		if (neighborState.getBlock() instanceof CableBlock neighborCable) {
			return neighborCable.tier == this.tier;
		}
		if (world instanceof World realWorld) {
			return EnergyStorage.SIDED.find(realWorld, neighborPos, direction.getOpposite()) != null;
		}
		return false;
	}

	private static BooleanProperty propertyFor(Direction direction) {
		return switch (direction) {
			case NORTH -> Properties.NORTH;
			case EAST -> Properties.EAST;
			case SOUTH -> Properties.SOUTH;
			case WEST -> Properties.WEST;
			case UP -> Properties.UP;
			case DOWN -> Properties.DOWN;
		};
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(Properties.NORTH, Properties.EAST, Properties.SOUTH, Properties.WEST, Properties.UP, Properties.DOWN);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_CONNECTION_MASK[connectionMask(state)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_CONNECTION_MASK[connectionMask(state)];
	}
}
