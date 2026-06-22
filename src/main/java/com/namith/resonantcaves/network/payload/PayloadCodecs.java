package com.namith.resonantcaves.network.payload;

import com.namith.resonantcaves.block.EnergyTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/** Shared {@link PacketCodec}s for fields reused across this mod's GUI payloads. */
final class PayloadCodecs {
	private PayloadCodecs() {
	}

	static final PacketCodec<ByteBuf, EnergyTier> ENERGY_TIER = PacketCodecs.VAR_INT.xmap(
			ordinal -> EnergyTier.values()[ordinal], EnergyTier::ordinal);

	/**
	 * 0 encodes "no tier" (the Creative Station, which is deliberately tierless/untiered — it's a
	 * debug source, not part of the tier economy); 1+ is {@code ordinal + 1}.
	 */
	static final PacketCodec<ByteBuf, @Nullable EnergyTier> NULLABLE_ENERGY_TIER = PacketCodecs.VAR_INT.xmap(
			encoded -> encoded == 0 ? null : EnergyTier.values()[encoded - 1],
			tier -> tier == null ? 0 : tier.ordinal() + 1);

	/** 0 encodes "no direction" (a relay cable with no direct source/sink edge); 1+ is {@code ordinal + 1}. */
	static final PacketCodec<ByteBuf, @Nullable Direction> NULLABLE_DIRECTION = PacketCodecs.VAR_INT.xmap(
			encoded -> encoded == 0 ? null : Direction.values()[encoded - 1],
			direction -> direction == null ? 0 : direction.ordinal() + 1);

	static final PacketCodec<ByteBuf, long[]> LONG_ARRAY = PacketCodec.of(
			(long[] value, ByteBuf buf) -> {
				buf.writeInt(value.length);
				for (long sample : value) {
					buf.writeLong(sample);
				}
			},
			(ByteBuf buf) -> {
				int length = buf.readInt();
				long[] result = new long[length];
				for (int i = 0; i < length; i++) {
					result[i] = buf.readLong();
				}
				return result;
			});
}
