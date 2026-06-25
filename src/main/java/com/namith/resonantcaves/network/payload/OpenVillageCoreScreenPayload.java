package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server &rarr; client: opens a Village Core's vital-statistics screen. {@code capacity} and the
 * growth threshold are fixed constants the client already has ({@code VillageCoreBlockEntity})
 * and don't need to be sent over the wire.
 */
public record OpenVillageCoreScreenPayload(
		BlockPos pos, long storedEnergy, int houseCount, int villagerCount, boolean sustained)
		implements CustomPayload {
	public static final CustomPayload.Id<OpenVillageCoreScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "open_village_core_screen"));

	public static final PacketCodec<RegistryByteBuf, OpenVillageCoreScreenPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenVillageCoreScreenPayload::pos,
			PacketCodecs.VAR_LONG, OpenVillageCoreScreenPayload::storedEnergy,
			PacketCodecs.VAR_INT, OpenVillageCoreScreenPayload::houseCount,
			PacketCodecs.VAR_INT, OpenVillageCoreScreenPayload::villagerCount,
			PacketCodecs.BOOL, OpenVillageCoreScreenPayload::sustained,
			OpenVillageCoreScreenPayload::new);

	@Override
	public CustomPayload.Id<OpenVillageCoreScreenPayload> getId() {
		return ID;
	}
}
