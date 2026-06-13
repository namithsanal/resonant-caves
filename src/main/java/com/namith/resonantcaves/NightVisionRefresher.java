package com.namith.resonantcaves;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

/** Feature 7 (part 2) — re-applies low-amplitude Night Vision to Resonant Helmet wearers so it never flickers off. */
public final class NightVisionRefresher {
	private NightVisionRefresher() {
	}

	private static final int REFRESH_INTERVAL_TICKS = 100; // 5s
	private static final int EFFECT_DURATION_TICKS = 220;  // > refresh interval, so it never expires between refreshes

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % REFRESH_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONANT_HELMET)) {
					player.addStatusEffect(new StatusEffectInstance(
							StatusEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0, true, false, false));
				}
			}
		});
	}
}
