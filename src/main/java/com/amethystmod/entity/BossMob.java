package com.amethystmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/** A boss: boss bar, no despawning and health that grows with the number of players nearby. */
public abstract class BossMob extends AmethystMob {
	protected final ServerBossBar bar;
	private final double baseHealth;
	private int lastPlayerCount = 1;

	protected BossMob(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		bar = new ServerBossBar(getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
		baseHealth = getMaxHealth();
		experiencePoints = 500;
		setPersistent();
	}

	/** Players currently in the fight. */
	protected java.util.List<ServerPlayerEntity> fighters(double range) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return java.util.List.of();
		}
		return BossFx.playersNear(world, getPos(), range);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bar.setPercent(getHealth() / getMaxHealth());
		if (age % 40 == 0) {
			int count = Math.max(1, fighters(64).size());
			lastPlayerCount = count;
		}
	}

	protected float damageScale() {
		return (float) (1.0 + 0.7 * (lastPlayerCount - 1));
	}

	/**
	 * More players, tougher boss: every extra player adds 70% effective health. Health itself is
	 * capped at 1024 by the game, so incoming damage is divided instead.
	 */
	@Override
	public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
		if (!source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) {
			amount /= damageScale();
		}
		return super.damage(source, amount);
	}

	@Override
	public void onStartedTrackingBy(ServerPlayerEntity player) {
		super.onStartedTrackingBy(player);
		bar.addPlayer(player);
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bar.removePlayer(player);
	}

	@Override
	public void setCustomName(Text name) {
		super.setCustomName(name);
		bar.setName(getDisplayName());
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		bar.clearPlayers();
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}
}
