package com.amethystmod.item;

import com.amethystmod.ModParticles;
import com.amethystmod.ArtifactEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

/** The right-click abilities of the artifacts. */
public class Abilities {
	private static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.7f, 0.35f, 1.0f), 1.3f);

	/** Crystal Aegis: a thick crystal shell absorbs damage for a while. */
	public static boolean aegis(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 20, 4));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 15, 2));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 8, 1));
		Fx.particles(world, VIOLET, player.getPos().add(0, 1, 0), 60, 0.8, 0.02);
		Fx.particles(world, ModParticles.CRYSTAL_SPARK, player.getPos().add(0, 1, 0), 25, 0.7, 0.05);
		Fx.sound(world, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.6f);
		Fx.sound(world, player, SoundEvents.ITEM_TOTEM_USE, 0.5f, 1.6f);
		return true;
	}

	/** Amethyst Wings: limited-time creative-style flight. */
	public static boolean wings(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		ArtifactEvents.startFlight(player, 20 * 25);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 20 * 35, 0));
		Fx.particles(world, ModParticles.CRYSTAL_SPARK, player.getPos().add(0, 1, 0), 40, 0.6, 0.15);
		Fx.sound(world, player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.4f);
		return true;
	}

	/** Blink Ring: teleport along the line of sight. */
	public static boolean blink(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d from = player.getPos();
		for (double dist = 14.0; dist >= 1.0; dist -= 0.5) {
			Vec3d offset = look.multiply(dist);
			if (world.isSpaceEmpty(player, player.getBoundingBox().offset(offset))) {
				Fx.particles(world, ParticleTypes.PORTAL, from.add(0, 1, 0), 40, 0.4, 0.5);
				player.requestTeleport(from.x + offset.x, from.y + offset.y, from.z + offset.z);
				player.fallDistance = 0;
				Fx.particles(world, ParticleTypes.REVERSE_PORTAL, player.getPos().add(0, 1, 0), 40, 0.4, 0.2);
				Fx.sound(world, player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
				return true;
			}
		}
		return false;
	}

	/** Berserker Core: a burst of power with a price afterwards (see ArtifactEvents). */
	public static boolean berserk(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		int ticks = 20 * 20;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ticks, 2));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks, 2));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, ticks, 2));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks, 0));
		ArtifactEvents.scheduleBerserkCrash(player, ticks);
		Fx.particles(world, ParticleTypes.FLAME, player.getPos().add(0, 1, 0), 60, 0.6, 0.1);
		Fx.particles(world, ParticleTypes.ANGRY_VILLAGER, player.getPos().add(0, 2, 0), 6, 0.4, 0.0);
		Fx.sound(world, player, SoundEvents.ENTITY_RAVAGER_ROAR, 1.2f, 0.8f);
		return true;
	}

	/** Stasis Crystal: freezes everything around in time. */
	public static boolean stasis(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		boolean any = false;
		for (LivingEntity e : Fx.around(world, player.getPos(), 12, Fx.hostileTo(player))) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 8, 9));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 8, 4));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 8, 0));
			e.setFrozenTicks(e.getMinFreezeDamageTicks() + 20 * 4);
			Fx.particles(world, ParticleTypes.SNOWFLAKE, e.getPos().add(0, e.getHeight() / 2, 0), 25, 0.4, 0.02);
			any = true;
		}
		Fx.particles(world, ModParticles.CRYSTAL_SPARK, player.getPos().add(0, 1, 0), 60, 3.0, 0.02);
		Fx.sound(world, player, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.5f, 1.8f);
		return true;
	}

	/** Gravity Orb: pulls everything around into the user and shakes it up. */
	public static boolean gravity(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d center = player.getPos().add(0, 0.5, 0);
		for (LivingEntity e : Fx.around(world, center, 16, Fx.hostileTo(player))) {
			Vec3d pull = center.subtract(e.getPos());
			double dist = Math.max(1.0, pull.length());
			Fx.push(e, pull.normalize().multiply(Math.min(2.2, 0.5 + dist * 0.12)).add(0, 0.25, 0));
			e.damage(world.getDamageSources().indirectMagic(player, player), 4.0f);
			Fx.particles(world, ParticleTypes.PORTAL, e.getPos().add(0, e.getHeight() / 2, 0), 20, 0.3, 0.6);
		}
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, center, 120, 4.0, 0.3);
		Fx.sound(world, player, SoundEvents.ENTITY_ENDERMAN_STARE, 1.0f, 0.5f);
		return true;
	}

	/** Thunder Shard: calls lightning down on whatever the user is looking at. */
	public static boolean thunder(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		double range = 48;
		Vec3d start = player.getEyePos();
		Vec3d end = start.add(player.getRotationVec(1.0f).multiply(range));
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
		Vec3d target = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();

		Box searchBox = player.getBoundingBox().stretch(target.subtract(start)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.raycast(player, start, target, searchBox,
				e -> !e.isSpectator() && e.canHit(), start.squaredDistanceTo(target));
		if (entityHit != null) {
			target = entityHit.getPos();
		}

		for (int i = 0; i < 3; i++) {
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(target.x, target.y, target.z);
				bolt.setChanneler(player);
				world.spawnEntity(bolt);
			}
		}
		for (LivingEntity e : Fx.around(world, target, 4, Fx.hostileTo(player))) {
			e.damage(world.getDamageSources().indirectMagic(player, player), 10.0f);
		}
		return true;
	}
}
