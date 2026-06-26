package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server &rarr; client: tell the client to open the Village Core GUI. */
public record OpenVillageCoreScreenPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<OpenVillageCoreScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "open_village_core_screen"));

	public static final PacketCodec<RegistryByteBuf, OpenVillageCoreScreenPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenVillageCoreScreenPayload::pos,
			OpenVillageCoreScreenPayload::new);

	@Override
	public CustomPayload.Id<OpenVillageCoreScreenPayload> getId() {
		return ID;
	}
}
