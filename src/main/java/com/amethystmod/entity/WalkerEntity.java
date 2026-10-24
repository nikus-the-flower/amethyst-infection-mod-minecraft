package com.amethystmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A tall crystal humanoid of the tower. Hurls fans of shards, then closes in. */
public class WalkerEntity extends AmethystMob {
	public WalkerEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		experiencePoints = 25;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 80)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9)
				.add(EntityAttributes.GENERIC_ARMOR, 6)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new AbilityGoals.ShootShards(this, 5, 18, 22, 90, 3, 6.0f, 6.0f, 1.1f));
		goalSelector.add(3, new MeleeAttackGoal(this, 1.0, false));
		initIdle();
		initTargets();
	}
}
