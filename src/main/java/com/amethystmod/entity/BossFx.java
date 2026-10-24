package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import com.amethystmod.item.Fx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

/** Shared visuals and area attacks of the bosses. */
public class BossFx {
	/** Everything an amethyst creature may hurt: players and non-amethyst living things. */
	public static Predicate<LivingEntity> foesOf(LivingEntity source) {
		return e -> e != source && e.isAlive() && !e.isSpectator() && !(e instanceof CrystalKin);
	}

	/**
	 * Living players (not spectators) within {@code range}; during the self-test this also returns the
	 * test dummies, so boss code paths can be exercised on a server with nobody online.
	 */
	public static List<net.minecraft.server.network.ServerPlayerEntity> playersNear(ServerWorld world, Vec3d center, double range) {
		List<net.minecraft.server.network.ServerPlayerEntity> list = new java.util.ArrayList<>(world.getPlayers(
				p -> p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(center) <= range * range));
		for (net.minecraft.server.network.ServerPlayerEntity dummy : com.amethystmod.SelfTest.DUMMIES) {
			if (dummy.getWorld() == world && dummy.squaredDistanceTo(center) <= range * range) {
				list.add(dummy);
			}
		}
		return list;
	}

	/** A rune circle on the floor that warns of an attack about to land there. */
	public static void marker(ServerWorld world, Vec3d pos, double radius, int ticks) {
		CrystalEffectEntity.spawn(world, ModEntities.RUNE_CIRCLE, pos.add(0, 0.05, 0), (float) radius, ticks);
	}

	/** A crystal growing out of the floor for a moment. */
	public static void spike(ServerWorld world, Vec3d pos, float size, int ticks) {
		CrystalEffectEntity.spawn(world, ModEntities.CRYSTAL_SPIKE, pos, size, ticks);
	}

	public static void spikeRing(ServerWorld world, Vec3d center, double radius, int count, float size) {
		for (int i = 0; i < count; i++) {
			double a = Math.PI * 2 * i / count + world.random.nextDouble() * 0.5;
			double r = radius * (0.6 + world.random.nextDouble() * 0.4);
			spike(world, center.add(Math.cos(a) * r, 0, Math.sin(a) * r), size * (0.7f + world.random.nextFloat() * 0.6f), 20 + world.random.nextInt(10));
		}
	}

	/** A glowing warning ring on the ground that shrinks and fills as the eruption approaches. */
	public static void telegraph(ServerWorld world, Vec3d center, double radius, float progress) {
		int points = (int) (radius * 3);
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			world.spawnParticles(ModParticles.PRISM_FLARE, center.x + Math.cos(a) * radius, center.y + 0.1,
					center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
		int fill = (int) (progress * radius * 1.5);
		for (int i = 0; i < fill; i++) {
			double a = world.random.nextDouble() * Math.PI * 2;
			double r = world.random.nextDouble() * radius;
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, center.x + Math.cos(a) * r, center.y + 0.1 + progress,
					center.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
		}
	}

	/** Crystal spikes burst out of the ground and throw everything in the circle into the air. */
	public static void erupt(ServerWorld world, LivingEntity source, Vec3d center, double radius, float damage) {
		BlockStateParticleEffect shard = new BlockStateParticleEffect(ParticleTypes.BLOCK,
				ModBlocks.crystal(CrystalColor.VIOLET, 2).getDefaultState());
		// a big crystal bursts out where the circle was, with a few smaller ones around it
		spike(world, center, (float) Math.max(1.0, radius * 0.55), 26);
		spikeRing(world, center, radius * 0.7, (int) Math.min(6, 2 + radius), (float) Math.max(0.6, radius * 0.25));
		world.spawnParticles(shard, center.x, center.y + 0.3, center.z, 14, radius * 0.4, 0.3, radius * 0.4, 0.3);
		world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, source.getSoundCategory(), 2.0f, 0.6f);
		world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), source.getSoundCategory(), 0.7f, 1.4f);
		for (LivingEntity e : Fx.around(world, center, radius, foesOf(source))) {
			e.damage(world.getDamageSources().indirectMagic(source, source), damage);
			Fx.push(e, new Vec3d(0, 0.95, 0));
			AmethystMob.infect(e, 160);
		}
	}

	/** A beam from {@code from} to {@code to}; returns the entities it hit. */
	public static List<LivingEntity> beam(ServerWorld world, LivingEntity source, Vec3d from, Vec3d to, boolean thin) {
		Vec3d d = to.subtract(from);
		double len = d.length();
		Vec3d step = d.normalize();
		for (double t = 0; t < len; t += thin ? 1.1 : 0.7) {
			Vec3d p = from.add(step.multiply(t));
			world.spawnParticles(thin ? ModParticles.CRYSTAL_SPARK : ModParticles.PRISM_FLARE, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
		}
		Box box = new Box(from, to).expand(1.0);
		return world.getEntitiesByClass(LivingEntity.class, box,
				e -> foesOf(source).test(e) && e.getBoundingBox().expand(0.4).raycast(from, to).isPresent());
	}
}
