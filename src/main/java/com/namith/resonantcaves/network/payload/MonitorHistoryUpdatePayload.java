package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/** Server &rarr; client: appends one new throughput sample to an already-open monitor screen. */
public record MonitorHistoryUpdatePayload(BlockPos pos, long newSample, @Nullable Direction flowDirection)
		implements CustomPayload {
	public static final CustomPayload.Id<MonitorHistoryUpdatePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "monitor_history_update"));

	public static final PacketCodec<RegistryByteBuf, MonitorHistoryUpdatePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, MonitorHistoryUpdatePayload::pos,
			PacketCodecs.VAR_LONG, MonitorHistoryUpdatePayload::newSample,
			PayloadCodecs.NULLABLE_DIRECTION, MonitorHistoryUpdatePayload::flowDirection,
			MonitorHistoryUpdatePayload::new);

	@Override
	public CustomPayload.Id<MonitorHistoryUpdatePayload> getId() {
		return ID;
	}
}
