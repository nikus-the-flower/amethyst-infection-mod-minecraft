package com.amethystmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A wolf-like crystal hunter that pounces. Roams the infection on the surface and underground. */
public class StalkerEntity extends AmethystMob {
	public StalkerEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 34)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new PounceAtTargetGoal(this, 0.45f));
		goalSelector.add(3, new MeleeAttackGoal(this, 1.2, true));
		initIdle();
		initTargets();
	}
}
