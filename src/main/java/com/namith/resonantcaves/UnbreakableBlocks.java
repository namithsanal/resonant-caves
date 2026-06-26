package com.namith.resonantcaves;

import com.namith.resonantcaves.block.CreativeVillageCoreBlock;
import com.namith.resonantcaves.block.ModBlocks;
import com.namith.resonantcaves.block.VillageCoreBlock;
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

	/**
	 * The "host rock" that cannot be mined: stone and deepslate, plus Resonant Ore (Feature 3),
	 * which is already bedrock-hard via its block settings — listed here too so this predicate
	 * remains the single source of truth for "unbreakable" blocks.
	 */
	public static boolean isUnbreakable(BlockState state) {
		return state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE)
				|| state.isOf(ModBlocks.RESONANT_ORE) || state.isOf(ModBlocks.DEEPSLATE_RESONANT_ORE)
				// Village Core is unbreakable once placed in survival; Creative Village Core stays breakable
				// so creative admins can still remove it.
				|| (state.getBlock() instanceof VillageCoreBlock && !(state.getBlock() instanceof CreativeVillageCoreBlock));
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
