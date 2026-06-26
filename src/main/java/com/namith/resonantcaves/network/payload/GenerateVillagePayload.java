package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client &rarr; server: player pressed "Generate" in the Village Core GUI. */
public record GenerateVillagePayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<GenerateVillagePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "generate_village"));

	public static final PacketCodec<RegistryByteBuf, GenerateVillagePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, GenerateVillagePayload::pos,
			GenerateVillagePayload::new);

	@Override
	public CustomPayload.Id<GenerateVillagePayload> getId() {
		return ID;
	}
}
