package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import com.namith.resonantcaves.block.EnergyTier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server &rarr; client: opens a station's output-control screen, seeded with its stored-energy
 * history for the graph. {@code creative} is true for the Creative Station, which hides the
 * stored-energy readout and graph (it has no real storage) in favour of an "unlimited" label.
 */
public record OpenStationScreenPayload(
		BlockPos pos, EnergyTier tier, boolean creative, long storedEnergy, long targetOutput, long[] history)
		implements CustomPayload {
	public static final CustomPayload.Id<OpenStationScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "open_station_screen"));

	public static final PacketCodec<RegistryByteBuf, OpenStationScreenPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenStationScreenPayload::pos,
			PayloadCodecs.ENERGY_TIER, OpenStationScreenPayload::tier,
			PacketCodecs.BOOL, OpenStationScreenPayload::creative,
			PacketCodecs.VAR_LONG, OpenStationScreenPayload::storedEnergy,
			PacketCodecs.VAR_LONG, OpenStationScreenPayload::targetOutput,
			PayloadCodecs.LONG_ARRAY, OpenStationScreenPayload::history,
			OpenStationScreenPayload::new);

	@Override
	public CustomPayload.Id<OpenStationScreenPayload> getId() {
		return ID;
	}
}
