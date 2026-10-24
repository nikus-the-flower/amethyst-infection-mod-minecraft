package com.amethystmod.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

/** Small helpers shared by weapon and artifact abilities. */
public class Fx {
	/** Living entities (other than the user and other players) that can be targeted by abilities. */
	public static Predicate<LivingEntity> hostileTo(LivingEntity user) {
		return e -> e != user && e.isAlive() && !(e instanceof PlayerEntity) && !e.isSpectator();
	}

	public static List<LivingEntity> around(ServerWorld world, Vec3d center, double radius, Predicate<LivingEntity> filter) {
		Box box = new Box(center.x - radius, center.y - radius, center.z - radius,
				center.x + radius, center.y + radius, center.z + radius);
		return world.getEntitiesByClass(LivingEntity.class, box.expand(8),
				e -> filter.test(e) && distanceSq(e.getBoundingBox(), center) <= radius * radius);
	}

	/** Squared distance from a point to the nearest point of a box (0 inside it). */
	public static double distanceSq(Box b, Vec3d p) {
		double dx = Math.max(0, Math.max(b.minX - p.x, p.x - b.maxX));
		double dy = Math.max(0, Math.max(b.minY - p.y, p.y - b.maxY));
		double dz = Math.max(0, Math.max(b.minZ - p.z, p.z - b.maxZ));
		return dx * dx + dy * dy + dz * dz;
	}

	/** Living entities inside a cone in front of the user. */
	public static List<LivingEntity> cone(ServerWorld world, LivingEntity user, double range, double minDot) {
		Vec3d eye = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		return around(world, eye, range, hostileTo(user)).stream().filter(e -> {
			Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
			double len = to.length();
			return len < 0.5 || to.multiply(1.0 / len).dotProduct(look) >= minDot;
		}).toList();
	}

	/** Damages without the usual knockback: the target keeps the velocity it had. */
	public static void hurtNoKnockback(LivingEntity target, net.minecraft.entity.damage.DamageSource source, float amount) {
		Vec3d before = target.getVelocity();
		target.damage(source, amount);
		target.setVelocity(before);
		target.velocityModified = true;
	}

	public static void push(Entity entity, Vec3d velocity) {
		entity.setVelocity(velocity);
		entity.velocityModified = true;
	}

	public static void particles(ServerWorld world, ParticleEffect type, Vec3d pos, int count, double spread, double speed) {
		world.spawnParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	/** A particle that flies along the given direction (count 0 makes the offset the direction). */
	public static void shoot(ServerWorld world, ParticleEffect type, Vec3d pos, Vec3d dir, double speed) {
		world.spawnParticles(type, pos.x, pos.y, pos.z, 0, dir.x, dir.y, dir.z, speed);
	}

	public static void sound(ServerWorld world, Entity at, SoundEvent sound, float volume, float pitch) {
		world.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
	}
}
