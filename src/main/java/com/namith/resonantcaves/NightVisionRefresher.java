package com.namith.resonantcaves;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Feature 7 (part 2) — re-applies low-amplitude Night Vision to Resonant Crown wearers so it never flickers off. */
public final class NightVisionRefresher {
	private NightVisionRefresher() {
	}

	private static final int REFRESH_INTERVAL_TICKS = 100; // 5s
	// Vanilla pulses the night-vision screen fade whenever the remaining duration drops below 200
	// ticks (GameRenderer.getNightVisionStrength), causing a "flicker" near expiry. Keep the minimum
	// remaining duration (EFFECT_DURATION_TICKS - REFRESH_INTERVAL_TICKS) comfortably above that.
	private static final int EFFECT_DURATION_TICKS = 320;

	// Players currently known to be wearing the crown, so equip/unequip can be applied the instant
	// it happens rather than waiting for the next REFRESH_INTERVAL_TICKS tick.
	private static final Set<UUID> WEARING = new HashSet<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean refresh = server.getTicks() % REFRESH_INTERVAL_TICKS == 0;
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				boolean wearing = player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONANT_CROWN);
				boolean wasWearing = WEARING.contains(player.getUuid());
				if (wearing && (!wasWearing || refresh)) {
					player.addStatusEffect(new StatusEffectInstance(
							StatusEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0, true, false, false));
				} else if (!wearing && wasWearing) {
					player.removeStatusEffect(StatusEffects.NIGHT_VISION);
				}
				if (wearing) {
					WEARING.add(player.getUuid());
				} else {
					WEARING.remove(player.getUuid());
				}
			}
		});
	}
}
