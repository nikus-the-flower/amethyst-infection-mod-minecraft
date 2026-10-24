package com.amethystmod.item;

import com.amethystmod.ModParticles;
import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

/** The staff and hammer abilities. */
public class Weapons {
	private static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(1.0f, 0.45f, 0.85f), 1.4f);
	private static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.65f, 0.3f, 1.0f), 1.4f);

	private static DamageSource sourceOf(ServerWorld world, LivingEntity user) {
		return user instanceof PlayerEntity p ? world.getDamageSources().playerAttack(p) : world.getDamageSources().mobAttack(user);
	}

	/** Flame Staff: a short-range flamethrower that sets everything in the cone ablaze. */
	public static void flamethrower(ServerWorld world, LivingEntity user, int usedTicks) {
		Vec3d dir = user.getRotationVec(1.0f);
		Vec3d origin = user.getEyePos().add(dir.multiply(0.8)).add(0, -0.25, 0);
		for (int i = 0; i < 10; i++) {
			Vec3d spread = dir.add(
					(user.getRandom().nextDouble() - 0.5) * 0.28,
					(user.getRandom().nextDouble() - 0.5) * 0.28,
					(user.getRandom().nextDouble() - 0.5) * 0.28);
			Fx.shoot(world, i % 4 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, origin, spread, 0.55 + user.getRandom().nextDouble() * 0.3);
		}
		Fx.shoot(world, ParticleTypes.SMOKE, origin.add(dir.multiply(3)), dir, 0.1);
		if (usedTicks % 5 == 0) {
			Fx.sound(world, user, SoundEvents.ITEM_FIRECHARGE_USE, 0.6f, 0.8f);
		}
		if (usedTicks % 3 == 0) {
			for (LivingEntity e : Fx.cone(world, user, 8.0, 0.85)) {
				e.timeUntilRegen = 0;
				Fx.hurtNoKnockback(e, sourceOf(world, user), 3.5f);
				e.setOnFireFor(8);
			}
		}
	}

	/** Prism Scepter: a piercing beam of crystal light. */
	/** The beam each player's prism scepter is drawing right now. */
	private static final java.util.Map<java.util.UUID, com.amethystmod.entity.BeamEffectEntity> PRISM_BEAMS = new java.util.HashMap<>();

	public static void prismBeam(ServerWorld world, LivingEntity user, int usedTicks) {
		double range = 28.0;
		Vec3d start = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d end = start.add(look.multiply(range));
		BlockHitResult hit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getPos();
		}
		Vec3d beam = end.subtract(start);
		double length = beam.length();
		Vec3d step = beam.normalize();
		Vec3d from = start.add(step.multiply(1.0)).add(0, -0.2, 0);
		// a 3D beam of light that follows the staff while it is held
		com.amethystmod.entity.BeamEffectEntity fx = PRISM_BEAMS.get(user.getUuid());
		Vec3d aim = end.subtract(from);
		if (fx == null || fx.isRemoved() || fx.getWorld() != world) {
			fx = com.amethystmod.entity.BeamEffectEntity.beam(world, from, aim, (float) aim.length(), 4);
			if (fx != null) {
				fx.fire(0.22f);
				PRISM_BEAMS.put(user.getUuid(), fx);
			}
		}
		if (fx != null) {
			fx.moveTo(from);
			fx.aim(aim);
			fx.setLength((float) aim.length());
			fx.keepAlive(3);
		}
		if (usedTicks % 2 == 0) {
			Fx.particles(world, ModParticles.CRYSTAL_SPARK, end, 2, 0.2, 0.08);
		}
		if (usedTicks % 12 == 0) {
			Fx.sound(world, user, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f + user.getRandom().nextFloat() * 0.6f);
		}
		if (usedTicks % 4 == 0) {
			Vec3d segEnd = end;
			var box = user.getBoundingBox().stretch(beam).expand(1.0);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, Fx.hostileTo(user))) {
				if (e.getBoundingBox().expand(0.35).raycast(start, segEnd).isPresent()) {
					e.timeUntilRegen = 0;
					Fx.hurtNoKnockback(e, world.getDamageSources().indirectMagic(user, user), 6.0f);
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0));
					Fx.particles(world, ModParticles.CRYSTAL_SPARK, e.getPos().add(0, e.getHeight() / 2, 0), 5, 0.3, 0.1);
				}
			}
		}
	}

	/** Frost Staff: a wave of cold that freezes and slows enemies and turns water to ice. */
	public static boolean frostWave(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d origin = player.getEyePos().add(dir.multiply(0.8)).add(0, -0.25, 0);
		for (int i = 0; i < 80; i++) {
			Vec3d spread = dir.add(
					(player.getRandom().nextDouble() - 0.5) * 0.7,
					(player.getRandom().nextDouble() - 0.5) * 0.7,
					(player.getRandom().nextDouble() - 0.5) * 0.7);
			Fx.shoot(world, i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SNOWFLAKE, origin, spread, 0.5 + player.getRandom().nextDouble() * 0.5);
		}
		for (LivingEntity e : Fx.cone(world, player, 10.0, 0.75)) {
			Fx.hurtNoKnockback(e, world.getDamageSources().freeze(), 6.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 8, 3));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 5, 0));
			e.setFrozenTicks(e.getMinFreezeDamageTicks() + 20 * 7);
			Fx.particles(world, ParticleTypes.SNOWFLAKE, e.getPos().add(0, e.getHeight() / 2, 0), 25, 0.4, 0.02);
		}
		for (int d = 1; d <= 9; d++) {
			BlockPos center = BlockPos.ofFloored(origin.add(dir.multiply(d)));
			for (BlockPos p : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
				if (world.getBlockState(p).isOf(Blocks.WATER) && world.getFluidState(p).isStill()) {
					world.setBlockState(p, Blocks.ICE.getDefaultState());
				}
			}
		}
		Fx.sound(world, player, SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
		Fx.sound(world, player, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
		return true;
	}

	/** Crystal Hammer: slams the ground and sends a crystal shockwave outward. */
	public static boolean slam(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d center = player.getPos();
		for (LivingEntity e : Fx.around(world, center, 7, Fx.hostileTo(player))) {
			e.damage(world.getDamageSources().playerAttack(player), 14.0f);
			Vec3d away = e.getPos().subtract(center);
			Vec3d push = away.lengthSquared() < 0.01 ? Vec3d.ZERO : away.normalize().multiply(0.9);
			Fx.push(e, push.add(0, 0.95, 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 1));
		}
		BlockStateParticleEffect shard = new BlockStateParticleEffect(ParticleTypes.BLOCK,
				ModBlocks.crystal(CrystalColor.VIOLET, 1).getDefaultState());
		for (int ring = 1; ring <= 7; ring++) {
			for (int i = 0; i < ring * 8; i++) {
				double angle = (Math.PI * 2 * i) / (ring * 8);
				Vec3d p = center.add(Math.cos(angle) * ring, 0.2, Math.sin(angle) * ring);
				Fx.particles(world, shard, p, 3, 0.2, 0.5);
				if (i % 4 == 0) {
					Fx.particles(world, ModParticles.CRYSTAL_SPARK, p.add(0, 0.5, 0), 1, 0.1, 0.15);
				}
			}
		}
		Fx.sound(world, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.0f, 1.3f);
		Fx.sound(world, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.5f);
		return true;
	}
}
