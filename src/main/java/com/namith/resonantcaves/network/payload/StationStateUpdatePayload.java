package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server &rarr; client: refreshes the live stored-energy readout on an already-open station
 * screen. {@code ratePerTick} is the net change in stored energy (positive = charging, negative =
 * discharging) since the previous push, one second ago — not a windowed average.
 */
public record StationStateUpdatePayload(BlockPos pos, long storedEnergy, long ratePerTick) implements CustomPayload {
	public static final CustomPayload.Id<StationStateUpdatePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "station_state_update"));

	public static final PacketCodec<RegistryByteBuf, StationStateUpdatePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, StationStateUpdatePayload::pos,
			PacketCodecs.VAR_LONG, StationStateUpdatePayload::storedEnergy,
			PacketCodecs.VAR_LONG, StationStateUpdatePayload::ratePerTick,
			StationStateUpdatePayload::new);

	@Override
	public CustomPayload.Id<StationStateUpdatePayload> getId() {
		return ID;
	}
}
