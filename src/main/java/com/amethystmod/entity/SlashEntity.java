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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A crescent of light cut loose from a sword: it flies straight ahead, passes through enemies (each is
 * cut once) and breaks on walls. Not saved.
 */
public class SlashEntity extends Entity {
	private static final TrackedData<Float> SIZE = DataTracker.registerData(SlashEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> ROLL = DataTracker.registerData(SlashEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public static final int LIFE = 24;
	private static final double SPEED = 1.0;

	private UUID ownerId;
	private float damage;
	private Vec3d dir = new Vec3d(1, 0, 0);
	private final Set<Integer> cut = new HashSet<>();

	public SlashEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	public static void spawn(ServerWorld world, PlayerEntity owner, Vec3d from, Vec3d dir, float size, float roll, float damage) {
		SlashEntity slash = ModEntities.SLASH.create(world);
		if (slash == null) {
			return;
		}
		slash.ownerId = owner.getUuid();
		slash.damage = damage;
		slash.dir = dir.normalize();
		slash.dataTracker.set(SIZE, size);
		slash.dataTracker.set(ROLL, roll);
		slash.refreshPositionAndAngles(from.x, from.y, from.z, 0, 0);
		slash.face();
		world.spawnEntity(slash);
	}

	private void face() {
		double h = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		setYaw((float) (MathHelper.atan2(dir.x, dir.z) * 57.2957795));
		setPitch((float) (MathHelper.atan2(dir.y, h) * 57.2957795));
	}

	public float getSize() {
		return dataTracker.get(SIZE);
	}

	public float getRoll() {
		return dataTracker.get(ROLL);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(SIZE, 1.5f);
		builder.add(ROLL, 0.0f);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (age > LIFE) {
			discard();
			return;
		}
		Vec3d pos = getPos();
		Vec3d next = pos.add(dir.multiply(SPEED));
		PlayerEntity owner = ownerId == null ? null : world.getPlayerByUuid(ownerId);
		double reach = getSize() * 0.85;
		Box box = new Box(pos, next).expand(reach, 0.6, reach);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box,
				e -> owner == null ? e.isAlive() : Fx.hostileTo(owner).test(e))) {
			if (cut.add(e.getId())) {
				DamageSource source = owner == null ? world.getDamageSources().magic() : world.getDamageSources().playerAttack(owner);
				e.timeUntilRegen = 0;
				e.damage(source, damage);
				Fx.push(e, dir.multiply(0.45).add(0, 0.2, 0));
				Fx.particles(world, ModParticles.CRYSTAL_SPARK, e.getPos().add(0, e.getHeight() / 2, 0), 6, 0.3, 0.15);
			}
		}
		if (world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this))
				.getType() == HitResult.Type.BLOCK) {
			Fx.particles(world, ModParticles.PRISM_FLARE, pos, 8, 0.4, 0.1);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.6f, 1.4f);
			discard();
			return;
		}
		setPosition(next);
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
