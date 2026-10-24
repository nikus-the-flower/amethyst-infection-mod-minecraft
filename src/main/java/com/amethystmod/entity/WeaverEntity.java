package com.amethystmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A crystal spider that climbs walls and ceilings of the caves. */
public class WeaverEntity extends AmethystMob {
	private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(WeaverEntity.class, TrackedDataHandlerRegistry.BYTE);

	public WeaverEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 26)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
				.add(EntityAttributes.GENERIC_ARMOR, 3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CLIMBING, (byte) 0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new SpiderNavigation(this, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new PounceAtTargetGoal(this, 0.4f));
		goalSelector.add(3, new MeleeAttackGoal(this, 1.15, true));
		initIdle();
		initTargets();
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient) {
			dataTracker.set(CLIMBING, (byte) (horizontalCollision ? 1 : 0));
		}
	}

	@Override
	public boolean isClimbing() {
		return dataTracker.get(CLIMBING) != 0;
	}
}
