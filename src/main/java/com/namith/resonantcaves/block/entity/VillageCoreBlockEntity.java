package com.namith.resonantcaves.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.namith.resonantcaves.block.CreativeVillageCoreBlock;
import com.namith.resonantcaves.block.VillageExpander;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenVillageCoreScreenPayload;
import com.namith.resonantcaves.network.payload.VillageCoreStateUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * The Village Core block entity. Accepts RF from the cable/station network (insert-only). Once per
 * Minecraft midnight it charges per-house + per-villager upkeep, pays out emeralds, and expands the
 * village by one jigsaw piece (or converts villagers to pillagers on deficit). Tracks citizen
 * villagers by UUID for accurate birth/death/spawn statistics gathered at midnight reconciliation —
 * no event listeners needed.
 */
public class VillageCoreBlockEntity extends BlockEntity {
	// --- Constants ---
	public static final int SCAN_RADIUS = 64;
	public static final long HOUSE_COST = 800L;
	public static final long VILLAGER_COST = 400L;
	public static final long GROWTH_COST = 60_000L;

	// --- Energy storage (insert-only) ---
	public final SimpleEnergyStorage coreEnergyStorage = new SimpleEnergyStorage(Long.MAX_VALUE, Long.MAX_VALUE, 0L) {
		@Override
		public boolean supportsExtraction() {
			return false;
		}
		@Override
		protected void onFinalCommit() {
			VillageCoreBlockEntity.this.markDirty();
		}
	};

	// --- Emerald drawer storage (Storage-Drawers-style: right-click with emeralds = insert, empty hand = extract 64) ---
	public long storedEmeralds = 0L;
	public final Storage<ItemVariant> emeraldStorage = new EmeraldStorage();

	// --- Persistent village state ---
	public final List<VillageExpander.JigsawConnector> frontier = new ArrayList<>();
	public final List<BlockBox> placedBoxes = new ArrayList<>();
	public final List<BlockBox> placedHouseBoxes = new ArrayList<>();
	public int houseCount = 0;
	public int piecesPlaced = 0;
	public boolean foundationLaid = false;

	// --- Statistics ---
	public long lifetimePillagersConverted = 0L;
	public long lifetimeBirths = 0L;
	public long lifetimeDeaths = 0L;
	public long lifetimeSpawns = 0L;
	public long peakPopulation = 0L;
	public long daysActive = 0L;

	// --- Citizen registry ---
	public final Set<UUID> citizenUUIDs = new HashSet<>();

	// --- Population history ---
	public final PopulationHistory populationHistory = new PopulationHistory();

	// --- Day tracking ---
	long lastProcessedDay = -1L;

	public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VILLAGE_CORE, pos, state);
	}

	// -------------------------------------------------------------------------
	// Tick
	// -------------------------------------------------------------------------

	public static void tick(World world, BlockPos pos, BlockState state, VillageCoreBlockEntity be) {
		if (world.isClient) return;
		boolean isCreative = state.getBlock() instanceof CreativeVillageCoreBlock;
		ServerWorld serverWorld = (ServerWorld) world;

		long currentDay = world.getTime() / 24000L;
		if (be.lastProcessedDay < 0) {
			be.lastProcessedDay = currentDay;
		} else if (currentDay > be.lastProcessedDay) {
			be.lastProcessedDay = currentDay;
			be.processMidnight(serverWorld, isCreative);
		}
	}

	// -------------------------------------------------------------------------
	// Midnight processing
	// -------------------------------------------------------------------------

	public void processMidnight(ServerWorld world, boolean isCreative) {
		daysActive++;

		// 1. Reconcile citizen registry
		reconcileVillagers(world);
		int villagerCount = citizenUUIDs.size();

		// 2. Village expansion / upkeep
		if (!foundationLaid) {
			VillageExpander.layFoundation(world, this.pos, this);
		} else {
			long upkeep = (long) houseCount * HOUSE_COST + (long) villagerCount * VILLAGER_COST;
			long energy = coreEnergyStorage.amount;

			if (isCreative || energy >= upkeep) {
				if (!isCreative) {
					coreEnergyStorage.amount -= upkeep;
					energy -= upkeep;
				}
				storedEmeralds += houseCount;
				boolean canGrow = isCreative || energy >= GROWTH_COST;
				if (canGrow && !frontier.isEmpty()) {
					// Retry until a piece is placed or frontier exhausted — a single attempt can hit
					// a dead-end connector without placing anything, so keep trying until success.
					int piecesBefore = piecesPlaced;
					int maxAttempts = Math.min(frontier.size() + 1, 25);
					for (int attempt = 0; attempt < maxAttempts && !frontier.isEmpty(); attempt++) {
						VillageExpander.grow(world, this.pos, this);
						if (piecesPlaced > piecesBefore) break;
					}
					if (!isCreative) coreEnergyStorage.amount = Math.max(0, coreEnergyStorage.amount - GROWTH_COST);
				}
			} else {
				long deficit = upkeep - energy;
				int unitsAtRisk = (int) Math.ceil((double) deficit / (HOUSE_COST + VILLAGER_COST));
				convertClusteredVillagers(world, unitsAtRisk);
			}
		}

		// 3–5. Stats + burn
		peakPopulation = Math.max(peakPopulation, (long) citizenUUIDs.size());
		populationHistory.append(citizenUUIDs.size());
		if (!isCreative) coreEnergyStorage.amount = 0;
		markDirty();
		pushStateUpdate(world);
	}

	private void reconcileVillagers(ServerWorld world) {
		Box wideBox = new Box(
				pos.getX() - SCAN_RADIUS * 2, pos.getY() - SCAN_RADIUS, pos.getZ() - SCAN_RADIUS * 2,
				pos.getX() + SCAN_RADIUS * 2 + 1, pos.getY() + SCAN_RADIUS + 1, pos.getZ() + SCAN_RADIUS * 2 + 1);

		Set<UUID> wideFound = new HashSet<>();
		for (VillagerEntity v : world.getEntitiesByClass(VillagerEntity.class, wideBox, VillagerEntity::isAlive)) {
			wideFound.add(v.getUuid());
		}

		// Deaths: registered but not found in the wide scan (presumed dead or wandered very far)
		Iterator<UUID> it = citizenUUIDs.iterator();
		while (it.hasNext()) {
			UUID uuid = it.next();
			if (!wideFound.contains(uuid)) {
				it.remove();
				lifetimeDeaths++;
			}
		}

		// Births: found in normal scan range but not registered
		Box normalBox = new Box(
				pos.getX() - SCAN_RADIUS, pos.getY() - SCAN_RADIUS, pos.getZ() - SCAN_RADIUS,
				pos.getX() + SCAN_RADIUS + 1, pos.getY() + SCAN_RADIUS + 1, pos.getZ() + SCAN_RADIUS + 1);
		for (VillagerEntity v : world.getEntitiesByClass(VillagerEntity.class, normalBox, VillagerEntity::isAlive)) {
			if (citizenUUIDs.add(v.getUuid())) {
				lifetimeBirths++;
			}
		}
	}

	private void convertClusteredVillagers(ServerWorld world, int unitsAtRisk) {
		if (unitsAtRisk <= 0) return;
		Box scanBox = new Box(
				pos.getX() - SCAN_RADIUS, pos.getY() - SCAN_RADIUS, pos.getZ() - SCAN_RADIUS,
				pos.getX() + SCAN_RADIUS + 1, pos.getY() + SCAN_RADIUS + 1, pos.getZ() + SCAN_RADIUS + 1);
		List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, scanBox, VillagerEntity::isAlive);
		if (villagers.isEmpty()) return;

		VillagerEntity anchor = villagers.get(world.getRandom().nextInt(villagers.size()));
		villagers.sort((a, b) -> {
			double da = a.squaredDistanceTo(anchor);
			double db = b.squaredDistanceTo(anchor);
			return Double.compare(da, db);
		});

		int converted = 0;
		for (VillagerEntity v : villagers) {
			if (converted >= unitsAtRisk) break;
			if (world.getRandom().nextFloat() < 0.25f) {
				citizenUUIDs.remove(v.getUuid());
				net.minecraft.entity.mob.PillagerEntity pillager = net.minecraft.entity.EntityType.PILLAGER.create(world);
				if (pillager != null) {
					pillager.refreshPositionAndAngles(v.getX(), v.getY(), v.getZ(), v.getYaw(), 0f);
					world.spawnEntity(pillager);
				}
				v.discard();
				lifetimePillagersConverted++;
				converted++;
			}
		}
	}

	// -------------------------------------------------------------------------
	// Screen
	// -------------------------------------------------------------------------

	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) return;
		boolean creative = this.getCachedState().getBlock() instanceof CreativeVillageCoreBlock;
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player, new OpenVillageCoreScreenPayload(
				this.pos,
				this.citizenUUIDs.size(),
				this.houseCount,
				this.piecesPlaced,
				this.lifetimePillagersConverted,
				this.lifetimeDeaths,
				this.peakPopulation,
				this.daysActive,
				this.storedEmeralds,
				this.populationHistory.snapshot(),
				creative));
	}

	private void pushStateUpdate(ServerWorld world) {
		VillageCoreStateUpdatePayload payload = new VillageCoreStateUpdatePayload(
				this.pos, this.citizenUUIDs.size(), this.houseCount, this.storedEmeralds);
		ModNetworking.pushVillageCoreUpdate(world.getServer(), this.pos, payload);
	}

	// -------------------------------------------------------------------------
	// NBT
	// -------------------------------------------------------------------------

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLong("StoredEnergy", coreEnergyStorage.amount);
		nbt.putLong("StoredEmeralds", storedEmeralds);
		nbt.putInt("HouseCount", houseCount);
		nbt.putInt("PiecesPlaced", piecesPlaced);
		nbt.putBoolean("FoundationLaid", foundationLaid);
		nbt.putLong("LastProcessedDay", lastProcessedDay);
		nbt.putLong("DaysActive", daysActive);
		nbt.putLong("LifetimePillagers", lifetimePillagersConverted);
		nbt.putLong("LifetimeBirths", lifetimeBirths);
		nbt.putLong("LifetimeDeaths", lifetimeDeaths);
		nbt.putLong("LifetimeSpawns", lifetimeSpawns);
		nbt.putLong("PeakPopulation", peakPopulation);

		// Citizen UUIDs
		NbtList citizenList = new NbtList();
		for (UUID uuid : citizenUUIDs) {
			NbtCompound entry = new NbtCompound();
			entry.putLong("M", uuid.getMostSignificantBits());
			entry.putLong("L", uuid.getLeastSignificantBits());
			citizenList.add(entry);
		}
		nbt.put("CitizenUUIDs", citizenList);

		// Frontier
		NbtList frontierList = new NbtList();
		for (VillageExpander.JigsawConnector c : frontier) {
			frontierList.add(c.toNbt());
		}
		nbt.put("Frontier", frontierList);

		// Placed boxes
		NbtList boxList = new NbtList();
		for (BlockBox box : placedBoxes) {
			NbtCompound boxNbt = new NbtCompound();
			boxNbt.putIntArray("Coords", new int[]{
					box.getMinX(), box.getMinY(), box.getMinZ(),
					box.getMaxX(), box.getMaxY(), box.getMaxZ()});
			boxList.add(boxNbt);
		}
		nbt.put("PlacedBoxes", boxList);

		// Placed house boxes (for strict house-to-house overlap detection)
		NbtList houseBoxList = new NbtList();
		for (BlockBox box : placedHouseBoxes) {
			NbtCompound boxNbt = new NbtCompound();
			boxNbt.putIntArray("Coords", new int[]{
					box.getMinX(), box.getMinY(), box.getMinZ(),
					box.getMaxX(), box.getMaxY(), box.getMaxZ()});
			houseBoxList.add(boxNbt);
		}
		nbt.put("PlacedHouseBoxes", houseBoxList);

		populationHistory.writeNbt(nbt, "PopHistory");
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		coreEnergyStorage.amount = nbt.getLong("StoredEnergy");
		storedEmeralds = nbt.getLong("StoredEmeralds");
		houseCount = nbt.getInt("HouseCount");
		piecesPlaced = nbt.getInt("PiecesPlaced");
		foundationLaid = nbt.getBoolean("FoundationLaid");
		lastProcessedDay = nbt.contains("LastProcessedDay") ? nbt.getLong("LastProcessedDay") : -1L;
		daysActive = nbt.getLong("DaysActive");
		lifetimePillagersConverted = nbt.getLong("LifetimePillagers");
		lifetimeBirths = nbt.getLong("LifetimeBirths");
		lifetimeDeaths = nbt.getLong("LifetimeDeaths");
		lifetimeSpawns = nbt.getLong("LifetimeSpawns");
		peakPopulation = nbt.getLong("PeakPopulation");

		citizenUUIDs.clear();
		NbtList citizenList = nbt.getList("CitizenUUIDs", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < citizenList.size(); i++) {
			NbtCompound entry = citizenList.getCompound(i);
			citizenUUIDs.add(new UUID(entry.getLong("M"), entry.getLong("L")));
		}

		frontier.clear();
		NbtList frontierList = nbt.getList("Frontier", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < frontierList.size(); i++) {
			VillageExpander.JigsawConnector c = VillageExpander.JigsawConnector.fromNbt(frontierList.getCompound(i));
			if (c != null) frontier.add(c);
		}

		placedBoxes.clear();
		NbtList boxList = nbt.getList("PlacedBoxes", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < boxList.size(); i++) {
			int[] coords = boxList.getCompound(i).getIntArray("Coords");
			if (coords.length == 6) {
				placedBoxes.add(new BlockBox(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]));
			}
		}

		placedHouseBoxes.clear();
		NbtList houseBoxList = nbt.getList("PlacedHouseBoxes", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < houseBoxList.size(); i++) {
			int[] coords = houseBoxList.getCompound(i).getIntArray("Coords");
			if (coords.length == 6) {
				placedHouseBoxes.add(new BlockBox(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]));
			}
		}

		populationHistory.readNbt(nbt, "PopHistory");
	}

	// -------------------------------------------------------------------------
	// Emerald drawer storage (Fabric Transfer API — for hopper/pipe compatibility)
	// -------------------------------------------------------------------------

	final class EmeraldStorage extends SnapshotParticipant<Long> implements Storage<ItemVariant> {
		@Override
		protected Long createSnapshot() {
			return VillageCoreBlockEntity.this.storedEmeralds;
		}

		@Override
		protected void readSnapshot(Long snapshot) {
			VillageCoreBlockEntity.this.storedEmeralds = snapshot;
		}

		@Override
		protected void onFinalCommit() {
			VillageCoreBlockEntity.this.markDirty();
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (!resource.isOf(Items.EMERALD) || maxAmount <= 0) return 0;
			updateSnapshots(transaction);
			VillageCoreBlockEntity.this.storedEmeralds += maxAmount;
			return maxAmount;
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (!resource.isOf(Items.EMERALD) || maxAmount <= 0) return 0;
			updateSnapshots(transaction);
			long taken = Math.min(maxAmount, VillageCoreBlockEntity.this.storedEmeralds);
			VillageCoreBlockEntity.this.storedEmeralds -= taken;
			return taken;
		}

		@Override
		public Iterator<StorageView<ItemVariant>> iterator() {
			long count = VillageCoreBlockEntity.this.storedEmeralds;
			if (count <= 0) return Collections.emptyIterator();
			StorageView<ItemVariant> view = new StorageView<>() {
				@Override
				public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
					return EmeraldStorage.this.extract(resource, maxAmount, tx);
				}
				@Override public boolean isResourceBlank() { return false; }
				@Override public ItemVariant getResource() { return ItemVariant.of(Items.EMERALD); }
				@Override public long getAmount() { return VillageCoreBlockEntity.this.storedEmeralds; }
				@Override public long getCapacity() { return Long.MAX_VALUE; }
			};
			return Collections.singletonList(view).iterator();
		}
	}
}
