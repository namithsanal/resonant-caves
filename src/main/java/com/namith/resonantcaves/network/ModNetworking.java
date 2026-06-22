package com.namith.resonantcaves.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.namith.resonantcaves.block.entity.EnergyScreenSource;
import com.namith.resonantcaves.block.entity.MonitorBlockEntity;
import com.namith.resonantcaves.network.payload.CloseScreenPayload;
import com.namith.resonantcaves.network.payload.MonitorHistoryUpdatePayload;
import com.namith.resonantcaves.network.payload.OpenMonitorScreenPayload;
import com.namith.resonantcaves.network.payload.OpenStationScreenPayload;
import com.namith.resonantcaves.network.payload.SetStationOutputPayload;
import com.namith.resonantcaves.network.payload.StationStateUpdatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Registers this mod's GUI payloads (no {@code ScreenHandler}/inventory is used anywhere — these
 * screens have no item slots, so the server just tells the client which {@code Screen} to open
 * and keeps pushing small periodic updates while it stays open) and tracks, per player, which
 * monitor/station block is currently showing on their screen.
 */
public final class ModNetworking {
	private static final int PUSH_INTERVAL_TICKS = 20;

	private static final Map<UUID, BlockPos> OPEN_SCREENS = new ConcurrentHashMap<>();
	/** Each open station screen's stored-energy reading as of the previous push, to derive a rate of change. */
	private static final Map<UUID, Long> LAST_STORED_ENERGY = new ConcurrentHashMap<>();
	private static int tickCounter;

	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(OpenMonitorScreenPayload.ID, OpenMonitorScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MonitorHistoryUpdatePayload.ID, MonitorHistoryUpdatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenStationScreenPayload.ID, OpenStationScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StationStateUpdatePayload.ID, StationStateUpdatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SetStationOutputPayload.ID, SetStationOutputPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CloseScreenPayload.ID, CloseScreenPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SetStationOutputPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof EnergyScreenSource source) {
						source.setTargetOutput(payload.newTargetOutput());
					}
				}));

		ServerPlayNetworking.registerGlobalReceiver(CloseScreenPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					UUID uuid = context.player().getUuid();
					OPEN_SCREENS.remove(uuid);
					LAST_STORED_ENERGY.remove(uuid);
				}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.getPlayer().getUuid();
			OPEN_SCREENS.remove(uuid);
			LAST_STORED_ENERGY.remove(uuid);
		});
		ServerTickEvents.END_SERVER_TICK.register(ModNetworking::pushOpenScreenUpdates);
	}

	/** Called when a player's client opens a monitor/station screen, so periodic updates start flowing. */
	public static void trackOpenScreen(ServerPlayerEntity player, BlockPos pos) {
		OPEN_SCREENS.put(player.getUuid(), pos);
	}

	private static void pushOpenScreenUpdates(MinecraftServer server) {
		tickCounter++;
		if (tickCounter < PUSH_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			BlockPos pos = OPEN_SCREENS.get(player.getUuid());
			if (pos == null) {
				continue;
			}
			if (player.getPos().squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0 * 64.0) {
				OPEN_SCREENS.remove(player.getUuid());
				LAST_STORED_ENERGY.remove(player.getUuid());
				continue;
			}

			BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
			if (blockEntity instanceof MonitorBlockEntity monitor) {
				ServerPlayNetworking.send(player, new MonitorHistoryUpdatePayload(
						pos, monitor.getLatestSample(), monitor.getLatestFlowDirection()));
			} else if (blockEntity instanceof EnergyScreenSource source) {
				long current = source.getStoredEnergyForDisplay();
				long previous = LAST_STORED_ENERGY.getOrDefault(player.getUuid(), current);
				long ratePerTick = (current - previous) / PUSH_INTERVAL_TICKS;
				LAST_STORED_ENERGY.put(player.getUuid(), current);
				ServerPlayNetworking.send(player, new StationStateUpdatePayload(pos, current, ratePerTick));
			} else {
				OPEN_SCREENS.remove(player.getUuid());
				LAST_STORED_ENERGY.remove(player.getUuid());
			}
		}
	}
}
