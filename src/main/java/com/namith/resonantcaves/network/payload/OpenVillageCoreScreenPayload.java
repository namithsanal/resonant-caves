package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server &rarr; client: opens the Village Core stats + population-graph screen, seeded with the
 * current snapshot. Uses a manual codec because the field count exceeds PacketCodec.tuple's limit.
 */
public record OpenVillageCoreScreenPayload(
		BlockPos pos,
		int villagerCount,
		int houseCount,
		int piecesPlaced,
		long lifetimePillagers,
		long deathCount,
		long peakPopulation,
		long daysActive,
		long storedEmeralds,
		int[] populationHistory,
		boolean creative)
		implements CustomPayload {

	public static final CustomPayload.Id<OpenVillageCoreScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "open_village_core_screen"));

	public static final PacketCodec<RegistryByteBuf, OpenVillageCoreScreenPayload> CODEC = PacketCodec.of(
			(value, buf) -> {
				BlockPos.PACKET_CODEC.encode(buf, value.pos);
				buf.writeVarInt(value.villagerCount);
				buf.writeVarInt(value.houseCount);
				buf.writeVarInt(value.piecesPlaced);
				buf.writeVarLong(value.lifetimePillagers);
				buf.writeVarLong(value.deathCount);
				buf.writeVarLong(value.peakPopulation);
				buf.writeVarLong(value.daysActive);
				buf.writeVarLong(value.storedEmeralds);
				PayloadCodecs.INT_ARRAY.encode(buf, value.populationHistory);
				buf.writeBoolean(value.creative);
			},
			buf -> new OpenVillageCoreScreenPayload(
					BlockPos.PACKET_CODEC.decode(buf),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarLong(),
					buf.readVarLong(),
					buf.readVarLong(),
					buf.readVarLong(),
					buf.readVarLong(),
					PayloadCodecs.INT_ARRAY.decode(buf),
					buf.readBoolean()));

	@Override
	public CustomPayload.Id<OpenVillageCoreScreenPayload> getId() {
		return ID;
	}
}
