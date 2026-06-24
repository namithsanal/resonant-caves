package com.namith.resonantcaves.item;

import java.util.Optional;

import com.namith.resonantcaves.block.WandPowerSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Shared hitscan-attack logic for the Resonant Wand, used identically by the player's {@code use()},
 * the skeleton's {@code WandAttackGoal}, and the snow golem's {@code GolemWandSeekGoal} — only the
 * power-dependency parameters differ per wielder (see each call site).
 */
public final class WandAttacks {
	public static final double MAX_RANGE = 64.0;

	/** Skeleton's lenient, no-line-of-sight station search — it has a fallback, so leniency here just means more frequent full-power shots. */
	public static final double SKELETON_STATION_SEARCH_RADIUS = 16.0;
	/** Player/golem's wider search, but gated by line of sight (see {@link #hasLineOfSight}) since neither has a fallback. */
	public static final double EXTENDED_STATION_SEARCH_RADIUS = 32.0;

	private static final double DAMAGE_MIN = 2.0;
	private static final double DAMAGE_MODE = 6.67;
	private static final double DAMAGE_MAX = 12.0;

	private static final long ENERGY_BASE_COST = 20L;
	private static final long ENERGY_PER_BLOCK_COST = 5L;

	private static final double FALLBACK_RANGE = 16.0;
	private static final double FALLBACK_DAMAGE_MIN = 1.0;
	private static final double FALLBACK_DAMAGE_MODE = 2.5;
	private static final double FALLBACK_DAMAGE_MAX = 4.0;

	private WandAttacks() {
	}

	/** @param fired whether an attack actually happened (false if no power and no fallback applied); {@code usedStation} whether a station was drawn from. */
	public record AttackResult(boolean fired, boolean usedStation) {
	}

	/**
	 * Performs one hitscan attack from {@code wielder}'s eye position in its current look direction.
	 * Looks for the nearest {@link WandPowerSource} within {@code stationSearchRadius} blocks (optionally
	 * requiring a clear line of sight to it); if none is found and {@code allowFallback} is true, fires a
	 * short-range, low-damage, energy-free attack instead; if {@code allowFallback} is false, does nothing.
	 */
	public static AttackResult fire(LivingEntity wielder, boolean allowFallback, double stationSearchRadius,
			boolean requireLineOfSightToStation) {
		World world = wielder.getWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return new AttackResult(false, false);
		}

		BlockPos stationPos = findNearestStation(wielder, serverWorld, stationSearchRadius, requireLineOfSightToStation);

		double range;
		double damageMin;
		double damageMode;
		double damageMax;
		if (stationPos != null) {
			range = MAX_RANGE;
			damageMin = DAMAGE_MIN;
			damageMode = DAMAGE_MODE;
			damageMax = DAMAGE_MAX;
		} else if (allowFallback) {
			range = FALLBACK_RANGE;
			damageMin = FALLBACK_DAMAGE_MIN;
			damageMode = FALLBACK_DAMAGE_MODE;
			damageMax = FALLBACK_DAMAGE_MAX;
		} else {
			return new AttackResult(false, false);
		}

		Vec3d eyePos = wielder.getEyePos();
		HitResult hit = ProjectileUtil.getCollision(wielder,
				entity -> entity != wielder && entity.isAlive() && !entity.isSpectator(), range);
		double hitDistance = eyePos.distanceTo(hit.getPos());

		boolean usedStation = false;
		if (stationPos != null) {
			long cost = ENERGY_BASE_COST + Math.round(ENERGY_PER_BLOCK_COST * hitDistance);
			if (!(serverWorld.getBlockEntity(stationPos) instanceof WandPowerSource source)
					|| source.drawEnergyForWand(cost) < cost) {
				return new AttackResult(false, false);
			}
			usedStation = true;
		}

		if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
			damage(target, wielder, rollDamage(damageMin, damageMode, damageMax, wielder.getRandom()));
		}

		spawnParticleLine(serverWorld, eyePos, hit.getPos());
		if (usedStation) {
			Vec3d stationCenter = Vec3d.ofCenter(stationPos);
			// The power feed is a beam too: anything standing between the station and the wielder
			// gets zapped, same as anything standing between the wielder and its intended target.
			damageNearestAlongSegment(serverWorld, wielder, stationCenter, eyePos, damageMin, damageMode, damageMax);
			spawnParticleLine(serverWorld, stationCenter, eyePos);
		}

		return new AttackResult(true, usedStation);
	}

	private static void damageNearestAlongSegment(ServerWorld world, LivingEntity wielder, Vec3d from, Vec3d to,
			double damageMin, double damageMode, double damageMax) {
		Box box = new Box(from, to).expand(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityCollision(world, wielder, from, to, box,
				entity -> entity != wielder && entity.isAlive() && !entity.isSpectator());
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			damage(target, wielder, rollDamage(damageMin, damageMode, damageMax, wielder.getRandom()));
		}
	}

	private static void damage(LivingEntity target, LivingEntity wielder, double amount) {
		DamageSource damageSource = wielder instanceof PlayerEntity player
				? wielder.getDamageSources().playerAttack(player)
				: wielder.getDamageSources().mobAttack(wielder);
		target.damage(damageSource, (float) amount);
	}

	private static BlockPos findNearestStation(LivingEntity wielder, ServerWorld world, double radius, boolean requireLineOfSight) {
		int r = (int) Math.ceil(radius);
		Optional<BlockPos> found = BlockPos.findClosest(wielder.getBlockPos(), r, r, pos ->
				world.getBlockEntity(pos) instanceof WandPowerSource
						&& (!requireLineOfSight || hasLineOfSight(wielder, pos, world)));
		return found.orElse(null);
	}

	/**
	 * A clear line from the wielder's eyes to the station: nothing else blocks the way. A block
	 * raycast stops at the first solid surface, which is the station's own face whenever nothing
	 * else is in the way (it can never "miss" all the way to the target's center, since that point
	 * is inside solid geometry) — so a hit on the station itself counts as clear, and only a hit on
	 * some other block (or, in the rare edge case, an outright miss) counts as blocked.
	 */
	private static boolean hasLineOfSight(LivingEntity wielder, BlockPos stationPos, World world) {
		HitResult hit = world.raycast(new RaycastContext(wielder.getEyePos(), Vec3d.ofCenter(stationPos),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, wielder));
		if (hit.getType() == HitResult.Type.MISS) {
			return true;
		}
		return hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(stationPos);
	}

	/** Same triangular-distribution shape as {@code ResonantOreBlockEntity.rollGenerationRate}, parameterized. */
	private static double rollDamage(double min, double mode, double max, Random random) {
		double u = random.nextDouble();
		double split = (mode - min) / (max - min);
		return u < split
				? min + Math.sqrt(u * (max - min) * (mode - min))
				: max - Math.sqrt((1 - u) * (max - min) * (max - mode));
	}

	private static void spawnParticleLine(ServerWorld world, Vec3d from, Vec3d to) {
		double distance = from.distanceTo(to);
		int steps = Math.max(1, (int) Math.round(distance / 0.5));
		for (int i = 0; i <= steps; i++) {
			Vec3d pos = from.lerp(to, (double) i / steps);
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
