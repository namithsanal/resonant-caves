package com.namith.resonantcaves;

import net.minecraft.block.Block;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 8 — per-world saved data tracking every active living-tree root. Vanilla log blocks have
 * no block entity, so there's nowhere else to hang per-position growth state.
 *
 * <p>Growth is an explicit L-system: {@code tipDirections} is the active frontier (every position
 * still eligible to extend, with the direction it's currently heading), and {@code parents} is the
 * graph's parent link for every non-root log. Tracking this explicitly — rather than inferring tips
 * from spatial adjacency — means a branch curling back near another part of the tree can never be
 * mistaken for "no tips left," which is what caused leaves to vanish under the old geometric model.
 */
public class LivingTreeState extends PersistentState {
	private static final String STATE_ID = "resonantcaves_living_trees";
	private static final PersistentState.Type<LivingTreeState> TYPE =
			new PersistentState.Type<>(LivingTreeState::new, LivingTreeState::fromNbt, DataFixTypes.LEVEL);

	public static final class TreeData {
		public final BlockPos rootPos;
		public final Block logBlock;
		public final List<BlockPos> logs = new ArrayList<>();
		public final List<BlockPos> leaves = new ArrayList<>();
		public final Map<BlockPos, Direction> tipDirections = new LinkedHashMap<>();
		public final Map<BlockPos, BlockPos> parents = new LinkedHashMap<>();
		public long inflowSinceLastCheck;

		TreeData(BlockPos rootPos, Block logBlock) {
			this.rootPos = rootPos;
			this.logBlock = logBlock;
			this.logs.add(rootPos);
			this.tipDirections.put(rootPos, Direction.UP);
		}
	}

	private final Map<BlockPos, TreeData> trees = new LinkedHashMap<>();

	public static LivingTreeState getOrCreate(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
	}

	public TreeData getOrCreateTree(BlockPos rootPos, Block logBlock) {
		TreeData data = trees.computeIfAbsent(rootPos, pos -> new TreeData(pos, logBlock));
		markDirty();
		return data;
	}

	public TreeData getTree(BlockPos rootPos) {
		return trees.get(rootPos);
	}

	public void removeTree(BlockPos rootPos) {
		if (trees.remove(rootPos) != null) {
			markDirty();
		}
	}

	public Map<BlockPos, TreeData> getTrees() {
		return trees;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		NbtList list = new NbtList();
		for (TreeData data : trees.values()) {
			NbtCompound entry = new NbtCompound();
			entry.put("Root", NbtHelper.fromBlockPos(data.rootPos));
			entry.putString("LogBlock", Registries.BLOCK.getId(data.logBlock).toString());
			entry.putLong("Inflow", data.inflowSinceLastCheck);
			entry.put("Logs", writePosList(data.logs));
			entry.put("Leaves", writePosList(data.leaves));
			entry.put("Tips", writeTipMap(data.tipDirections));
			entry.put("Parents", writeParentMap(data.parents));
			list.add(entry);
		}
		nbt.put("Trees", list);
		return nbt;
	}

	public static LivingTreeState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		LivingTreeState state = new LivingTreeState();
		for (NbtElement element : nbt.getList("Trees", NbtElement.COMPOUND_TYPE)) {
			NbtCompound entry = (NbtCompound) element;
			BlockPos root = NbtHelper.toBlockPos(entry, "Root").orElse(null);
			Block logBlock = Registries.BLOCK.get(Identifier.of(entry.getString("LogBlock")));
			if (root == null || logBlock == null) {
				continue;
			}
			TreeData data = new TreeData(root, logBlock);
			data.logs.clear();
			data.logs.addAll(readPosList(entry.getList("Logs", NbtElement.INT_ARRAY_TYPE)));
			data.leaves.addAll(readPosList(entry.getList("Leaves", NbtElement.INT_ARRAY_TYPE)));
			data.inflowSinceLastCheck = entry.getLong("Inflow");
			data.tipDirections.clear();
			readTipMap(entry.getList("Tips", NbtElement.COMPOUND_TYPE), data.tipDirections);
			readParentMap(entry.getList("Parents", NbtElement.COMPOUND_TYPE), data.parents);
			state.trees.put(root, data);
		}
		return state;
	}

	private static NbtList writePosList(List<BlockPos> positions) {
		NbtList list = new NbtList();
		for (BlockPos pos : positions) {
			list.add(NbtHelper.fromBlockPos(pos));
		}
		return list;
	}

	private static List<BlockPos> readPosList(NbtList list) {
		List<BlockPos> positions = new ArrayList<>();
		for (NbtElement element : list) {
			int[] arr = ((NbtIntArray) element).getIntArray();
			if (arr.length == 3) {
				positions.add(new BlockPos(arr[0], arr[1], arr[2]));
			}
		}
		return positions;
	}

	private static NbtList writeTipMap(Map<BlockPos, Direction> tips) {
		NbtList list = new NbtList();
		for (Map.Entry<BlockPos, Direction> tip : tips.entrySet()) {
			NbtCompound entry = new NbtCompound();
			entry.put("Pos", NbtHelper.fromBlockPos(tip.getKey()));
			entry.putString("Dir", tip.getValue().getName());
			list.add(entry);
		}
		return list;
	}

	private static void readTipMap(NbtList list, Map<BlockPos, Direction> out) {
		for (NbtElement element : list) {
			NbtCompound entry = (NbtCompound) element;
			BlockPos pos = NbtHelper.toBlockPos(entry, "Pos").orElse(null);
			Direction dir = Direction.byName(entry.getString("Dir"));
			if (pos != null && dir != null) {
				out.put(pos, dir);
			}
		}
	}

	private static NbtList writeParentMap(Map<BlockPos, BlockPos> parents) {
		NbtList list = new NbtList();
		for (Map.Entry<BlockPos, BlockPos> entry : parents.entrySet()) {
			NbtCompound compound = new NbtCompound();
			compound.put("Child", NbtHelper.fromBlockPos(entry.getKey()));
			compound.put("Parent", NbtHelper.fromBlockPos(entry.getValue()));
			list.add(compound);
		}
		return list;
	}

	private static void readParentMap(NbtList list, Map<BlockPos, BlockPos> out) {
		for (NbtElement element : list) {
			NbtCompound entry = (NbtCompound) element;
			BlockPos child = NbtHelper.toBlockPos(entry, "Child").orElse(null);
			BlockPos parent = NbtHelper.toBlockPos(entry, "Parent").orElse(null);
			if (child != null && parent != null) {
				out.put(child, parent);
			}
		}
	}
}
