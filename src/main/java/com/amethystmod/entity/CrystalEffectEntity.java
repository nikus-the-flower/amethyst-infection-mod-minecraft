package com.amethystmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A short-lived 3D visual: a crystal spike bursting out of the floor (CRYSTAL_SPIKE), a glowing
 * rune circle warning of an attack (RUNE_CIRCLE), or a crystal falling from the sky that lands
 * {@link #FALL_TICKS} after it appears (FALLING_CRYSTAL). No hitbox, no physics, not saved.
 */
public class CrystalEffectEntity extends Entity {
	public static final int FALL_TICKS = 12;
	private static final TrackedData<Float> SIZE = DataTracker.registerData(CrystalEffectEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> LIFE = DataTracker.registerData(CrystalEffectEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public CrystalEffectEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	public static void spawn(ServerWorld world, EntityType<CrystalEffectEntity> type, Vec3d pos, float size, int life) {
		CrystalEffectEntity e = type.create(world);
		if (e == null) {
			return;
		}
		e.refreshPositionAndAngles(pos.x, pos.y, pos.z, world.random.nextFloat() * 360, 0);
		e.dataTracker.set(SIZE, size);
		e.dataTracker.set(LIFE, life);
		world.spawnEntity(e);
	}

	public float getSize() {
		return dataTracker.get(SIZE);
	}

	public int getLife() {
		return dataTracker.get(LIFE);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(SIZE, 1.0f);
		builder.add(LIFE, 20);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && age >= getLife()) {
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
