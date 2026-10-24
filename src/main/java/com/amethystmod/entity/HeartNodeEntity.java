package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * A weak point of the Heart: a glowing node at the foot of its roots. Hits on it are passed to
 * the Heart doubled (hits on the Heart's body are weakened). Not saved: the Heart recreates them.
 */
public class HeartNodeEntity extends Entity {
	private UUID heartId;
	private double offsetX;
	private double offsetY;
	private double offsetZ;

	public HeartNodeEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	public void attach(HeartEntity heart, double ox, double oy, double oz) {
		heartId = heart.getUuid();
		offsetX = ox;
		offsetY = oy;
		offsetZ = oz;
		setPosition(heart.getX() + ox, heart.getY() + oy, heart.getZ() + oz);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	private HeartEntity heart() {
		if (heartId != null && getWorld() instanceof ServerWorld world && world.getEntity(heartId) instanceof HeartEntity heart) {
			return heart;
		}
		return null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		HeartEntity heart = heart();
		if (heart == null || !heart.isAlive()) {
			discard();
			return;
		}
		setPosition(heart.getX() + offsetX, heart.getY() + offsetY, heart.getZ() + offsetZ);
		if (age % 6 == 0 && heart.isVulnerable()) {
			world.spawnParticles(ModParticles.PRISM_FLARE, getX(), getY() + 0.6, getZ(), 2, 0.35, 0.35, 0.35, 0.01);
		}
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		HeartEntity heart = heart();
		if (heart == null || isInvulnerableTo(source)) {
			return false;
		}
		return heart.hurtFromNode(source, amount);
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
