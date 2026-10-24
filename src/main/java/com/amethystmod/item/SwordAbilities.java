package com.amethystmod.item;

import com.amethystmod.InfectionSpread;
import com.amethystmod.ModParticles;
import com.amethystmod.effect.ModEffects;
import com.amethystmod.entity.CrystalKin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** On-hit effects and right-click abilities of the relic swords. */
public class SwordAbilities {
	private static net.minecraft.entity.damage.DamageSource by(ServerWorld world, LivingEntity attacker) {
		return attacker instanceof PlayerEntity p ? world.getDamageSources().playerAttack(p) : world.getDamageSources().mobAttack(attacker);
	}

	// ---- Flame Brand ---------------------------------------------------------------

	public static void flameHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		target.setOnFireFor(6);
		Fx.particles(world, ParticleTypes.FLAME, target.getPos().add(0, target.getHeight() / 2, 0), 14, 0.4, 0.05);
	}

	/** A short, wide burst of fire in front of the wielder. */
	public static boolean flameWave(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d origin = player.getEyePos().add(dir.multiply(0.6));
		for (int i = 0; i < 90; i++) {
			Vec3d spread = dir.add((player.getRandom().nextDouble() - 0.5) * 0.9, (player.getRandom().nextDouble() - 0.5) * 0.5,
					(player.getRandom().nextDouble() - 0.5) * 0.9);
			Fx.shoot(world, i % 5 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, origin, spread, 0.4 + player.getRandom().nextDouble() * 0.5);
		}
		for (LivingEntity e : Fx.cone(world, player, 7.0, 0.55)) {
			e.damage(world.getDamageSources().playerAttack(player), 9.0f);
			e.setOnFireFor(8);
		}
		Fx.sound(world, player, SoundEvents.ITEM_FIRECHARGE_USE, 1.4f, 0.6f);
		Fx.sound(world, player, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
		return true;
	}

	// ---- Frost Edge ----------------------------------------------------------------

	public static void frostHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 4, 1));
		target.setFrozenTicks(target.getMinFreezeDamageTicks() + 60);
		Fx.particles(world, ParticleTypes.SNOWFLAKE, target.getPos().add(0, target.getHeight() / 2, 0), 18, 0.4, 0.03);
	}

	/** A ring of killing cold around the wielder. */
	public static boolean frostNova(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d center = player.getPos();
		for (double ring = 1; ring <= 6; ring += 1) {
			int points = (int) (ring * 8);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x + Math.cos(a) * ring, center.y + 0.3,
						center.z + Math.sin(a) * ring, 1, 0.0, 0.3, 0.0, 0.02);
			}
		}
		for (LivingEntity e : Fx.around(world, center, 6, Fx.hostileTo(player))) {
			e.damage(world.getDamageSources().freeze(), 7.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 6, 3));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 4, 0));
			e.setFrozenTicks(e.getMinFreezeDamageTicks() + 20 * 6);
		}
		Fx.sound(world, player, SoundEvents.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
		Fx.sound(world, player, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.7f);
		return true;
	}

	// ---- Storm Blade ---------------------------------------------------------------

	public static void stormHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.getRandom().nextFloat() < 0.3f) {
			strike(world, target, attacker, 8.0f);
		}
	}

	private static void strike(ServerWorld world, LivingEntity target, LivingEntity attacker, float damage) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt != null) {
			bolt.setCosmetic(true);
			bolt.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
			world.spawnEntity(bolt);
		}
		target.damage(world.getDamageSources().lightningBolt(), damage);
		Fx.particles(world, ParticleTypes.ELECTRIC_SPARK, target.getPos().add(0, target.getHeight() / 2, 0), 25, 0.5, 0.3);
	}

	/** Lightning leaps from enemy to enemy, starting with the one you are looking at. */
	public static boolean chainLightning(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d start = player.getEyePos();
		Vec3d end = start.add(player.getRotationVec(1.0f).multiply(24));
		EntityHitResult hit = ProjectileUtil.raycast(player, start, end, new Box(start, end).expand(1.5),
				e -> e instanceof LivingEntity l && Fx.hostileTo(player).test(l), 24 * 24);
		LivingEntity first = hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
		if (first == null) {
			List<LivingEntity> near = Fx.cone(world, player, 12.0, 0.8);
			first = near.isEmpty() ? null : near.get(0);
		}
		if (first == null) {
			return false;
		}
		List<LivingEntity> chain = new ArrayList<>();
		LivingEntity current = first;
		Vec3d from = player.getPos().add(0, 1.4, 0);
		for (int i = 0; i < 6 && current != null; i++) {
			chain.add(current);
			Vec3d to = current.getPos().add(0, current.getHeight() / 2, 0);
			Vec3d d = to.subtract(from);
			for (double t = 0; t < d.length(); t += 0.5) {
				Vec3d p = from.add(d.normalize().multiply(t));
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
			strike(world, current, player, 9.0f);
			from = to;
			LivingEntity previous = current;
			current = Fx.around(world, to, 6, e -> Fx.hostileTo(player).test(e) && !chain.contains(e)).stream()
					.min((a, b) -> Double.compare(a.squaredDistanceTo(previous), b.squaredDistanceTo(previous))).orElse(null);
		}
		Fx.sound(world, player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.4f);
		return true;
	}

	// ---- Void Edge -----------------------------------------------------------------

	public static void voidHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 4, 1));
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 6, 0));
		attacker.heal(1.5f);
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, target.getPos().add(0, target.getHeight() / 2, 0), 20, 0.4, 0.3);
	}

	/** Slips through space up to ten blocks forward, cutting everything on the way. */
	public static boolean voidStep(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d from = player.getPos();
		for (double dist = 10.0; dist >= 2.0; dist -= 0.5) {
			Vec3d offset = look.multiply(dist);
			if (!world.isSpaceEmpty(player, player.getBoundingBox().offset(offset))) {
				continue;
			}
			Box path = new Box(from, from.add(offset)).expand(1.2);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, path, Fx.hostileTo(player))) {
				e.damage(world.getDamageSources().playerAttack(player), 11.0f);
			}
			for (double t = 0; t < dist; t += 0.5) {
				Vec3d p = from.add(look.multiply(t)).add(0, 1, 0);
				world.spawnParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 4, 0.2, 0.3, 0.2, 0.05);
			}
			player.requestTeleport(from.x + offset.x, from.y + offset.y, from.z + offset.z);
			player.fallDistance = 0;
			Fx.sound(world, player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
			return true;
		}
		return false;
	}

	// ---- Purifier's Blade ----------------------------------------------------------

	public static void purifierHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (target instanceof CrystalKin) {
			target.damage(world.getDamageSources().magic(), 10.0f);
			Fx.particles(world, ModParticles.WARD_MOTE, target.getPos().add(0, target.getHeight() / 2, 0), 20, 0.5, 0.08);
		}
		attacker.removeStatusEffect(ModEffects.BLIGHT);
	}

	/** A cleansing flash that burns the infection from the ground and scorches the creatures of the amethyst. */
	public static boolean purgingFlash(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d center = player.getPos();
		InfectionSpread.purgeAround(world, player.getBlockPos(), 7, player.getRandom(), 90);
		for (LivingEntity e : Fx.around(world, center, 8, t -> t != player && t instanceof CrystalKin && t.isAlive())) {
			e.damage(world.getDamageSources().magic(), 18.0f);
			Vec3d away = e.getPos().subtract(center).multiply(1, 0, 1).normalize().multiply(1.1);
			Fx.push(e, away.add(0, 0.4, 0));
		}
		player.removeStatusEffect(ModEffects.BLIGHT);
		player.heal(4.0f);
		for (double ring = 1; ring <= 8; ring += 1.5) {
			int points = (int) (ring * 7);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				world.spawnParticles(ModParticles.WARD_MOTE, center.x + Math.cos(a) * ring, center.y + 0.3,
						center.z + Math.sin(a) * ring, 1, 0.0, 0.2, 0.0, 0.05);
			}
		}
		Fx.sound(world, player, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 1.6f);
		Fx.sound(world, player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
		return true;
	}
}
