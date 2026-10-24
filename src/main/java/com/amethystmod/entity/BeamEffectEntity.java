package com.amethystmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A 3D beam of light drawn by the client, whose shape is exactly the hitbox the server uses:
 * <ul>
 * <li>AIM / FIRE: a beam from this point toward (yaw, pitch); thin while aiming, thick when it fires.</li>
 * <li>SWEEP: a low beam lying on the floor, turning around this point like the hand of a clock
 * after a delay; jump over it.</li>
 * <li>RING: a glowing wall in a circle that grows from this point after a delay; jump over it.</li>
 * <li>WHIRL: crescent slashes of light circling an entity (a spear whirl).</li>
 * <li>DOME: a see-through sphere of light around an entity (a shield).</li>
 * </ul>
 * WHIRL and DOME follow their owner entity on the client, so they stay on it smoothly.
 * Swept and ring shapes are computed from the entity's age on both sides, so they move smoothly
 * and the server checks hits against the very same shape. No hitbox, no physics, not saved.
 */
public class BeamEffectEntity extends Entity {
	public static final int AIM = 0;
	public static final int FIRE = 1;
	public static final int SWEEP = 2;
	public static final int RING = 3;
	public static final int WHIRL = 4;
	public static final int DOME = 5;
	/** Colours: the violet of the Amethyst, or the white-blue of purified crystal. */
	public static final int AMETHYST = 0;
	public static final int PURE = 1;

	private static final TrackedData<Integer> MODE = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> LIFE = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> LENGTH = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> WIDTH = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> ANGLE = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> PITCH = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SPEED = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> DELAY = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> OWNER = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> COLOR = DataTracker.registerData(BeamEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** Last tick's direction, for smooth drawing of aimed beams. */
	public float prevAngle;
	public float prevPitch;

	public BeamEffectEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	private static BeamEffectEntity create(ServerWorld world, Vec3d pos, int mode, int life) {
		BeamEffectEntity e = ModEntities.BEAM_EFFECT.create(world);
		if (e == null) {
			return null;
		}
		e.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
		e.dataTracker.set(MODE, mode);
		e.dataTracker.set(LIFE, life);
		return e;
	}

	/** A beam toward {@code dir}; starts aiming (thin). Keep it alive and turn it with {@link #aim}. */
	public static BeamEffectEntity beam(ServerWorld world, Vec3d from, Vec3d dir, float length, int life) {
		BeamEffectEntity e = create(world, from, AIM, life);
		if (e == null) {
			return null;
		}
		e.dataTracker.set(LENGTH, length);
		e.dataTracker.set(WIDTH, 0.07f);
		e.aim(dir);
		e.prevAngle = e.getAngle();
		e.prevPitch = e.getBeamPitch();
		world.spawnEntity(e);
		return e;
	}

	/** A floor beam from {@code inner} to {@code length} blocks out, turning by {@code speed} rad/tick after {@code delay}. */
	public static BeamEffectEntity sweep(ServerWorld world, Vec3d center, float inner, float length, float halfWidth,
										 float angle, float speed, int delay, int life) {
		BeamEffectEntity e = create(world, center, SWEEP, life);
		if (e == null) {
			return null;
		}
		e.dataTracker.set(LENGTH, length);
		e.dataTracker.set(WIDTH, halfWidth);
		e.dataTracker.set(ANGLE, angle);
		e.dataTracker.set(PITCH, inner);
		e.dataTracker.set(SPEED, speed);
		e.dataTracker.set(DELAY, delay);
		world.spawnEntity(e);
		return e;
	}

	/** A ring wall growing from {@code start} blocks by {@code speed} blocks a tick after {@code delay}. */
	public static BeamEffectEntity ring(ServerWorld world, Vec3d center, float start, float speed, float height, int delay, int life) {
		BeamEffectEntity e = create(world, center, RING, life);
		if (e == null) {
			return null;
		}
		e.dataTracker.set(LENGTH, start);
		e.dataTracker.set(SPEED, speed);
		e.dataTracker.set(WIDTH, height);
		e.dataTracker.set(DELAY, delay);
		world.spawnEntity(e);
		return e;
	}

	/** Crescent slashes of radius {@code radius} circling {@code owner} at {@code speed} rad/tick. */
	public static BeamEffectEntity whirl(ServerWorld world, Entity owner, float radius, float speed, int life, int color) {
		BeamEffectEntity e = create(world, owner.getPos(), WHIRL, life);
		if (e == null) {
			return null;
		}
		e.dataTracker.set(LENGTH, radius);
		e.dataTracker.set(SPEED, speed);
		e.dataTracker.set(OWNER, owner.getId());
		e.dataTracker.set(COLOR, color);
		world.spawnEntity(e);
		return e;
	}

	/** A see-through sphere of radius {@code radius} around {@code owner}; keep it with {@link #keepAlive}. */
	public static BeamEffectEntity dome(ServerWorld world, Entity owner, float radius, int life, int color) {
		BeamEffectEntity e = create(world, owner.getPos(), DOME, life);
		if (e == null) {
			return null;
		}
		e.dataTracker.set(LENGTH, radius);
		e.dataTracker.set(OWNER, owner.getId());
		e.dataTracker.set(COLOR, color);
		world.spawnEntity(e);
		return e;
	}

	/** A small fast ring (a burst), in a colour. */
	public static BeamEffectEntity burst(ServerWorld world, Vec3d center, float start, float speed, float height, int life, int color) {
		BeamEffectEntity e = ring(world, center, start, speed, height, 0, life);
		if (e != null) {
			e.dataTracker.set(COLOR, color);
		}
		return e;
	}

	/** Lives {@code ticks} more ticks from now. */
	public void keepAlive(int ticks) {
		dataTracker.set(LIFE, age + ticks);
	}

	/** Moves a beam to a new start point (a staff beam follows its holder). */
	public void moveTo(Vec3d pos) {
		setPosition(pos);
	}

	public float getSpeed() {
		return dataTracker.get(SPEED);
	}

	public int getOwnerId() {
		return dataTracker.get(OWNER);
	}

	public int getColor() {
		return dataTracker.get(COLOR);
	}

	public void aim(Vec3d dir) {
		double h = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		dataTracker.set(ANGLE, (float) Math.atan2(dir.z, dir.x));
		dataTracker.set(PITCH, (float) Math.atan2(dir.y, h));
	}

	public void fire(float width) {
		dataTracker.set(MODE, FIRE);
		dataTracker.set(WIDTH, width);
	}

	public void setLength(float length) {
		dataTracker.set(LENGTH, length);
	}

	public void setLife(int life) {
		dataTracker.set(LIFE, life);
	}

	public int getMode() {
		return dataTracker.get(MODE);
	}

	public int getLife() {
		return dataTracker.get(LIFE);
	}

	public float getLength() {
		return dataTracker.get(LENGTH);
	}

	public float getBeamWidth() {
		return dataTracker.get(WIDTH);
	}

	public float getAngle() {
		return dataTracker.get(ANGLE);
	}

	public float getBeamPitch() {
		return dataTracker.get(PITCH);
	}

	/** For a sweep: where the beam starts, blocks from the centre. */
	public float getInner() {
		return dataTracker.get(PITCH);
	}

	public int getDelay() {
		return dataTracker.get(DELAY);
	}

	/** Direction of an aimed beam. */
	public Vec3d direction() {
		float yaw = getAngle();
		float pitch = getBeamPitch();
		return new Vec3d(MathHelper.cos(pitch) * MathHelper.cos(yaw), MathHelper.sin(pitch), MathHelper.cos(pitch) * MathHelper.sin(yaw));
	}

	/** Ticks since the delay ended (with the partial tick on the client). */
	public float active(float tickDelta) {
		return Math.max(0, age + tickDelta - getDelay());
	}

	/** The turning angle of a sweep beam (radians). */
	public float sweepAngle(float tickDelta) {
		return getAngle() + dataTracker.get(SPEED) * active(tickDelta);
	}

	/** The radius of a ring. */
	public float ringRadius(float tickDelta) {
		return getLength() + dataTracker.get(SPEED) * active(tickDelta);
	}

	public boolean started() {
		return age >= getDelay();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(MODE, AIM);
		builder.add(LIFE, 20);
		builder.add(LENGTH, 10.0f);
		builder.add(WIDTH, 0.5f);
		builder.add(ANGLE, 0.0f);
		builder.add(PITCH, 0.0f);
		builder.add(SPEED, 0.0f);
		builder.add(DELAY, 0);
		builder.add(OWNER, -1);
		builder.add(COLOR, AMETHYST);
	}

	@Override
	public void tick() {
		prevAngle = getAngle();
		prevPitch = getBeamPitch();
		super.tick();
		if (getOwnerId() >= 0) {
			Entity owner = getWorld().getEntityById(getOwnerId());
			if (owner != null) {
				setPosition(owner.getPos());
			} else if (!getWorld().isClient) {
				discard();
				return;
			}
		}
		if (!getWorld().isClient && age >= getLife()) {
			discard();
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
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
