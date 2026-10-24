package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A fast straight-flying crystal shard shot by the creatures and bosses. */
public class CrystalShardEntity extends ThrownItemEntity {
	private float damage = 6.0f;

	public CrystalShardEntity(EntityType<? extends ThrownItemEntity> type, World world) {
		super(type, world);
	}

	public CrystalShardEntity(World world, LivingEntity owner, float damage) {
		super(ModEntities.CRYSTAL_SHARD, owner, world);
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.CRYSTAL_SHARD;
	}

	@Override
	protected double getGravity() {
		return 0.0;
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !(entity instanceof CrystalKin);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
			if (age > 100) {
				discard();
			}
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		Entity target = hit.getEntity();
		if (target.damage(getDamageSources().indirectMagic(this, getOwner()), damage) && target instanceof LivingEntity living) {
			AmethystMob.infect(living, 100);
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ModParticles.PRISM_FLARE, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 0.8f, 1.5f);
			discard();
		}
	}
}
