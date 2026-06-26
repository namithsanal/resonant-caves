package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client &rarr; server: the Creative Village Core's "Simulate Day" button, triggers one {@code processMidnight()}. */
public record SimulateDayPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<SimulateDayPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "simulate_day"));

	public static final PacketCodec<RegistryByteBuf, SimulateDayPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, SimulateDayPayload::pos,
			SimulateDayPayload::new);

	@Override
	public CustomPayload.Id<SimulateDayPayload> getId() {
		return ID;
	}
}
