package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.ResonantCaves;
import com.namith.resonantcaves.block.EnergyTier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/** Server &rarr; client: opens a monitor's throughput-graph screen, seeded with its full history buffer. */
public record OpenMonitorScreenPayload(BlockPos pos, EnergyTier tier, long[] history, @Nullable Direction flowDirection)
		implements CustomPayload {
	public static final CustomPayload.Id<OpenMonitorScreenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ResonantCaves.MOD_ID, "open_monitor_screen"));

	public static final PacketCodec<RegistryByteBuf, OpenMonitorScreenPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenMonitorScreenPayload::pos,
			PayloadCodecs.ENERGY_TIER, OpenMonitorScreenPayload::tier,
			PayloadCodecs.LONG_ARRAY, OpenMonitorScreenPayload::history,
			PayloadCodecs.NULLABLE_DIRECTION, OpenMonitorScreenPayload::flowDirection,
			OpenMonitorScreenPayload::new);

	@Override
	public CustomPayload.Id<OpenMonitorScreenPayload> getId() {
		return ID;
	}
}
