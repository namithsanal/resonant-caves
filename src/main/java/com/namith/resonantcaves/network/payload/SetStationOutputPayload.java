package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client &rarr; server: the player confirmed a new precise target-output value in a station screen. */
public record SetStationOutputPayload(BlockPos pos, long newTargetOutput) implements CustomPayload {
	public static final CustomPayload.Id<SetStationOutputPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "set_station_output"));

	public static final PacketCodec<RegistryByteBuf, SetStationOutputPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, SetStationOutputPayload::pos,
			PacketCodecs.VAR_LONG, SetStationOutputPayload::newTargetOutput,
			SetStationOutputPayload::new);

	@Override
	public CustomPayload.Id<SetStationOutputPayload> getId() {
		return ID;
	}
}
