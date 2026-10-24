package com.amethystmod.entity;

import com.amethystmod.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Base of every creature of the amethyst: crystal sounds, the blight on hit, and an action timer for animations. */
public abstract class AmethystMob extends HostileEntity implements CrystalKin {
	private static final TrackedData<Integer> ACTION = DataTracker.registerData(AmethystMob.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> ANIM = DataTracker.registerData(AmethystMob.class, TrackedDataHandlerRegistry.INTEGER);

	// Animation poses the models know (see GeoModel). ACTION counts the remaining ticks of the pose.
	public static final int ANIM_RAISE = 0;
	public static final int ANIM_THRUST = 1;
	public static final int ANIM_CAST = 2;
	public static final int ANIM_SLAM = 3;
	public static final int ANIM_SPREAD = 4;
	public static final int ANIM_CROUCH = 5;
	public static final int ANIM_KNEEL = 6;
	public static final int ANIM_SPIN = 7;

	/** The ability goal used last, so the same one is rarely chosen twice in a row. */
	public Object lastAbility;

	protected AmethystMob(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ACTION, 0);
		builder.add(ANIM, 0);
	}

	/** Remaining ticks of the current ability animation (0 = none). Synced to the client for the model. */
	public int getAction() {
		return dataTracker.get(ACTION);
	}

	public void setAction(int ticks) {
		dataTracker.set(ACTION, ticks);
	}

	public int getAnim() {
		return dataTracker.get(ANIM);
	}

	/** Starts a pose for {@code ticks} ticks. */
	public void play(int anim, int ticks) {
		dataTracker.set(ANIM, anim);
		dataTracker.set(ACTION, ticks);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && getAction() > 0) {
			setAction(getAction() - 1);
		}
	}

	public static void infect(LivingEntity target, int ticks) {
		if (!com.amethystmod.ArtifactEvents.blightProof(target)) {
			// the blight lasts a little over half as long as its source says: it should hurt, not cripple
			target.addStatusEffect(new StatusEffectInstance(ModEffects.BLIGHT, ticks * 6 / 10));
		}
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity living) {
			infect(living, 200);
		}
		return hit;
	}

	/** Attack players and whoever hurt us; creatures of the amethyst never fight each other. */
	protected void initTargets() {
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	protected void initIdle() {
		goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
		goalSelector.add(8, new LookAroundGoal(this));
	}

	/** The infection glows; amethyst creatures do not need darkness to appear or to walk somewhere. */
	@Override
	public float getPathfindingFavor(net.minecraft.util.math.BlockPos pos, net.minecraft.world.WorldView world) {
		return 0.0f;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
	}
}
