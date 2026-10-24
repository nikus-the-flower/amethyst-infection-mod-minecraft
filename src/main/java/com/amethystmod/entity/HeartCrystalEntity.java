package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * A crystal of the Heart. Two kinds share this class:
 * <ul>
 * <li>pylon (HEART_PYLON): five of them stand on pedestals around the arena and shield the Heart;</li>
 * <li>sentinel (HEART_SENTINEL): summoned later; floats in place or orbits the Heart, shoots, but gives no shield.</li>
 * </ul>
 */
public class HeartCrystalEntity extends AmethystMob {
	private int shootCooldown = 40;
	private UUID orbitHeart;
	private float orbitAngle;
	private float orbitRadius;
	private float orbitHeight;

	public HeartCrystalEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder pylonAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 70)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
	}

	public static DefaultAttributeContainer.Builder sentinelAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 2)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
	}

	public boolean isPylon() {
		return getType() == ModEntities.HEART_PYLON;
	}

	/** Makes this sentinel circle the given Heart. */
	public void orbit(HeartEntity heart, float angle, float radius, float height) {
		orbitHeart = heart.getUuid();
		orbitAngle = angle;
		orbitRadius = radius;
		orbitHeight = height;
	}

	@Override
	protected void initGoals() {
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return false;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof CrystalKin) {
			return false;
		}
		// a sentinel shatters from any single hit
		if (!isPylon() && amount > 0) {
			return super.damage(source, Math.max(amount, getHealth() + 1));
		}
		return super.damage(source, amount);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		setVelocity(Vec3d.ZERO);
		if (orbitHeart != null) {
			if (world.getEntity(orbitHeart) instanceof HeartEntity heart && heart.isAlive()) {
				orbitAngle += 1.4f;
				double a = Math.toRadians(orbitAngle);
				setPosition(heart.getX() + Math.cos(a) * orbitRadius, heart.getY() + orbitHeight + MathHelper.sin(age * 0.1f) * 0.6,
						heart.getZ() + Math.sin(a) * orbitRadius);
			} else {
				discard();
				return;
			}
		}
		if (age % 4 == 0) {
			world.spawnParticles(isPylon() ? ModParticles.CRYSTAL_SPARK : ModParticles.PRISM_FLARE,
					getX(), getY() + getHeight() * 0.6, getZ(), 1, 0.4, 0.6, 0.4, 0.01);
		}
		if (--shootCooldown > 0) {
			return;
		}
		shootCooldown = (isPylon() ? 70 : 45) + random.nextInt(30);
		List<ServerPlayerEntity> near = BossFx.playersNear(world, getPos(), 34);
		if (near.isEmpty()) {
			return;
		}
		ServerPlayerEntity target = near.get(random.nextInt(near.size()));
		Vec3d from = getPos().add(0, getHeight() * 0.7, 0);
		Vec3d aim = target.getEyePos().subtract(from);
		CrystalShardEntity shard = new CrystalShardEntity(world, this, isPylon() ? 6.0f : 7.0f);
		shard.setPosition(from.x, from.y, from.z);
		shard.setVelocity(aim.x, aim.y, aim.z, 1.0f, 3.0f);
		world.spawnEntity(shard);
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.6f);
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld world) {
			Fx.particles(world, ModParticles.PRISM_FLARE, getPos().add(0, 1, 0), 40, 0.8, 0.3);
			Fx.particles(world, ParticleTypes.EXPLOSION, getPos().add(0, 1, 0), 1, 0.0, 0.0);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.5f);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (orbitHeart != null) {
			nbt.putUuid("OrbitHeart", orbitHeart);
			nbt.putFloat("OrbitAngle", orbitAngle);
			nbt.putFloat("OrbitRadius", orbitRadius);
			nbt.putFloat("OrbitHeight", orbitHeight);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("OrbitHeart")) {
			orbitHeart = nbt.getUuid("OrbitHeart");
			orbitAngle = nbt.getFloat("OrbitAngle");
			orbitRadius = nbt.getFloat("OrbitRadius");
			orbitHeight = nbt.getFloat("OrbitHeight");
		}
	}
}
