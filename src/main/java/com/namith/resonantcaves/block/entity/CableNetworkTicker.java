package com.namith.resonantcaves.block.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import com.namith.resonantcaves.block.EnergyTier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import team.reborn.energy.api.EnergyStorage;

/**
 * Adapted from Tech Reborn's {@code CableTickManager} (MIT-licensed,
 * {@code src/main/java/techreborn/blockentity/cable/}): once per game tick, the first cable in a
 * connected same-tier network to tick gathers the whole network via BFS, finds every directly
 * adjacent non-cable {@link EnergyStorage} (sources that {@code supportsExtraction}, sinks that
 * {@code supportsInsertion}), and moves energy from sources to sinks through it.
 *
 * <p>Unlike Tech Reborn's cables, ours hold no internal energy buffer — they're pure conduits —
 * so instead of pulling based on remaining buffer headroom, this simulates total sink demand
 * first, pulls exactly enough from sources to cover that demand after this tier's loss, and pushes
 * the post-loss amount into the sinks. The loss fraction itself ({@link EnergyTier#lossFraction})
 * is this mod's own addition — Tech Reborn's cables have a flat transfer-rate cap and no
 * distance/throughput-based loss at all.
 */
final class CableNetworkTicker {
	private static final int MAX_NETWORK_SIZE = 256;
	private static long tickCounter = 0;

	private CableNetworkTicker() {
	}

	public static void register() {
		ServerTickEvents.START_SERVER_TICK.register(server -> tickCounter++);
	}

	static void handleCableTick(CableBlockEntity startingCable) {
		if (startingCable.getNetworkTickStamp() == tickCounter) {
			return;
		}
		ServerWorld world = (ServerWorld) startingCable.getWorld();
		if (world == null) {
			return;
		}

		List<CableBlockEntity> network = gatherNetwork(world, startingCable);

		List<Target> sources = new ArrayList<>();
		List<Target> sinks = new ArrayList<>();
		for (CableBlockEntity cable : network) {
			for (Direction direction : Direction.values()) {
				BlockPos neighborPos = cable.getPos().offset(direction);
				if (world.getBlockEntity(neighborPos) instanceof CableBlockEntity) {
					// Cable-to-cable connectivity is handled by the BFS itself, not as a source/sink.
					continue;
				}
				EnergyStorage storage = EnergyStorage.SIDED.find(world, neighborPos, direction.getOpposite());
				if (storage == null) {
					continue;
				}
				if (storage.supportsExtraction()) {
					sources.add(new Target(direction, storage));
				}
				if (storage.supportsInsertion()) {
					sinks.add(new Target(direction, storage));
				}
			}
		}

		long delivered = 0;
		// The sink that actually received energy this tick is the network's one meaningful "flow
		// direction" — see setNetworkFlow's javadoc for why this is broadcast to every cable.
		Direction flowDirection = null;
		if (!sinks.isEmpty() && !sources.isEmpty()) {
			long requestedAmount = simulateTotalDemand(sinks);
			if (requestedAmount > 0) {
				double lossFraction = Math.min(0.999999, startingCable.getTier().lossFraction(requestedAmount, network.size()));
				long pullAmount = (long) Math.ceil(requestedAmount / (1.0 - lossFraction));
				long extracted = dispatchTransfer(sources, EnergyStorage::extract, pullAmount);
				if (extracted > 0) {
					delivered = (long) Math.floor(extracted * (1.0 - lossFraction));
					if (delivered > 0) {
						dispatchTransfer(sinks, EnergyStorage::insert, delivered);
						flowDirection = sinks.get(0).direction();
					}
				}
			}
		}

		for (CableBlockEntity cable : network) {
			cable.setNetworkFlow(delivered, flowDirection);
		}
	}

	/** Breadth-first search through directly-adjacent same-tier cables, capped at {@link #MAX_NETWORK_SIZE}. */
	private static List<CableBlockEntity> gatherNetwork(ServerWorld world, CableBlockEntity start) {
		List<CableBlockEntity> result = new ArrayList<>();
		Deque<CableBlockEntity> queue = new ArrayDeque<>();
		EnergyTier tier = start.getTier();

		start.setNetworkTickStamp(tickCounter);
		result.add(start);
		queue.add(start);

		while (!queue.isEmpty() && result.size() < MAX_NETWORK_SIZE) {
			CableBlockEntity current = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos neighborPos = current.getPos().offset(direction);
				if (!(world.getBlockEntity(neighborPos) instanceof CableBlockEntity neighbor)) {
					continue;
				}
				if (neighbor.getTier() != tier || neighbor.getNetworkTickStamp() == tickCounter) {
					continue;
				}
				neighbor.setNetworkTickStamp(tickCounter);
				result.add(neighbor);
				queue.add(neighbor);
				if (result.size() >= MAX_NETWORK_SIZE) {
					break;
				}
			}
		}
		return result;
	}

	private static long simulateTotalDemand(List<Target> sinks) {
		long total = 0;
		try (Transaction tx = Transaction.openOuter()) {
			for (Target sink : sinks) {
				total += sink.storage().insert(Long.MAX_VALUE, tx);
			}
			// Intentionally not committed — this is a simulation, so it aborts on close.
		}
		return total;
	}

	/**
	 * Fairly splits {@code maxAmount} across {@code targets} (shuffled, then handled smallest
	 * simulated result first so small recipients aren't starved by larger ones), mirroring Tech
	 * Reborn's {@code CableTickManager.dispatchTransfer}.
	 */
	private static long dispatchTransfer(List<Target> targets, TransferOperation operation, long maxAmount) {
		if (targets.isEmpty() || maxAmount <= 0) {
			return 0;
		}
		List<Target> shuffled = new ArrayList<>(targets);
		Collections.shuffle(shuffled);

		long[] simulated = new long[shuffled.size()];
		for (int i = 0; i < shuffled.size(); i++) {
			try (Transaction tx = Transaction.openOuter()) {
				simulated[i] = operation.transfer(shuffled.get(i).storage(), Long.MAX_VALUE, tx);
			}
		}
		Integer[] order = new Integer[shuffled.size()];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		Arrays.sort(order, Comparator.comparingLong(i -> simulated[i]));

		long transferred = 0;
		try (Transaction transaction = Transaction.openOuter()) {
			for (int i = 0; i < order.length; i++) {
				Target target = shuffled.get(order[i]);
				int remainingTargets = order.length - i;
				long remainingAmount = maxAmount - transferred;
				long perTargetCap = remainingAmount / remainingTargets;
				long localTransferred = operation.transfer(target.storage(), perTargetCap, transaction);
				transferred += localTransferred;
			}
			transaction.commit();
		}
		return transferred;
	}

	private interface TransferOperation {
		long transfer(EnergyStorage storage, long maxAmount, TransactionContext ctx);
	}

	private record Target(Direction direction, EnergyStorage storage) {
	}
}
