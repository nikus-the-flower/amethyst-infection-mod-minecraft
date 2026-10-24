package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * A spear of light Amitariel summons over his head. It hovers in a ring around him turning to follow
 * its target, then locks its aim (a thin line shows where it will fly) and shoots straight along it.
 * It sticks in the floor for a moment where it lands. The purified spear shoots the same spears (pure
 * white, friendly to players) straight where its holder looks. Not saved.
 */
public class PhantomSpearEntity extends Entity {
	private static final int HOVER = 0;
	private static final int FLY = 1;
	private static final int STUCK = 2;
	/** Ticks between locking the aim and launching: the time to step aside. */
	public static final int LOCK = 14;
	private static final double SPEED = 1.25;
	private static final TrackedData<Boolean> PURE = DataTracker.registerData(PhantomSpearEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private int state = HOVER;
	private UUID ownerId;
	private UUID targetId;
	private int index;
	private int count;
	private int launchAt;
	private float damage;
	private Vec3d dir = new Vec3d(1, 0, 0);
	private int flown;
	private int stuck;

	public PhantomSpearEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	/** Summons spear {@code index} of {@code count} over {@code owner}, launched at {@code target} after {@code delay} ticks. */
	public static void summon(ServerWorld world, LivingEntity owner, LivingEntity target, int index, int count, int delay, float damage) {
		PhantomSpearEntity spear = ModEntities.PHANTOM_SPEAR.create(world);
		if (spear == null) {
			return;
		}
		spear.ownerId = owner.getUuid();
		spear.targetId = target.getUuid();
		spear.index = index;
		spear.count = count;
		spear.launchAt = delay;
		spear.damage = damage;
		Vec3d pos = spear.hoverPos(owner);
		spear.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
		world.spawnEntity(spear);
		world.spawnParticles(ModParticles.PRISM_FLARE, pos.x, pos.y, pos.z, 6, 0.3, 0.3, 0.3, 0.02);
	}

	private Vec3d hoverPos(Entity owner) {
		double a = index * Math.PI * 2 / Math.max(1, count) + age * 0.035;
		return owner.getPos().add(Math.cos(a) * 2.6, 3.8 + 0.25 * Math.sin(age * 0.15 + index), Math.sin(a) * 2.6);
	}

	/** Points the model along {@code d} (the same convention as arrows). */
	private void face(Vec3d d) {
		double h = Math.sqrt(d.x * d.x + d.z * d.z);
		setYaw((float) (MathHelper.atan2(d.x, d.z) * 57.2957795));
		setPitch((float) (MathHelper.atan2(d.y, h) * 57.2957795));
	}

	/** A purified spear shot by a player from {@code from} along {@code dir}: it only hurts monsters. */
	public static void shoot(ServerWorld world, PlayerEntity owner, Vec3d from, Vec3d dir, float damage) {
		PhantomSpearEntity spear = ModEntities.PHANTOM_SPEAR.create(world);
		if (spear == null) {
			return;
		}
		spear.ownerId = owner.getUuid();
		spear.damage = damage;
		spear.dir = dir.normalize();
		spear.state = FLY;
		spear.dataTracker.set(PURE, true);
		spear.refreshPositionAndAngles(from.x, from.y, from.z, 0, 0);
		spear.face(spear.dir);
		world.spawnEntity(spear);
	}

	public boolean isPure() {
		return dataTracker.get(PURE);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(PURE, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		switch (state) {
			case HOVER -> hover(world);
			case FLY -> fly(world);
			default -> {
				if (++stuck > 30) {
					discard();
				}
			}
		}
	}

	private void hover(ServerWorld world) {
		Entity owner = ownerId == null ? null : world.getEntity(ownerId);
		if (!(owner instanceof LivingEntity living) || !living.isAlive()) {
			discard();
			return;
		}
		Entity target = targetId == null ? null : world.getEntity(targetId);
		if (target == null) {
			target = BossFx.playersNear(world, owner.getPos(), 40).stream().filter(p -> p.getUuid().equals(targetId)).findFirst().orElse(null);
		}
		if (!(target instanceof LivingEntity t) || !t.isAlive() || t.isSpectator()) {
			ServerPlayerEntity other = BossFx.playersNear(world, owner.getPos(), 32).stream().findFirst().orElse(null);
			if (other == null) {
				discard();
				return;
			}
			targetId = other.getUuid();
			return;
		}
		Vec3d pos = hoverPos(owner);
		setPosition(pos);
		Vec3d aim = t.getPos().add(0, t.getHeight() * 0.5, 0).subtract(pos).normalize();
		if (age < launchAt - LOCK) {
			dir = aim;
		} else if (age == launchAt - LOCK) {
			// the aim is fixed now: a thin line shows where it will fly
			dir = aim;
			BeamEffectEntity.beam(world, pos, dir, 32, LOCK);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 0.8f, 1.8f);
		}
		face(dir);
		if (age >= launchAt) {
			state = FLY;
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.HOSTILE, 1.5f, 0.7f);
		}
	}

	private void fly(ServerWorld world) {
		Vec3d pos = getPos();
		Vec3d next = pos.add(dir.multiply(SPEED));
		Entity owner = ownerId == null ? null : world.getEntity(ownerId);
		LivingEntity source = owner instanceof LivingEntity l ? l : null;
		boolean pure = isPure();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(pos, next).expand(0.8),
				e -> pure ? source != null && Fx.hostileTo(source).test(e)
						: source == null ? !(e instanceof CrystalKin) && e.isAlive() : BossFx.foesOf(source).test(e))) {
			if (e.getBoundingBox().expand(0.3).raycast(pos, next).isPresent()) {
				e.timeUntilRegen = 0;
				e.damage(world.getDamageSources().indirectMagic(this, source), damage);
				Fx.push(e, dir.multiply(0.6).add(0, 0.3, 0));
				if (!pure) {
					AmethystMob.infect(e, 100);
				}
				Fx.particles(world, ModParticles.PRISM_FLARE, e.getPos().add(0, 1, 0), 12, 0.3, 0.2);
				world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1.0f, 0.8f);
				discard();
				return;
			}
		}
		BlockHitResult hit = world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
		if (hit.getType() == HitResult.Type.BLOCK) {
			setPosition(hit.getPos().subtract(dir.multiply(0.4)));
			state = STUCK;
			if (!isPure()) {
				BossFx.spike(world, hit.getPos(), 0.7f, 22);
			}
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 1.0f, 0.7f);
			return;
		}
		setPosition(next);
		face(dir);
		if (++flown > 60) {
			discard();
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
