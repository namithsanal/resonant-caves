package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server &rarr; client: refreshes the live readout on an already-open Village Core screen. */
public record VillageCoreStateUpdatePayload(
		BlockPos pos, long storedEnergy, int houseCount, int villagerCount, boolean sustained)
		implements CustomPayload {
	public static final CustomPayload.Id<VillageCoreStateUpdatePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "village_core_state_update"));

	public static final PacketCodec<RegistryByteBuf, VillageCoreStateUpdatePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, VillageCoreStateUpdatePayload::pos,
			PacketCodecs.VAR_LONG, VillageCoreStateUpdatePayload::storedEnergy,
			PacketCodecs.VAR_INT, VillageCoreStateUpdatePayload::houseCount,
			PacketCodecs.VAR_INT, VillageCoreStateUpdatePayload::villagerCount,
			PacketCodecs.BOOL, VillageCoreStateUpdatePayload::sustained,
			VillageCoreStateUpdatePayload::new);

	@Override
	public CustomPayload.Id<VillageCoreStateUpdatePayload> getId() {
		return ID;
	}
}
