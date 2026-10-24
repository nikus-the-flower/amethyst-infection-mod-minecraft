package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import com.amethystmod.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** An arrow with a crystal tip that shatters on impact and cuts everything around the target. */
public class CrystalArrowEntity extends PersistentProjectileEntity {
	public CrystalArrowEntity(EntityType<? extends CrystalArrowEntity> type, World world) {
		super(type, world);
	}

	public CrystalArrowEntity(World world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CRYSTAL_ARROW, x, y, z, world, stack, shotFrom);
	}

	public CrystalArrowEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(ModEntities.CRYSTAL_ARROW, owner, world, stack, shotFrom);
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(ModItems.CRYSTAL_ARROW);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && !inGround && age % 2 == 0) {
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		shatter(hit.getPos(), hit.getEntity() instanceof LivingEntity l ? l : null);
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		shatter(hit.getPos(), null);
	}

	private void shatter(Vec3d at, @Nullable LivingEntity direct) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		LivingEntity shooter = getOwner() instanceof LivingEntity l ? l : null;
		for (LivingEntity e : Fx.around(world, at, 3.0, t -> t != shooter && t != direct && t.isAlive() && !(t instanceof CrystalKin))) {
			e.damage(world.getDamageSources().arrow(this, shooter), 5.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0));
		}
		Fx.particles(world, ModParticles.PRISM_FLARE, at, 14, 0.4, 0.12);
		Fx.particles(world, ParticleTypes.CRIT, at, 10, 0.5, 0.3);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, getSoundCategory(), 1.0f, 1.4f);
		discard();
	}
}
