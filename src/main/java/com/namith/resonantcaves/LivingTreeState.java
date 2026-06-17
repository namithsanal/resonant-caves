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
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 8 — per-world saved data tracking every active living-tree root. Vanilla log blocks have
 * no block entity, so there's nowhere else to hang per-position growth state.
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
		public long inflowSinceLastCheck;

		TreeData(BlockPos rootPos, Block logBlock) {
			this.rootPos = rootPos;
			this.logBlock = logBlock;
			this.logs.add(rootPos);
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
}
