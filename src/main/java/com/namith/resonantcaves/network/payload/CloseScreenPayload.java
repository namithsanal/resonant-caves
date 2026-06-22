package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client &rarr; server: a monitor or station screen was closed, so the server can stop pushing periodic updates. */
public record CloseScreenPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<CloseScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "close_screen"));

	public static final PacketCodec<RegistryByteBuf, CloseScreenPayload> CODEC =
			BlockPos.PACKET_CODEC.xmap(CloseScreenPayload::new, CloseScreenPayload::pos).cast();

	@Override
	public CustomPayload.Id<CloseScreenPayload> getId() {
		return ID;
	}
}
