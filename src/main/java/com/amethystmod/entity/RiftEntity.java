package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.world.AmethystWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A vertical tear in space. Stand in it for a moment and it carries you between the overworld (the
 * hall of the tower) and the world of the Amethyst. It is not saved: {@link RiftManager} keeps one
 * wherever the story has opened it.
 */
public class RiftEntity extends Entity {
	/** Ticks a player has to stand in the rift before it takes them. */
	private static final int ENTER_TICKS = 24;
	public static final int OPEN_TICKS = 50;
	/** Ticks since the rift started to open (it tears open when first created by a victory). */
	private static final TrackedData<Integer> OPENING = DataTracker.registerData(RiftEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private final Map<UUID, Integer> inside = new HashMap<>();

	public RiftEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	/** Plays the tearing-open animation (otherwise the rift appears already open). */
	public void tearOpen() {
		dataTracker.set(OPENING, 0);
	}

	/** 0 just torn .. 1 fully open. */
	public float openness(float tickDelta) {
		int opening = dataTracker.get(OPENING);
		if (opening < 0) {
			return 1.0f;
		}
		return Math.min(1.0f, (age + tickDelta - opening) / OPEN_TICKS);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(OPENING, -1);
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		if (world.isClient) {
			if (random.nextInt(3) == 0) {
				world.addParticle(ParticleTypes.PORTAL, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 4.5,
						getZ() + (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 0.6, -random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
			}
			if (random.nextInt(6) == 0) {
				world.addParticle(ModParticles.CRYSTAL_SPARK, getX(), getY() + random.nextDouble() * 4.5, getZ(),
						(random.nextDouble() - 0.5) * 0.15, 0.02, (random.nextDouble() - 0.5) * 0.15);
			}
			return;
		}
		ServerWorld server = (ServerWorld) world;
		if (age % 60 == 0) {
			server.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, 0.6f, 0.6f);
		}
		if (openness(0) < 1.0f) {
			return;
		}
		Map<UUID, Integer> now = new HashMap<>();
		for (ServerPlayerEntity player : server.getEntitiesByClass(ServerPlayerEntity.class, getBoundingBox(), p -> p.isAlive() && !p.isSpectator())) {
			if (player.hasPortalCooldown()) {
				continue;
			}
			int ticks = inside.getOrDefault(player.getUuid(), 0) + 1;
			now.put(player.getUuid(), ticks);
			if (ticks == 1) {
				server.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.5f, 1.4f);
			}
			if (ticks >= ENTER_TICKS) {
				TeleportTarget target = AmethystWorld.portalTarget(server, player);
				if (target != null) {
					player.setPortalCooldown(80);
					player.teleportTo(target);
				}
				now.remove(player.getUuid());
			}
		}
		inside.clear();
		inside.putAll(now);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
