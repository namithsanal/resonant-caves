package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server &rarr; client: periodic live-data push for an open Village Core screen. */
public record VillageCoreStateUpdatePayload(
		BlockPos pos,
		int villagerCount,
		int houseCount,
		long storedEmeralds)
		implements CustomPayload {

	public static final CustomPayload.Id<VillageCoreStateUpdatePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "village_core_state_update"));

	public static final PacketCodec<RegistryByteBuf, VillageCoreStateUpdatePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, VillageCoreStateUpdatePayload::pos,
			PacketCodecs.VAR_INT, VillageCoreStateUpdatePayload::villagerCount,
			PacketCodecs.VAR_INT, VillageCoreStateUpdatePayload::houseCount,
			PacketCodecs.VAR_LONG, VillageCoreStateUpdatePayload::storedEmeralds,
			VillageCoreStateUpdatePayload::new);

	@Override
	public CustomPayload.Id<VillageCoreStateUpdatePayload> getId() {
		return ID;
	}
}
