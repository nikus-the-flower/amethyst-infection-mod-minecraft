package com.amethystmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A fast raptor of the tower with sickle claws that leaps onto its prey. */
public class LurkerEntity extends AmethystMob {
	public LurkerEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 18;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 46)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7)
				.add(EntityAttributes.GENERIC_ARMOR, 3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new PounceAtTargetGoal(this, 0.62f));
		goalSelector.add(3, new MeleeAttackGoal(this, 1.3, true));
		initIdle();
		initTargets();
	}
}
