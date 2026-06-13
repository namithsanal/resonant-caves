package com.namith.resonantcaves;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Client entrypoint. Hosts the Resonant Helmet's "hostile mob radar" particle effect (Feature 7). */
public class ResonantCavesClient implements ClientModInitializer {
	private static final double RADIUS = 128.0;
	private static final double PULSE_PERIOD_TICKS = 60.0;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(ResonantCavesClient::onClientTick);
	}

	private static void onClientTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;
		if (player == null || world == null) {
			return;
		}
		if (!player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONANT_HELMET)) {
			return;
		}

		// "Breathing" pulse: slow sine wave in [0, 1] based on the world tick counter.
		double phase = (world.getTime() % (long) PULSE_PERIOD_TICKS) / PULSE_PERIOD_TICKS;
		double pulse = (Math.sin(phase * 2.0 * Math.PI) + 1.0) / 2.0;
		if (pulse < 0.15) {
			return; // "exhale" phase — no particles this tick
		}

		Box searchBox = player.getBoundingBox().expand(RADIUS);
		List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, searchBox, LivingEntity::isAlive);
		double bob = (pulse - 0.5) * 0.15;

		for (HostileEntity hostile : hostiles) {
			if (player.squaredDistanceTo(hostile) > RADIUS * RADIUS) {
				continue;
			}
			Vec3d center = hostile.getBoundingBox().getCenter();
			world.addParticle(ParticleTypes.GLOW, center.x, center.y + bob, center.z, 0.0, 0.0, 0.0);
		}
	}
}
