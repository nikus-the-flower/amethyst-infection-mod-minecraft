package com.amethystmod.entity;

import com.amethystmod.item.Fx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The building blocks of the Heart's attacks. Each brick keeps its own state, so several can run at
 * once: a combination attack is a short script that starts bricks at chosen moments. A brick warns
 * with rune circles before it hits, and hits exactly where its crystals or beams are.
 */
final class HeartBricks {
	private HeartBricks() {
	}

	abstract static class Brick {
		protected final HeartEntity heart;
		protected int t;
		private final Map<UUID, Integer> lastHit = new HashMap<>();
		private final List<Vec3d> pendingPos = new ArrayList<>();
		private final List<Integer> pendingAt = new ArrayList<>();

		Brick(HeartEntity heart) {
			this.heart = heart;
		}

		/** One tick; true once the brick is finished. */
		final boolean tick(ServerWorld world, List<ServerPlayerEntity> players) {
			t++;
			return step(world, players);
		}

		protected abstract boolean step(ServerWorld world, List<ServerPlayerEntity> players);

		/** Called when the brick ends or the attack is cut short (death, phase change). */
		void stop() {
		}

		protected int stage() {
			return heart.getStage();
		}

		protected Random random() {
			return heart.getRandom();
		}

		protected Vec3d center() {
			return heart.center();
		}

		protected boolean canHit(LivingEntity e, int every) {
			Integer at = lastHit.get(e.getUuid());
			if (at != null && t - at < every) {
				return false;
			}
			lastHit.put(e.getUuid(), t);
			return true;
		}

		/** Hurts every foe within {@code radius} of {@code at} (each at most once per {@code every} ticks). */
		protected void hitAround(ServerWorld world, Vec3d at, double radius, float damage, double up, int every) {
			for (LivingEntity e : Fx.around(world, at, radius, BossFx.foesOf(heart))) {
				if (canHit(e, every)) {
					heart.hurt(world, e, damage);
					Fx.push(e, new Vec3d(0, up, 0));
				}
			}
		}

		/** Queues a crystal that bursts at {@code at} in {@code delay} ticks (the caller shows the warning). */
		protected void queue(Vec3d at, int delay) {
			pendingPos.add(at);
			pendingAt.add(t + delay);
		}

		/** Bursts the queued crystals whose time has come. */
		protected void burst(ServerWorld world, double radius, float size, float damage) {
			for (int i = pendingAt.size() - 1; i >= 0; i--) {
				if (pendingAt.get(i) > t) {
					continue;
				}
				Vec3d at = pendingPos.get(i);
				BossFx.spike(world, at, size * (0.85f + random().nextFloat() * 0.3f), 18 + (int) (size * 4));
				hitAround(world, at, radius, damage, 0.6, 10);
				pendingAt.remove(i);
				pendingPos.remove(i);
			}
		}

		protected boolean pending() {
			return !pendingAt.isEmpty();
		}

		/** A random spot on the arena floor, between {@code min} blocks from the Heart and the edge. */
		protected Vec3d randomSpot(double min) {
			double a = random().nextDouble() * Math.PI * 2;
			double r = min + Math.sqrt(random().nextDouble()) * (HeartEntity.ARENA - 2 - min);
			return center().add(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		protected Vec3d floorUnder(ServerPlayerEntity p) {
			return new Vec3d(p.getX(), center().y, p.getZ());
		}
	}

	/** Crystals keep bursting where each player stands: keep running. */
	static class Chase extends Brick {
		private final int duration;
		private final int every;

		Chase(HeartEntity heart, int duration, int every) {
			super(heart);
			this.duration = duration;
			this.every = every;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			if (t == 1) {
				Fx.sound(world, heart, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 4.0f, 1.3f);
			}
			if (t >= 8 && t <= duration && t % every == 0) {
				for (ServerPlayerEntity p : players) {
					Vec3d at = floorUnder(p);
					BossFx.marker(world, at, 1.5, 10);
					queue(at, 10);
				}
			}
			burst(world, 1.5, 1.1f, 6.0f + 1.5f * stage());
			return t > duration && !pending();
		}
	}

	/** One sweep of a spiral: from the Heart outward or from the edge inward. */
	record Pass(boolean outward, int sign, double angle) {
	}

	/**
	 * Arms of crystals spiral out one after another (and/or back in). Big spirals use fewer, larger
	 * crystals spaced further apart.
	 */
	static class Spiral extends Brick {
		private final int arms;
		private final boolean big;
		private final List<Pass> passes;
		private final int steps;
		private final int interval;
		private final double spacing;
		private final double twist;
		private final int lead;
		private int pass;
		private int passStart = 4;

		Spiral(HeartEntity heart, int arms, boolean big, List<Pass> passes) {
			super(heart);
			this.arms = arms;
			this.big = big;
			this.passes = passes;
			this.spacing = big ? 3.0 : 1.6;
			this.steps = (int) ((HeartEntity.ARENA - 6) / spacing) + 1;
			this.interval = big ? 4 : 2;
			this.twist = big ? 0.28 : 0.16;
			this.lead = big ? 9 : 6;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			if (pass < passes.size()) {
				int local = t - passStart;
				if (local >= 0 && local % interval == 0) {
					int k = local / interval;
					if (k >= steps) {
						pass++;
						passStart = t + 8;
					} else {
						Pass p = passes.get(pass);
						int index = p.outward() ? k : steps - 1 - k;
						double r = 6 + index * spacing;
						for (int i = 0; i < arms; i++) {
							double a = p.angle() + i * Math.PI * 2 / arms + p.sign() * twist * k;
							Vec3d at = center().add(Math.cos(a) * r, 0, Math.sin(a) * r);
							BossFx.marker(world, at, big ? 2.6 : 1.5, lead + 1);
							queue(at, lead);
						}
						if (k % (big ? 2 : 4) == 0) {
							Fx.sound(world, heart, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 3.0f, (big ? 0.4f : 0.6f) + k * 0.03f);
						}
					}
				}
			}
			if (big) {
				burst(world, 2.6, 2.2f, 12.0f + 2 * stage());
			} else {
				burst(world, 1.5, 1.2f, 9.0f + 2 * stage());
			}
			return pass >= passes.size() && !pending();
		}
	}

	/**
	 * Crystals keep growing out of the floor at random spots all over the arena, many each second,
	 * and now and then right under somebody. Each spot is marked a moment before.
	 */
	static class Grow extends Brick {
		private static final int LEAD = 16;
		private static final double RADIUS = 1.4;
		private final int duration;
		private final double perTick;
		private double owed;

		Grow(HeartEntity heart, int duration, double perTick) {
			super(heart);
			this.duration = duration;
			this.perTick = perTick;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			if (t <= duration) {
				owed += perTick;
				while (owed >= 1) {
					owed--;
					Vec3d at = randomSpot(4);
					BossFx.marker(world, at, RADIUS, LEAD + 1);
					queue(at, LEAD);
				}
				if (t % 24 == 1) {
					for (ServerPlayerEntity p : players) {
						Vec3d at = floorUnder(p);
						BossFx.marker(world, at, RADIUS, LEAD + 1);
						queue(at, LEAD);
					}
				}
				if (t % 6 == 0) {
					Fx.sound(world, heart, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 3.0f, 0.6f + random().nextFloat() * 0.4f);
				}
			}
			burst(world, RADIUS, 1.0f, 8.0f + 2 * stage());
			return t > duration && !pending();
		}
	}

	/** Crystals keep falling from above onto random spots all over the arena, many each second. */
	static class Rain extends Brick {
		private static final int WARN = 6;
		private static final double RADIUS = 1.4;
		private final int duration;
		private final double perTick;
		private double owed;
		private final List<Vec3d> falling = new ArrayList<>();
		private final List<Integer> landAt = new ArrayList<>();

		Rain(HeartEntity heart, int duration, double perTick) {
			super(heart);
			this.duration = duration;
			this.perTick = perTick;
		}

		private void drop(ServerWorld world, Vec3d at) {
			BossFx.marker(world, at, RADIUS, WARN + CrystalEffectEntity.FALL_TICKS + 1);
			falling.add(at);
			landAt.add(t + WARN);
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			if (t <= duration) {
				owed += perTick;
				while (owed >= 1) {
					owed--;
					drop(world, randomSpot(4));
				}
				if (t % 24 == 1) {
					for (ServerPlayerEntity p : players) {
						drop(world, floorUnder(p));
					}
				}
			}
			for (int i = landAt.size() - 1; i >= 0; i--) {
				Vec3d at = falling.get(i);
				if (landAt.get(i) == t) {
					// the crystal starts falling now and lands FALL_TICKS later
					CrystalEffectEntity.spawn(world, ModEntities.FALLING_CRYSTAL, at, 0.9f + random().nextFloat() * 0.3f, CrystalEffectEntity.FALL_TICKS + 14);
				} else if (landAt.get(i) + CrystalEffectEntity.FALL_TICKS <= t) {
					hitAround(world, at, RADIUS, 8.0f + 2 * stage(), 0.5, 8);
					falling.remove(i);
					landAt.remove(i);
				}
			}
			if (t % 5 == 0 && t <= duration + CrystalEffectEntity.FALL_TICKS) {
				Fx.sound(world, heart, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 3.0f, 0.8f + random().nextFloat() * 0.4f);
			}
			return t > duration && falling.isEmpty();
		}
	}

	/** One to three ground waves in a row: glowing ring walls running out from the Heart. Jump over them. */
	static class Waves extends Brick {
		static final float SPEED = 0.9f;
		static final float HEIGHT = 0.75f;
		/** The hitbox is a little smaller than the glowing wall, so a well-timed jump always clears it. */
		private static final float HIT_HEIGHT = 0.6f;
		private static final float HIT_BAND = 0.55f;
		private static final int GAP = 14;
		private static final int CHARGE = 30;
		private final int count;
		private final List<BeamEffectEntity> rings = new ArrayList<>();
		private final java.util.Set<String> hit = new java.util.HashSet<>();

		Waves(HeartEntity heart, int count) {
			super(heart);
			this.count = count;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			Vec3d core = heart.core();
			if (t <= CHARGE) {
				if (t % 3 == 0) {
					Vec3d from = core.add(random().nextGaussian() * 7, random().nextGaussian() * 4, random().nextGaussian() * 7);
					Fx.shoot(world, com.amethystmod.ModParticles.PRISM_FLARE, from, core.subtract(from).normalize(), 0.7);
				}
				if (t == 1) {
					Fx.sound(world, heart, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 4.0f, 0.9f);
				}
				return false;
			}
			if (t == CHARGE + 1) {
				int life = (int) ((HeartEntity.ARENA - 3.5) / SPEED) + 4;
				for (int i = 0; i < count; i++) {
					BeamEffectEntity ring = BeamEffectEntity.ring(world, center(), 3.5f, SPEED, HEIGHT, i * GAP, i * GAP + life);
					if (ring != null) {
						rings.add(ring);
					}
				}
			}
			if ((t - CHARGE - 1) % GAP == 0 && (t - CHARGE - 1) / GAP < count) {
				Fx.sound(world, heart, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 4.0f, 0.8f + 0.1f * ((t - CHARGE - 1) / GAP));
				Cutscenes.shake(world, center(), 50, 2.0f, 10);
			}
			Vec3d c = center();
			for (int r = 0; r < rings.size(); r++) {
				BeamEffectEntity ring = rings.get(r);
				if (!ring.started() || ring.isRemoved()) {
					continue;
				}
				float radius = ring.ringRadius(0);
				for (ServerPlayerEntity p : players) {
					double dx = p.getX() - c.x;
					double dz = p.getZ() - c.z;
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (Math.abs(dist - radius) < HIT_BAND && p.getY() < c.y + HIT_HEIGHT && hit.add(p.getUuid() + ":" + r)) {
						heart.hurt(world, p, 13.0f);
						Vec3d away = dist < 0.01 ? Vec3d.ZERO : new Vec3d(dx, 0, dz).normalize().multiply(1.3);
						Fx.push(p, away.add(0, 0.45, 0));
					}
				}
			}
			return t >= CHARGE + 1 + (count - 1) * GAP + (int) ((HeartEntity.ARENA - 3.5) / SPEED) + 2;
		}

		@Override
		void stop() {
			rings.forEach(BeamEffectEntity::discard);
			rings.clear();
		}
	}

	/**
	 * Lasers from the Heart down onto the floor in random directions (not at anyone), wandering
	 * around the arena. Each shows a thin line first, then fires.
	 */
	static class Lasers extends Brick {
		private static final int AIM = 24;
		private static final float WIDTH = 0.55f;
		private final int count;
		private final int duration;
		private final List<BeamEffectEntity> beams = new ArrayList<>();
		private final double[] angle;
		private final double[] turn;
		private final double[] radius;
		private final double[] phase;

		Lasers(HeartEntity heart, int count, int duration) {
			super(heart);
			this.count = count;
			this.duration = duration;
			angle = new double[count];
			turn = new double[count];
			radius = new double[count];
			phase = new double[count];
		}

		private Vec3d spot(int i) {
			double r = radius[i] + 6 * Math.sin(t * 0.05 + phase[i]);
			return center().add(Math.cos(angle[i]) * r, 0, Math.sin(angle[i]) * r);
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			Vec3d core = heart.core();
			if (t == 1) {
				double base = random().nextDouble() * Math.PI * 2;
				for (int i = 0; i < count; i++) {
					angle[i] = base + i * Math.PI * 2 / count + random().nextDouble() * 0.8;
					turn[i] = (random().nextBoolean() ? 1 : -1) * (0.012 + random().nextDouble() * 0.02);
					radius[i] = 12 + random().nextDouble() * 10;
					phase[i] = random().nextDouble() * Math.PI * 2;
					Vec3d to = spot(i).subtract(core);
					BeamEffectEntity beam = BeamEffectEntity.beam(world, core, to, (float) to.length() + 1, AIM + duration + 5);
					if (beam != null) {
						beams.add(beam);
					}
				}
				Fx.sound(world, heart, SoundEvents.BLOCK_BEACON_POWER_SELECT, 4.0f, 0.8f);
			}
			for (int i = 0; i < beams.size(); i++) {
				angle[i] += t > AIM ? turn[i] : turn[i] * 0.3;
				Vec3d to = spot(i).subtract(core);
				BeamEffectEntity beam = beams.get(i);
				beam.aim(to);
				beam.setLength((float) to.length() + 1);
				if (t == AIM) {
					beam.fire(WIDTH);
				}
				if (t > AIM && t % 3 == 0) {
					Vec3d end = core.add(to);
					for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new net.minecraft.util.math.Box(core, end).expand(1.0),
							e -> BossFx.foesOf(heart).test(e) && e.getBoundingBox().expand(WIDTH - 0.3).raycast(core, end).isPresent())) {
						e.timeUntilRegen = 0;
						heart.hurt(world, e, 5.0f + stage() * 1.5f);
					}
				}
			}
			if (t == AIM) {
				Fx.sound(world, heart, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 4.0f, 1.1f);
			}
			if (t > AIM && t % 20 == 0) {
				Fx.sound(world, heart, SoundEvents.BLOCK_BEACON_AMBIENT, 3.0f, 1.3f);
			}
			return beams.isEmpty() || t >= AIM + duration;
		}

		@Override
		void stop() {
			beams.forEach(BeamEffectEntity::discard);
			beams.clear();
		}
	}

	/** Five giant crystals at the corners of a pentagon around the arena (ten from phase 3, unless told otherwise). */
	static class Pentagon extends Brick {
		private final boolean inner;
		private final List<Vec3d> spots = new ArrayList<>();

		Pentagon(HeartEntity heart, boolean inner) {
			super(heart);
			this.inner = inner;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			if (t == 1) {
				double base = random().nextDouble() * Math.PI * 2;
				for (int i = 0; i < 5; i++) {
					double a = base + i * Math.PI * 2 / 5;
					spots.add(center().add(Math.cos(a) * 21, 0, Math.sin(a) * 21));
				}
				if (inner) {
					for (int i = 0; i < 5; i++) {
						double a = base + (i + 0.5) * Math.PI * 2 / 5;
						spots.add(center().add(Math.cos(a) * 12, 0, Math.sin(a) * 12));
					}
				}
				for (int i = 0; i < spots.size(); i++) {
					BossFx.marker(world, spots.get(i), 5.0, i < 5 ? 40 : 66);
				}
				Fx.sound(world, heart, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 5.0f, 0.3f);
			}
			if (t == 40 || t == 66) {
				int from = t == 40 ? 0 : 5;
				for (int i = from; i < Math.min(spots.size(), from + 5); i++) {
					BossFx.erupt(world, heart, spots.get(i), 5.0, 14.0f + 2 * stage());
				}
				Cutscenes.shake(world, center(), 50, 2.2f, 10);
				Fx.sound(world, heart, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 4.0f, 0.6f);
			}
			return t >= (spots.size() > 5 ? 74 : 48);
		}
	}

	/**
	 * The death beam: a thin sight line follows its target for two seconds, locks for a moment, then
	 * a thick beam fires along it, still turning slowly after the target.
	 */
	static class DeathBeam extends Brick {
		private static final int LOCK_TICKS = 8;
		private final int aimTicks;
		private final int fireTicks;
		private static final float LENGTH = 45.0f;
		private static final float WIDTH = 0.7f;
		private final List<UUID> targets = new ArrayList<>();
		private final List<BeamEffectEntity> beams = new ArrayList<>();
		private final List<Vec3d> aims = new ArrayList<>();
		private final double offset;

		DeathBeam(HeartEntity heart, List<ServerPlayerEntity> players, int count, double offset) {
			super(heart);
			this.offset = offset;
			int stage = Math.max(1, heart.getStage());
			aimTicks = 40 - 4 * (stage - 1);
			fireTicks = 22 + 8 * (stage - 1);
			if (!players.isEmpty()) {
				int first = heart.getRandom().nextInt(players.size());
				for (int i = 0; i < count; i++) {
					targets.add(players.get((first + i) % players.size()).getUuid());
				}
			}
		}

		private ServerPlayerEntity find(List<ServerPlayerEntity> players, UUID id) {
			for (ServerPlayerEntity p : players) {
				if (p.getUuid().equals(id)) {
					return p;
				}
			}
			return null;
		}

		/** Where beam {@code i} wants to point; a second beam on the same target is turned aside. */
		private Vec3d want(Vec3d core, ServerPlayerEntity p, int i) {
			Vec3d d = p.getPos().add(0, 1.0, 0).subtract(core).normalize();
			boolean twin = i > 0 && targets.get(i).equals(targets.get(0));
			return twin ? d.rotateY((float) offset) : d;
		}

		@Override
		protected boolean step(ServerWorld world, List<ServerPlayerEntity> players) {
			Vec3d core = heart.core();
			if (t == 1) {
				for (int i = 0; i < targets.size(); i++) {
					ServerPlayerEntity p = find(players, targets.get(i));
					Vec3d dir = p == null ? new Vec3d(1, -0.2, 0) : want(core, p, i);
					aims.add(dir);
					BeamEffectEntity beam = BeamEffectEntity.beam(world, core, dir, LENGTH, aimTicks + LOCK_TICKS + fireTicks + 5);
					if (beam != null) {
						beams.add(beam);
					}
				}
				Fx.sound(world, heart, SoundEvents.BLOCK_BEACON_POWER_SELECT, 4.0f, 0.5f);
			}
			if (beams.isEmpty()) {
				return true;
			}
			for (int i = 0; i < beams.size(); i++) {
				BeamEffectEntity beam = beams.get(i);
				ServerPlayerEntity p = find(players, targets.get(i));
				Vec3d aim = aims.get(i);
				if (p != null && t <= aimTicks) {
					aim = want(core, p, i);
				} else if (p != null && t > aimTicks + LOCK_TICKS) {
					// the fired beam drifts slowly after its target
					aim = aim.add(want(core, p, i).subtract(aim).multiply(0.07)).normalize();
				}
				aims.set(i, aim);
				beam.aim(aim);
				if (t == aimTicks + LOCK_TICKS) {
					beam.fire(WIDTH);
				}
				if (t > aimTicks + LOCK_TICKS && t % 3 == 0) {
					Vec3d end = core.add(aim.multiply(LENGTH));
					for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new net.minecraft.util.math.Box(core, end).expand(1.0),
							e -> BossFx.foesOf(heart).test(e) && e.getBoundingBox().expand(WIDTH - 0.3).raycast(core, end).isPresent())) {
						e.timeUntilRegen = 0;
						heart.hurt(world, e, 6.0f + stage() * 1.5f);
					}
				}
			}
			if (t % 10 == 0 && t <= aimTicks) {
				Fx.sound(world, heart, SoundEvents.BLOCK_BEACON_AMBIENT, 4.0f, 0.5f + t * 0.03f);
			}
			if (t == aimTicks) {
				Fx.sound(world, heart, SoundEvents.BLOCK_BEACON_DEACTIVATE, 4.0f, 1.6f);
			}
			if (t == aimTicks + LOCK_TICKS) {
				Fx.sound(world, heart, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 5.0f, 0.6f);
				Cutscenes.shake(world, center(), 50, 1.5f, fireTicks);
			}
			return t >= aimTicks + LOCK_TICKS + fireTicks;
		}

		@Override
		void stop() {
			for (BeamEffectEntity beam : beams) {
				beam.discard();
			}
			beams.clear();
		}
	}
}
