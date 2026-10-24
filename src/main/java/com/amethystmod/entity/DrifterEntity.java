package com.amethystmod.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/** A winged, tentacled flyer of the tower. Hovers above its prey and rains shards from a distance. */
public class DrifterEntity extends AmethystMob {
	public DrifterEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 45;
		moveControl = new FlightMoveControl(this, 20, true);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 100)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(true);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new AbilityGoals.ShootShards(this, 4, 26, 18, 55, 5, 10.0f, 7.0f, 1.3f));
		goalSelector.add(2, new Hover(this));
		goalSelector.add(6, new FlyGoal(this, 1.0));
		goalSelector.add(8, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
		initTargets();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	/** Keeps a distance from the target and floats above it. */
	private static class Hover extends Goal {
		private final DrifterEntity mob;

		Hover(DrifterEntity mob) {
			this.mob = mob;
			setControls(EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return mob.getTarget() != null && mob.getTarget().isAlive();
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			if (target == null) {
				return;
			}
			Vec3d away = mob.getPos().subtract(target.getPos());
			away = new Vec3d(away.x, 0, away.z);
			if (away.lengthSquared() < 0.01) {
				away = new Vec3d(1, 0, 0);
			}
			Vec3d spot = target.getPos().add(away.normalize().multiply(10)).add(0, 5.5, 0);
			mob.getMoveControl().moveTo(spot.x, spot.y, spot.z, 1.0);
			mob.getLookControl().lookAt(target, 30.0f, 30.0f);
		}
	}
}
