package com.namith.surfacesurvival;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;

/**
 * Feature 1 — Unbreakable stone &amp; deepslate.
 *
 * <p>Cancels the breaking of {@code minecraft:stone} and {@code minecraft:deepslate} only.
 * Everything else stays breakable, including the stone variants (granite, diorite, andesite,
 * tuff), dirt, gravel, and all ores. Creative-mode players are exempt so the world remains
 * editable, mirroring how bedrock behaves.
 */
public final class UnbreakableBlocks {
	private UnbreakableBlocks() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (player.isCreative()) {
				return true; // allow creative players to edit the world
			}
			if (state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE)) {
				return false; // veto the break: the block stays put
			}
			return true;
		});
	}
}
