package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/** Telegraphed abilities for goal-driven creatures: wind up (animation), then strike, then cool down. */
public class AbilityGoals {
	public abstract static class Windup extends Goal {
		protected final AmethystMob mob;
		private final int windup;
		private final int cooldown;
		private int timer;
		private int nextUse;

		public Windup(AmethystMob mob, int windup, int cooldown) {
			this.mob = mob;
			this.windup = windup;
			this.cooldown = cooldown;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		protected abstract boolean inRange(LivingEntity target, double distanceSq);

		protected abstract void release(ServerWorld world, LivingEntity target);

		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
		}

		/** The pose the model takes while winding up. */
		protected int anim() {
			return AmethystMob.ANIM_RAISE;
		}

		@Override
		public boolean canStart() {
			LivingEntity target = mob.getTarget();
			if (target == null || !target.isAlive() || mob.age < nextUse || !mob.canSee(target)
					|| !inRange(target, mob.squaredDistanceTo(target))) {
				return false;
			}
			// variety: the same ability right after itself is unlikely, and ready abilities do not all fire at once
			if (mob.lastAbility == this && mob.getRandom().nextFloat() < 0.85f) {
				return false;
			}
			return mob.getRandom().nextFloat() < 0.35f;
		}

		@Override
		public boolean shouldContinue() {
			return timer > 0 && mob.getTarget() != null && mob.getTarget().isAlive();
		}

		@Override
		public void start() {
			timer = windup;
			mob.play(anim(), windup + 8);
			mob.lastAbility = this;
			mob.getNavigation().stop();
		}

		@Override
		public void stop() {
			mob.setAction(0);
			nextUse = mob.age + cooldown;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			if (target == null || !(mob.getWorld() instanceof ServerWorld world)) {
				return;
			}
			mob.getLookControl().lookAt(target, 40.0f, 40.0f);
			mob.getNavigation().stop();
			windupTick(world, target, windup - timer);
			if (--timer <= 0) {
				release(world, target);
			}
		}
	}

	/** Fires a fan of crystal shards at the target. */
	public static class ShootShards extends Windup {
		private final double minDist;
		private final double maxDist;
		private final int count;
		private final float spread;
		private final float damage;
		private final float speed;

		public ShootShards(AmethystMob mob, double minDist, double maxDist, int windup, int cooldown, int count, float spread, float damage, float speed) {
			super(mob, windup, cooldown);
			this.minDist = minDist;
			this.maxDist = maxDist;
			this.count = count;
			this.spread = spread;
			this.damage = damage;
			this.speed = speed;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return distanceSq >= minDist * minDist && distanceSq <= maxDist * maxDist;
		}

		@Override
		protected int anim() {
			return AmethystMob.ANIM_CAST;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			Vec3d eye = mob.getEyePos();
			if (elapsed % 3 == 0) {
				world.spawnParticles(ModParticles.PRISM_FLARE, eye.x, eye.y, eye.z, 1, 0.4, 0.4, 0.4, 0.05);
			}
			if (elapsed == 0) {
				Fx.sound(world, mob, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			Vec3d origin = mob.getEyePos();
			Vec3d aim = target.getEyePos().subtract(origin);
			for (int i = 0; i < count; i++) {
				CrystalShardEntity shard = new CrystalShardEntity(world, mob, damage);
				shard.setPosition(origin.x, origin.y - 0.2, origin.z);
				shard.setVelocity(aim.x, aim.y, aim.z, speed, spread * (count > 1 ? 1 : 0.4f));
				world.spawnEntity(shard);
			}
			Fx.sound(world, mob, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.6f);
			Fx.particles(world, ModParticles.PRISM_FLARE, origin, 14, 0.3, 0.2);
		}
	}

	/** Slams the ground and sends a shockwave that hurts and throws everything around. */
	public static class Slam extends Windup {
		private final double radius;
		private final float damage;

		public Slam(AmethystMob mob, int windup, int cooldown, double radius, float damage) {
			super(mob, windup, cooldown);
			this.radius = radius;
			this.damage = damage;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return distanceSq <= (radius - 1) * (radius - 1);
		}

		@Override
		protected int anim() {
			return AmethystMob.ANIM_SLAM;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				BossFx.marker(world, mob.getPos(), radius, 28);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			BossFx.erupt(world, mob, mob.getPos(), radius, damage);
			BossFx.spikeRing(world, mob.getPos(), radius * 0.6, 7, 1.4f);
			Cutscenes.shake(world, mob.getPos(), 20, 1.5f, 8);
		}
	}
}
