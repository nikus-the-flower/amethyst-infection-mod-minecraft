package com.amethystmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A giant geode golem. Slow, nearly unmovable, and it slams the ground when you get close. */
public class ColossusEntity extends AmethystMob {
	public ColossusEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 240)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12)
				.add(EntityAttributes.GENERIC_ARMOR, 12)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new AbilityGoals.Slam(this, 28, 110, 9.0, 9.0f));
		goalSelector.add(3, new MeleeAttackGoal(this, 1.0, false));
		initIdle();
		initTargets();
	}
}
