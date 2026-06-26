package com.namith.resonantcaves.block;

import java.util.ArrayList;
import java.util.List;

import com.namith.resonantcaves.ResonantCaves;
import com.namith.resonantcaves.block.entity.VillageCoreBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.EmptyPoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Static helper that drives the Village Core's one-piece-per-midnight jigsaw expansion, mirroring
 * vanilla {@code StructurePoolBasedGenerator} without the all-at-once depth limit.
 */
public final class VillageExpander {
	private static final Identifier TOWN_CENTERS_ID = Identifier.of("minecraft", "village/plains/town_centers");
	// The vanilla terminator pool — connecting to it is always a dead end, skip adding it to frontier.
	private static final Identifier EMPTY_POOL = Identifier.of("minecraft", "empty");

	private VillageExpander() {
	}

	private static boolean isDeadEndPool(Registry<StructurePool> poolRegistry, String poolIdStr) {
		Identifier id = Identifier.tryParse(poolIdStr);
		if (id == null || id.equals(EMPTY_POOL)) return true;
		StructurePool p = poolRegistry.get(id);
		return p == null || p.getElementCount() == 0;
	}

	// -------------------------------------------------------------------------
	// Foundation (first midnight)
	// -------------------------------------------------------------------------

	/**
	 * Places the town-center piece near the core. Tries 8 offsets (cardinal + diagonal) looking for
	 * a surface that's within ±20Y of the core. Returns silently without mutating the entity if no
	 * suitable position is found.
	 */
	public static void layFoundation(ServerWorld world, BlockPos corePos, VillageCoreBlockEntity entity) {
		StructureTemplateManager templateManager = world.getStructureTemplateManager();
		DynamicRegistryManager registries = world.getRegistryManager();
		Registry<StructurePool> poolRegistry = registries.get(RegistryKeys.TEMPLATE_POOL);

		StructurePool pool = poolRegistry.get(TOWN_CENTERS_ID);
		if (pool == null || pool.getElementCount() == 0) {
			ResonantCaves.LOGGER.warn("[VillageCore] town_centers pool not found — skipping foundation.");
			return;
		}

		BlockPos foundationPos = findFoundationPos(world, corePos);
		if (foundationPos == null) {
			ResonantCaves.LOGGER.warn("[VillageCore] no valid surface near core at {} — skipping.", corePos);
			return;
		}

		List<StructurePoolElement> elements = pool.getElementIndicesInRandomOrder(world.getRandom());
		for (StructurePoolElement element : elements) {
			if (element instanceof EmptyPoolElement) continue;

			BlockRotation rotation = BlockRotation.random(world.getRandom());
			BlockBox boundingBox = element.getBoundingBox(templateManager, foundationPos, rotation);
			boolean success = element.generate(
					templateManager, world, world.getStructureAccessor(),
					world.getChunkManager().getChunkGenerator(),
					foundationPos, BlockPos.ORIGIN, rotation, boundingBox,
					world.getRandom(), StructureLiquidSettings.APPLY_WATERLOGGING, false);

			if (!success) continue;

			// Collect all jigsaw blocks from this piece as frontier connectors (skip dead-end pools)
			List<StructureTemplate.StructureBlockInfo> blocks =
					element.getStructureBlockInfos(templateManager, foundationPos, rotation, world.getRandom());
			for (StructureTemplate.StructureBlockInfo info : blocks) {
				if (!info.state().isOf(Blocks.JIGSAW) || info.nbt() == null) continue;
				NbtCompound jNbt = info.nbt();
				String kPoolId = jNbt.getString(JigsawBlockEntity.POOL_KEY);
				if (isDeadEndPool(poolRegistry, kPoolId)) continue;
				// info.pos() already offset by foundationPos (since we passed foundationPos, not ORIGIN)
				BlockPos worldKPos = info.pos();
				Direction facing = JigsawBlock.getFacing(info.state());
				String myName = jNbt.getString(JigsawBlockEntity.NAME_KEY);
				entity.frontier.add(new JigsawConnector(worldKPos, facing, kPoolId, myName));
			}
			ResonantCaves.LOGGER.info("[VillageCore] Foundation placed at {} — {} frontier connectors", foundationPos, entity.frontier.size());

			entity.placedBoxes.add(boundingBox);
			entity.foundationLaid = true;
			entity.piecesPlaced++;
			entity.markDirty();
			return;
		}
		ResonantCaves.LOGGER.warn("[VillageCore] could not place any town-center element near {}.", corePos);
	}

	private static BlockPos findFoundationPos(ServerWorld world, BlockPos corePos) {
		int[][] offsets = {{12, 0}, {-12, 0}, {0, 12}, {0, -12}, {9, 9}, {-9, 9}, {9, -9}, {-9, -9}};
		for (int[] offset : offsets) {
			int x = corePos.getX() + offset[0];
			int z = corePos.getZ() + offset[1];
			int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			if (Math.abs(surfaceY - corePos.getY()) <= 20) {
				return new BlockPos(x, surfaceY, z);
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Grow (subsequent midnights)
	// -------------------------------------------------------------------------

	/**
	 * Selects one open connector from the frontier (weighted by inverse distance to the core), picks
	 * a compatible piece from the connector's target pool, and places it. Removes dead-end connectors
	 * that have no valid placement. Mutates {@code entity.frontier}, {@code entity.placedBoxes},
	 * {@code entity.houseCount}, {@code entity.piecesPlaced}, and (for house pieces) spawns one
	 * villager and adds their UUID to {@code entity.citizenUUIDs}.
	 */
	public static void grow(ServerWorld world, BlockPos corePos, VillageCoreBlockEntity entity) {
		if (entity.frontier.isEmpty()) return;

		StructureTemplateManager templateManager = world.getStructureTemplateManager();
		DynamicRegistryManager registries = world.getRegistryManager();
		Registry<StructurePool> poolRegistry = registries.get(RegistryKeys.TEMPLATE_POOL);
		StructureAccessor structureAccessor = world.getStructureAccessor();
		ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();

		JigsawConnector selected = selectWeightedConnector(entity.frontier, corePos, world);

		Identifier poolId = Identifier.tryParse(selected.poolId());
		if (poolId == null) {
			entity.frontier.remove(selected);
			return;
		}
		StructurePool pool = poolRegistry.get(poolId);
		if (pool == null || pool.getElementCount() == 0) {
			entity.frontier.remove(selected);
			return;
		}

		List<StructurePoolElement> elements = pool.getElementIndicesInRandomOrder(world.getRandom());
		// Track whether any rotation+element fit geometrically (shape OK, may just need chunks loaded).
		// If so, leave the connector in the frontier for a later attempt once chunks load.
		boolean foundValidShape = false;

		for (StructurePoolElement element : elements) {
			if (element instanceof EmptyPoolElement) continue;

			for (BlockRotation rotation : BlockRotation.values()) {
				List<StructureTemplate.StructureBlockInfo> blocks =
						element.getStructureBlockInfos(templateManager, BlockPos.ORIGIN, rotation, world.getRandom());

				StructureTemplate.StructureBlockInfo matchJ = findMatchingJigsaw(blocks, selected);
				if (matchJ == null) continue;

				BlockPos placementPos = selected.worldPos().offset(selected.facing()).subtract(matchJ.pos());
				BlockBox boundingBox = element.getBoundingBox(templateManager, placementPos, rotation);

				if (boundingBox.contains(corePos)) continue;

				// Overlap check — shrink by 1 to allow the 1-block border sharing that vanilla village
				// pieces have at road/house junctions. Houses get a strict check against other houses
				// (stored separately) to prevent side-by-side houses sharing a wall.
				if (overlapsAny(boundingBox.expand(-1), entity.placedBoxes)) continue;
				if (selected.poolId().contains("/houses/") && overlapsAny(boundingBox, entity.placedHouseBoxes)) continue;

				foundValidShape = true;
				if (!allChunksLoaded(world, boundingBox)) continue;

				// Collect new frontier connectors, filtering dead-end pools at add time
				List<JigsawConnector> newConnectors = new ArrayList<>();
				for (StructureTemplate.StructureBlockInfo info : blocks) {
					if (!info.state().isOf(Blocks.JIGSAW) || info.nbt() == null) continue;
					if (info.pos().equals(matchJ.pos())) continue;
					NbtCompound kNbt = info.nbt();
					String kPoolId = kNbt.getString(JigsawBlockEntity.POOL_KEY);
					if (isDeadEndPool(poolRegistry, kPoolId)) continue;
					BlockPos worldKPos = placementPos.add(info.pos());
					Direction kFacing = JigsawBlock.getFacing(info.state());
					String kName = kNbt.getString(JigsawBlockEntity.NAME_KEY);
					newConnectors.add(new JigsawConnector(worldKPos, kFacing, kPoolId, kName));
				}

				entity.frontier.remove(selected);
				entity.frontier.addAll(newConnectors);
				entity.placedBoxes.add(boundingBox);

				// Carve terrain out of the footprint so house interiors aren't filled with dirt/sand.
				// Vanilla templates only store solid blocks; unrecorded positions keep whatever block was there.
				clearTerrain(world, boundingBox);

				boolean placed = element.generate(
						templateManager, world, structureAccessor, chunkGenerator,
						placementPos, BlockPos.ORIGIN, rotation, boundingBox,
						world.getRandom(), StructureLiquidSettings.APPLY_WATERLOGGING, false);

				if (!placed) {
					entity.frontier.add(selected);
					entity.frontier.removeAll(newConnectors);
					entity.placedBoxes.remove(boundingBox);
					continue;
				}

				if (selected.poolId().contains("/houses/")) {
					entity.houseCount++;
					entity.placedHouseBoxes.add(boundingBox);
					spawnVillager(world, placementPos, boundingBox, entity);
				}
				entity.piecesPlaced++;
				entity.markDirty();
				ResonantCaves.LOGGER.info("[VillageCore] Placed piece at {} pool={} frontier={}", placementPos, selected.poolId(), entity.frontier.size());
				return;
			}
		}

		if (!foundValidShape) {
			entity.frontier.remove(selected);
			ResonantCaves.LOGGER.info("[VillageCore] Dead-end removed pool={} pos={} frontier={}", selected.poolId(), selected.worldPos(), entity.frontier.size());
		}
	}

	private static StructureTemplate.StructureBlockInfo findMatchingJigsaw(
			List<StructureTemplate.StructureBlockInfo> blocks,
			JigsawConnector connector) {
		for (StructureTemplate.StructureBlockInfo info : blocks) {
			if (!info.state().isOf(Blocks.JIGSAW) || info.nbt() == null) continue;
			String target = info.nbt().getString(JigsawBlockEntity.TARGET_KEY);
			Direction facing = JigsawBlock.getFacing(info.state());
			if (target.equals(connector.myName()) && facing == connector.facing().getOpposite()) {
				return info;
			}
		}
		return null;
	}

	private static JigsawConnector selectWeightedConnector(
			List<JigsawConnector> frontier, BlockPos corePos, ServerWorld world) {
		if (frontier.size() == 1) return frontier.get(0);

		double totalWeight = 0;
		double[] weights = new double[frontier.size()];
		for (int i = 0; i < frontier.size(); i++) {
			double dist = frontier.get(i).worldPos().getManhattanDistance(corePos);
			weights[i] = 1.0 / (1.0 + dist / 16.0);
			totalWeight += weights[i];
		}

		double roll = world.getRandom().nextDouble() * totalWeight;
		double cumulative = 0;
		for (int i = 0; i < frontier.size(); i++) {
			cumulative += weights[i];
			if (roll <= cumulative) return frontier.get(i);
		}
		return frontier.get(frontier.size() - 1);
	}

	private static boolean overlapsAny(BlockBox box, List<BlockBox> placed) {
		for (BlockBox other : placed) {
			if (box.intersects(other)) return true;
		}
		return false;
	}

	private static boolean allChunksLoaded(ServerWorld world, BlockBox box) {
		int minCx = box.getMinX() >> 4;
		int maxCx = box.getMaxX() >> 4;
		int minCz = box.getMinZ() >> 4;
		int maxCz = box.getMaxZ() >> 4;
		for (int cx = minCx; cx <= maxCx; cx++) {
			for (int cz = minCz; cz <= maxCz; cz++) {
				if (!world.isChunkLoaded(cx, cz)) return false;
			}
		}
		return true;
	}

	private static void clearTerrain(ServerWorld world, BlockBox box) {
		for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
			for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
				for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
					BlockPos bp = new BlockPos(x, y, z);
					if (isNaturalTerrain(world.getBlockState(bp))) {
						world.setBlockState(bp, Blocks.AIR.getDefaultState(), 3);
					}
				}
			}
		}
	}

	private static boolean isNaturalTerrain(BlockState state) {
		Block b = state.getBlock();
		return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.COARSE_DIRT
			|| b == Blocks.PODZOL || b == Blocks.MUD || b == Blocks.ROOTED_DIRT
			|| b == Blocks.SAND || b == Blocks.RED_SAND || b == Blocks.SANDSTONE
			|| b == Blocks.GRAVEL
			|| b == Blocks.STONE || b == Blocks.DEEPSLATE
			|| b == Blocks.GRANITE || b == Blocks.DIORITE || b == Blocks.ANDESITE || b == Blocks.TUFF;
	}

	private static void spawnVillager(ServerWorld world, BlockPos placementPos, BlockBox box, VillageCoreBlockEntity entity) {
		// Spawn near the center of the bounding box, at surface height
		int centerX = (box.getMinX() + box.getMaxX()) / 2;
		int centerZ = (box.getMinZ() + box.getMaxZ()) / 2;
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);

		VillagerEntity villager = EntityType.VILLAGER.create(world);
		if (villager == null) return;
		villager.refreshPositionAndAngles(centerX + 0.5, y, centerZ + 0.5, 0f, 0f);
		world.spawnEntity(villager);
		entity.citizenUUIDs.add(villager.getUuid());
		entity.lifetimeSpawns++;
	}

	// -------------------------------------------------------------------------
	// JigsawConnector record
	// -------------------------------------------------------------------------

	/**
	 * Represents an open outward-facing jigsaw connector in an already-placed piece. Stored in the
	 * Village Core's frontier list and serialized to NBT across sessions.
	 *
	 * @param worldPos the block position of this jigsaw block in world space
	 * @param facing   which direction this connector points out (the direction of future expansion)
	 * @param poolId   string ID of the pool to pick the next piece from ({@code JigsawBlockEntity.POOL_KEY})
	 * @param myName   this connector's own name; the matching piece must have a jigsaw with
	 *                 {@code target == myName} ({@code JigsawBlockEntity.NAME_KEY})
	 */
	public record JigsawConnector(BlockPos worldPos, Direction facing, String poolId, String myName) {
		public NbtCompound toNbt() {
			NbtCompound nbt = new NbtCompound();
			nbt.putInt("X", worldPos.getX());
			nbt.putInt("Y", worldPos.getY());
			nbt.putInt("Z", worldPos.getZ());
			nbt.putString("Dir", facing.getName());
			nbt.putString("Pool", poolId);
			nbt.putString("Name", myName);
			return nbt;
		}

		public static JigsawConnector fromNbt(NbtCompound nbt) {
			BlockPos pos = new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"));
			Direction dir = Direction.byName(nbt.getString("Dir"));
			if (dir == null) dir = Direction.NORTH;
			return new JigsawConnector(pos, dir, nbt.getString("Pool"), nbt.getString("Name"));
		}
	}
}
