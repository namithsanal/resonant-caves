package com.namith.resonantcaves.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.namith.resonantcaves.ResonantCaves;
import com.namith.resonantcaves.block.VillageBiomeStyle;
import com.namith.resonantcaves.network.ModNetworking;
import com.namith.resonantcaves.network.payload.OpenVillageCoreScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.LimitingEnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Founds and grows a village: a pure {@code EnergyStorage} sink (wired into the cable network
 * exactly like a Station) that, once per Minecraft day, pays upkeep for its houses/villagers,
 * spends any single-day surplus on a new house/decoration, then burns whatever's left — it is
 * deliberately not a long-term battery. A deficit day instead rolls pillager conversion for
 * however many villagers' worth of upkeep went unpaid.
 */
public class VillageCoreBlockEntity extends BlockEntity {
	public static final long CORE_CAPACITY = 60_000L;
	public static final long HOUSE_GROWTH_THRESHOLD = 50_000L;

	private static final long DAY_LENGTH_TICKS = 24_000L;
	private static final int MAX_SITE_SEARCH_RADIUS = 16;
	private static final int SITE_SEARCH_ATTEMPTS = 48;
	private static final int OVERLAP_MARGIN = 2;
	private static final int DECORATION_EVERY_N_GROWTHS = 4;
	private static final int STARTER_VILLAGER_PLACEMENT_ATTEMPTS = 6;
	private static final long DEFAULT_HOUSE_BASE_DRAW_PER_DAY = 400L;
	private static final long DEFAULT_HOUSE_PER_VILLAGER_DRAW_PER_DAY = 150L;
	private static final float PILLAGER_CONVERSION_CHANCE = 0.25F;
	private static final BlockRotation[] ROTATIONS = BlockRotation.values();

	private final SimpleEnergyStorage realStorage = new SimpleEnergyStorage(CORE_CAPACITY, Long.MAX_VALUE, Long.MAX_VALUE) {
		@Override
		protected void onFinalCommit() {
			VillageCoreBlockEntity.this.markDirty();
		}
	};

	private final List<VillageHouseRecord> structures = new ArrayList<>();
	@Nullable
	private VillageBiomeStyle style;
	private long lastDailyCheckTime;
	private int growthEventCount;
	private int lastPopulation;
	private long lastTotalDemand;
	private long lastTotalPaid;
	private boolean lastDaySustained = true;

	public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VILLAGE_CORE, pos, state);
	}

	protected VillageCoreBlockEntity(net.minecraft.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Insert-only on every side, externally — internal upkeep/repair draws bypass this via {@link #consumeForUpkeep}. */
	public EnergyStorage getEnergyStorage() {
		return new LimitingEnergyStorage(this.realStorage, Long.MAX_VALUE, 0);
	}

	public long getStoredEnergyForDisplay() {
		return this.realStorage.amount;
	}

	public int getHouseCount() {
		int count = 0;
		for (VillageHouseRecord structure : this.structures) {
			if (!structure.decoration) {
				count++;
			}
		}
		return count;
	}

	public int getVillagerCount() {
		return this.lastPopulation;
	}

	public boolean isLastDaySustained() {
		return this.lastDaySustained;
	}

	public long getLastTotalDemand() {
		return this.lastTotalDemand;
	}

	public long getLastTotalPaid() {
		return this.lastTotalPaid;
	}

	private long consumeForUpkeep(long amount) {
		try (Transaction tx = Transaction.openOuter()) {
			long extracted = this.realStorage.extract(amount, tx);
			tx.commit();
			return extracted;
		}
	}

	/** Allows the Creative Village Core to top itself off right before the daily checkpoint runs. */
	protected void beforeDailyCheckpoint() {
	}

	/** For {@code CreativeVillageCoreBlockEntity}: force the tank to full, bypassing the transactional API. */
	protected final void fillToCapacity() {
		this.realStorage.amount = CORE_CAPACITY;
		this.markDirty();
	}

	public static void tick(World world, BlockPos pos, BlockState state, VillageCoreBlockEntity entity) {
		if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		entity.onTick(serverWorld);
	}

	private void onTick(ServerWorld world) {
		long time = world.getTime();
		if (time - this.lastDailyCheckTime < DAY_LENGTH_TICKS) {
			return;
		}
		this.lastDailyCheckTime = time;
		this.beforeDailyCheckpoint();
		this.runDailyCheckpoint(world);
	}

	private void runDailyCheckpoint(ServerWorld world) {
		VillageBiomeStyle style = this.resolveStyle(world);
		List<VillagerEntity> villagers = this.findLoadedVillagers(world);
		this.lastPopulation = villagers.size();

		long totalDemand = 0L;
		for (VillageHouseRecord house : this.structures) {
			if (house.decoration) {
				continue;
			}
			int residents = this.countResidents(house, villagers);
			totalDemand += house.baseDraw + house.perVillagerDraw * residents;
		}

		long totalPaid = this.consumeForUpkeep(totalDemand);
		this.lastTotalDemand = totalDemand;
		this.lastTotalPaid = totalPaid;
		this.lastDaySustained = totalPaid >= totalDemand;

		if (this.lastDaySustained) {
			if (this.realStorage.amount >= HOUSE_GROWTH_THRESHOLD) {
				boolean placeDecoration = this.growthEventCount > 0 && (this.growthEventCount + 1) % DECORATION_EVERY_N_GROWTHS == 0;
				if (this.tryGrowVillage(world, style, placeDecoration)) {
					this.growthEventCount++;
				}
			}
		} else {
			long unpaidAmount = totalDemand - totalPaid;
			int deficitCount = (int) Math.floor(unpaidAmount / (double) DEFAULT_HOUSE_PER_VILLAGER_DRAW_PER_DAY);
			deficitCount = Math.max(0, Math.min(deficitCount, this.lastPopulation));
			this.rollPillagerConversion(world, villagers, deficitCount);
		}

		// The Core is not a long-term battery: whatever's left at the end of the day is burned.
		this.realStorage.amount = 0;
		this.markDirty();
	}

	private VillageBiomeStyle resolveStyle(ServerWorld world) {
		if (this.style == null) {
			RegistryKey<Biome> biomeKey = world.getBiome(this.pos).getKey().orElse(BiomeKeys.PLAINS);
			this.style = VillageBiomeStyle.resolve(biomeKey);
			this.markDirty();
		}
		return this.style;
	}

	private List<VillagerEntity> findLoadedVillagers(ServerWorld world) {
		Box searchBox = new Box(this.pos).expand(MAX_SITE_SEARCH_RADIUS + 48);
		return world.getEntitiesByType(TypeFilter.instanceOf(VillagerEntity.class), searchBox, villager -> true);
	}

	private int countResidents(VillageHouseRecord house, List<VillagerEntity> villagers) {
		BlockBox footprint = house.footprint();
		int count = 0;
		for (VillagerEntity villager : villagers) {
			Optional<net.minecraft.util.math.GlobalPos> home = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME);
			if (home.isPresent() && footprint.contains(home.get().pos())) {
				count++;
			}
		}
		return count;
	}

	private boolean tryGrowVillage(ServerWorld world, VillageBiomeStyle style, boolean placeDecoration) {
		Random random = world.getRandom();
		int missingTemplateCount = 0;
		int unevenTerrainCount = 0;
		int overlapCount = 0;
		for (int attempt = 0; attempt < SITE_SEARCH_ATTEMPTS; attempt++) {
			Identifier templateId = placeDecoration ? style.randomDecoration(random) : style.randomBuilding(random);
			Optional<StructureTemplate> templateOpt = world.getStructureTemplateManager().getTemplate(templateId);
			if (templateOpt.isEmpty()) {
				missingTemplateCount++;
				continue;
			}
			StructureTemplate template = templateOpt.get();
			BlockRotation rotation = ROTATIONS[random.nextInt(ROTATIONS.length)];
			Vec3i rawSize = template.getSize();
			boolean swapAxes = rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90;
			int sizeX = swapAxes ? rawSize.getZ() : rawSize.getX();
			int sizeZ = swapAxes ? rawSize.getX() : rawSize.getZ();
			int sizeY = rawSize.getY();

			int offsetX = random.nextInt(MAX_SITE_SEARCH_RADIUS * 2 + 1) - MAX_SITE_SEARCH_RADIUS;
			int offsetZ = random.nextInt(MAX_SITE_SEARCH_RADIUS * 2 + 1) - MAX_SITE_SEARCH_RADIUS;
			BlockPos columnOrigin = this.pos.add(offsetX, 0, offsetZ);

			BlockPos origin = findFlatClearOrigin(world, columnOrigin, sizeX, sizeY, sizeZ);
			if (origin == null) {
				unevenTerrainCount++;
				continue;
			}

			VillageHouseRecord candidate = new VillageHouseRecord(origin, templateId, rotation, sizeX, sizeY, sizeZ,
					placeDecoration ? 0L : DEFAULT_HOUSE_BASE_DRAW_PER_DAY,
					placeDecoration ? 0L : DEFAULT_HOUSE_PER_VILLAGER_DRAW_PER_DAY,
					placeDecoration);
			if (this.overlapsExisting(candidate)) {
				overlapCount++;
				continue;
			}

			StructurePlacementData placementData = new StructurePlacementData()
					.setRotation(rotation)
					.setMirror(BlockMirror.NONE)
					.setIgnoreEntities(false)
					.setUpdateNeighbors(true);
			template.place(world, origin, BlockPos.ORIGIN, placementData, random, Block.NOTIFY_ALL | Block.FORCE_STATE);
			stripJigsawBlocks(world, candidate);

			this.structures.add(candidate);
			if (!placeDecoration) {
				this.spawnStarterVillager(world, origin, sizeX, sizeZ);
			}
			this.markDirty();
			ResonantCaves.LOGGER.info("Village Core at {}: placed {} after {} attempt(s)", this.pos, templateId, attempt + 1);
			return true;
		}
		ResonantCaves.LOGGER.info(
				"Village Core at {}: growth failed after {} attempts (missing template: {}, uneven/unclear terrain: {}, overlap: {})",
				this.pos, SITE_SEARCH_ATTEMPTS, missingTemplateCount, unevenTerrainCount, overlapCount);
		return false;
	}

	/**
	 * Vanilla's house/decoration templates embed {@code minecraft:jigsaw} connector blocks (normally
	 * resolved by the real jigsaw village-assembly algorithm, which this mod deliberately doesn't
	 * run — see "Current state" in CLAUDE.md; real road generation off the same connectors was
	 * considered and explicitly deferred, since it amounts to reimplementing a meaningful chunk of
	 * that same algorithm). Each jigsaw block records its own {@code final_state} — vanilla's own
	 * "what this becomes if nothing connects here" — so resolving to that instead of always air is
	 * the cheap, more faithful fix. {@code final_state} is usually a bare block ID (almost always
	 * {@code minecraft:air}); the rare ones with bracketed block-state properties have those
	 * properties dropped in favor of the block's default state, rather than depending on the
	 * command-block-state parsing API for a cosmetic-only fallback.
	 */
	private static void stripJigsawBlocks(ServerWorld world, VillageHouseRecord record) {
		BlockPos origin = record.origin;
		for (BlockPos pos : BlockPos.iterate(origin, origin.add(record.sizeX - 1, record.sizeY - 1, record.sizeZ - 1))) {
			if (!world.getBlockState(pos).isOf(Blocks.JIGSAW)) {
				continue;
			}
			BlockState replacement = Blocks.AIR.getDefaultState();
			if (world.getBlockEntity(pos) instanceof JigsawBlockEntity jigsaw) {
				replacement = resolveFinalState(jigsaw.getFinalState());
			}
			world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
		}
	}

	private static BlockState resolveFinalState(String finalState) {
		int bracket = finalState.indexOf('[');
		String idPart = (bracket >= 0 ? finalState.substring(0, bracket) : finalState).trim();
		Identifier id = Identifier.tryParse(idPart);
		return id == null ? Blocks.AIR.getDefaultState() : Registries.BLOCK.get(id).getDefaultState();
	}

	/** Strict flatness (every column's heightmap must agree) plus a fully-air footprint volume. */
	@Nullable
	private static BlockPos findFlatClearOrigin(ServerWorld world, BlockPos columnOrigin, int sizeX, int sizeY, int sizeZ) {
		int firstTopY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, columnOrigin.getX(), columnOrigin.getZ());
		for (int dx = 0; dx < sizeX; dx++) {
			for (int dz = 0; dz < sizeZ; dz++) {
				int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, columnOrigin.getX() + dx, columnOrigin.getZ() + dz);
				if (topY != firstTopY) {
					return null;
				}
			}
		}
		BlockPos origin = new BlockPos(columnOrigin.getX(), firstTopY, columnOrigin.getZ());
		for (BlockPos checkPos : BlockPos.iterate(origin, origin.add(sizeX - 1, sizeY - 1, sizeZ - 1))) {
			if (!world.getBlockState(checkPos).isAir()) {
				return null;
			}
		}
		return origin;
	}

	private boolean overlapsExisting(VillageHouseRecord candidate) {
		BlockBox candidateBox = candidate.expandedFootprint(OVERLAP_MARGIN);
		BlockBox coreBox = new BlockBox(this.pos.getX(), this.pos.getY(), this.pos.getZ(),
				this.pos.getX(), this.pos.getY(), this.pos.getZ()).expand(OVERLAP_MARGIN);
		if (candidateBox.intersects(coreBox)) {
			return true;
		}
		for (VillageHouseRecord existing : this.structures) {
			if (candidateBox.intersects(existing.expandedFootprint(OVERLAP_MARGIN))) {
				return true;
			}
		}
		return false;
	}

	private void spawnStarterVillager(ServerWorld world, BlockPos origin, int sizeX, int sizeZ) {
		VillagerEntity villager = EntityType.VILLAGER.create(world);
		if (villager == null) {
			return;
		}
		Random random = world.getRandom();
		for (int attempt = 0; attempt < STARTER_VILLAGER_PLACEMENT_ATTEMPTS; attempt++) {
			double x = origin.getX() + random.nextDouble() * sizeX;
			double z = origin.getZ() + random.nextDouble() * sizeZ;
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z);
			villager.refreshPositionAndAngles(x, y, z, random.nextFloat() * 360.0F, 0.0F);
			if (world.isSpaceEmpty(villager, villager.getBoundingBox())) {
				villager.initialize(world, world.getLocalDifficulty(villager.getBlockPos()), SpawnReason.NATURAL, null);
				world.spawnEntity(villager);
				return;
			}
		}
	}

	private void rollPillagerConversion(ServerWorld world, List<VillagerEntity> villagers, int deficitCount) {
		if (deficitCount <= 0 || villagers.isEmpty()) {
			return;
		}
		List<VillagerEntity> pool = new ArrayList<>(villagers);
		Random random = world.getRandom();
		int rollCount = Math.min(deficitCount, pool.size());
		for (int i = 0; i < rollCount; i++) {
			VillagerEntity villager = pool.remove(random.nextInt(pool.size()));
			if (random.nextFloat() < PILLAGER_CONVERSION_CHANCE) {
				this.convertToPillager(world, villager);
			}
		}
	}

	private void convertToPillager(ServerWorld world, VillagerEntity villager) {
		PillagerEntity pillager = EntityType.PILLAGER.create(world);
		if (pillager == null) {
			return;
		}
		Random random = world.getRandom();
		boolean placed = false;
		for (int attempt = 0; attempt < STARTER_VILLAGER_PLACEMENT_ATTEMPTS; attempt++) {
			double x = villager.getX() + (random.nextDouble() * 2 - 1) * 2.0;
			double z = villager.getZ() + (random.nextDouble() * 2 - 1) * 2.0;
			pillager.refreshPositionAndAngles(x, villager.getY(), z, random.nextFloat() * 360.0F, 0.0F);
			if (world.isSpaceEmpty(pillager, pillager.getBoundingBox())) {
				placed = true;
				break;
			}
		}
		if (!placed) {
			return;
		}
		pillager.initialize(world, world.getLocalDifficulty(pillager.getBlockPos()), SpawnReason.NATURAL, null);
		world.spawnEntity(pillager);

		villager.releaseTicketFor(MemoryModuleType.HOME);
		villager.releaseTicketFor(MemoryModuleType.JOB_SITE);
		villager.releaseTicketFor(MemoryModuleType.POTENTIAL_JOB_SITE);
		villager.releaseTicketFor(MemoryModuleType.MEETING_POINT);
		villager.discard();
	}

	/** Opens this Core's vital-statistics GUI for the player. */
	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ModNetworking.trackOpenScreen(player, this.pos);
		ServerPlayNetworking.send(player, new OpenVillageCoreScreenPayload(
				this.pos, this.getStoredEnergyForDisplay(), this.getHouseCount(), this.getVillagerCount(), this.lastDaySustained));
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLong("StoredEnergy", this.realStorage.amount);
		if (this.style != null) {
			nbt.putString("Style", this.style.name());
		}
		nbt.putLong("LastDailyCheckTime", this.lastDailyCheckTime);
		nbt.putInt("GrowthEventCount", this.growthEventCount);
		nbt.putInt("LastPopulation", this.lastPopulation);
		nbt.putLong("LastTotalDemand", this.lastTotalDemand);
		nbt.putLong("LastTotalPaid", this.lastTotalPaid);
		nbt.putBoolean("LastDaySustained", this.lastDaySustained);

		NbtList structureList = new NbtList();
		for (VillageHouseRecord structure : this.structures) {
			structureList.add(structure.writeNbt());
		}
		nbt.put("Structures", structureList);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.realStorage.amount = nbt.getLong("StoredEnergy");
		if (nbt.contains("Style")) {
			try {
				this.style = VillageBiomeStyle.valueOf(nbt.getString("Style"));
			} catch (IllegalArgumentException ignored) {
				this.style = null;
			}
		}
		this.lastDailyCheckTime = nbt.getLong("LastDailyCheckTime");
		this.growthEventCount = nbt.getInt("GrowthEventCount");
		this.lastPopulation = nbt.getInt("LastPopulation");
		this.lastTotalDemand = nbt.getLong("LastTotalDemand");
		this.lastTotalPaid = nbt.getLong("LastTotalPaid");
		this.lastDaySustained = !nbt.contains("LastDaySustained") || nbt.getBoolean("LastDaySustained");

		this.structures.clear();
		NbtList structureList = nbt.getList("Structures", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < structureList.size(); i++) {
			this.structures.add(VillageHouseRecord.readNbt(structureList.getCompound(i)));
		}
	}
}
