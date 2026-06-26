package com.namith.resonantcaves.block;

import com.mojang.serialization.MapCodec;
import com.namith.resonantcaves.block.entity.ModBlockEntities;
import com.namith.resonantcaves.block.entity.VillageCoreBlockEntity;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
 * The Village Core — an energy-powered village builder. Storage Drawers-style block interaction:
 * <ul>
 *   <li>Sneak + right-click → open stats/graph GUI</li>
 *   <li>Right-click holding emeralds → insert the whole stack</li>
 *   <li>Right-click with empty hand → extract 64 emeralds</li>
 * </ul>
 * Once placed in survival mode the block is unbreakable (see {@code UnbreakableBlocks}).
 */
public class VillageCoreBlock extends BlockWithEntity {
	public static final MapCodec<VillageCoreBlock> CODEC = createCodec(VillageCoreBlock::new);
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	public VillageCoreBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends VillageCoreBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new VillageCoreBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient) return null;
		return validateTicker(type, ModBlockEntities.VILLAGE_CORE, VillageCoreBlockEntity::tick);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		if (!(world.getBlockEntity(pos) instanceof VillageCoreBlockEntity entity)) return ActionResult.PASS;

		if (player.isSneaking()) {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				entity.openScreen(serverPlayer);
			}
			return ActionResult.SUCCESS;
		}

		ItemStack held = player.getMainHandStack();
		if (held.isOf(Items.EMERALD)) {
			entity.storedEmeralds += held.getCount();
			if (!player.isCreative()) held.setCount(0);
			entity.markDirty();
			return ActionResult.SUCCESS;
		}

		if (held.isEmpty()) {
			long take = Math.min(64L, entity.storedEmeralds);
			if (take > 0) {
				player.giveItemStack(new ItemStack(Items.EMERALD, (int) take));
				entity.storedEmeralds -= take;
				entity.markDirty();
			}
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
