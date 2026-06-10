package com.namith.surfacesurvival;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * Feature 1 — Unbreakable stone &amp; deepslate.
 *
 * <p>{@code minecraft:stone} and {@code minecraft:deepslate} cannot be mined. Everything else stays
 * breakable, including the stone variants (granite, diorite, andesite, tuff), dirt, gravel, and all
 * ores. Creative-mode players are exempt so the world stays editable, mirroring how bedrock behaves.
 *
 * <p>This works in two layers that share {@link #isUnbreakable(BlockState)}:
 * <ul>
 *   <li>{@code AbstractBlockStateMixin} forces the mining speed to zero, so survival players see no
 *       cracking animation and make no progress — the bedrock feel.</li>
 *   <li>The {@link PlayerBlockBreakEvents#BEFORE} handler below is an authoritative server-side veto:
 *       even if a break is somehow initiated (e.g. a modified client), the block is never removed.</li>
 * </ul>
 */
public final class UnbreakableBlocks {
	private UnbreakableBlocks() {
	}

	/** The "host rock" that cannot be mined: stone and deepslate only. */
	public static boolean isUnbreakable(BlockState state) {
		return state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE);
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (player.isCreative()) {
				return true; // allow creative players to edit the world
			}
			return !isUnbreakable(state); // false => veto the break: the block stays put
		});
	}
}
