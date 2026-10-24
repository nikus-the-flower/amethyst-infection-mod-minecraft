package com.amethystmod;

import com.amethystmod.effect.ModEffects;
import com.amethystmod.item.Fx;
import com.amethystmod.item.ModArmorMaterials;
import com.amethystmod.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Passive artifact effects, armor set bonuses, timed abilities and the death/damage hooks. */
public class ArtifactEvents {
	public static final TagKey<EntityType<?>> AMETHYST_CREATURES =
			TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(AmethystMod.MOD_ID, "amethyst_creatures"));

	private static final Map<UUID, Integer> FLIGHT = new HashMap<>();
	private static final Set<UUID> FLIGHT_ENGAGED = new HashSet<>();
	private static final Map<UUID, Integer> BERSERK_CRASH = new HashMap<>();
	private static boolean reducingDamage;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ArtifactEvents::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			if (!FLIGHT.containsKey(player.getUuid())) {
				endFlight(player);
			}
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(ArtifactEvents::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(ArtifactEvents::allowDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register(ArtifactEvents::afterDeath);
	}

	// ---- inventory helpers -------------------------------------------------

	public static ItemStack findArtifact(ServerPlayerEntity player, Item item) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.isOf(item)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static int armorPieces(LivingEntity entity, RegistryEntry<?> material) {
		int count = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (entity.getEquippedStack(slot).getItem() instanceof ArmorItem armor && armor.getMaterial() == material) {
				count++;
			}
		}
		return count;
	}

	// ---- timed abilities ---------------------------------------------------

	/**
	 * Starts timed flight. The wearer is thrown into the air and switched to flying as soon as they
	 * leave the ground (a flying flag set while standing is cleared again by the game).
	 */
	public static void startFlight(ServerPlayerEntity player, int ticks) {
		FLIGHT.put(player.getUuid(), ticks);
		FLIGHT_ENGAGED.remove(player.getUuid());
		player.getAbilities().allowFlying = true;
		player.sendAbilitiesUpdate();
		Fx.push(player, new Vec3d(player.getVelocity().x, 0.9, player.getVelocity().z));
		player.fallDistance = 0;
	}

	private static void tickFlight(MinecraftServer server) {
		for (UUID id : FLIGHT.keySet()) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
			if (player == null) {
				continue;
			}
			if (!player.isOnGround() && !FLIGHT_ENGAGED.contains(id)) {
				FLIGHT_ENGAGED.add(id);
				player.getAbilities().allowFlying = true;
				player.getAbilities().flying = true;
				player.sendAbilitiesUpdate();
			}
			if (player.getAbilities().flying && server.getTicks() % 2 == 0) {
				ServerWorld world = player.getServerWorld();
				Vec3d back = player.getPos().add(player.getRotationVec(1.0f).multiply(-0.6)).add(0, 1.0, 0);
				world.spawnParticles(ModParticles.CRYSTAL_SPARK, back.x, back.y, back.z, 2, 0.35, 0.3, 0.35, 0.02);
			}
		}
	}

	private static void endFlight(ServerPlayerEntity player) {
		FLIGHT_ENGAGED.remove(player.getUuid());
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().allowFlying = false;
			player.getAbilities().flying = false;
			player.sendAbilitiesUpdate();
		}
	}

	public static void scheduleBerserkCrash(ServerPlayerEntity player, int ticks) {
		BERSERK_CRASH.put(player.getUuid(), ticks);
	}

	// ---- per-tick logic ----------------------------------------------------

	private static void tick(MinecraftServer server) {
		tickFlight(server);
		tickTimers(server, FLIGHT, (player) -> endFlight(player));
		tickTimers(server, BERSERK_CRASH, player -> {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 12, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 12, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 12, 2));
		});

		if (server.getTicks() % 10 != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			applyPassives(player);
		}
	}

	private static void tickTimers(MinecraftServer server, Map<UUID, Integer> timers,
								   java.util.function.Consumer<ServerPlayerEntity> onEnd) {
		Iterator<Map.Entry<UUID, Integer>> it = timers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			int left = entry.getValue() - 1;
			if (left <= 0 || !player.isAlive()) {
				it.remove();
				onEnd.accept(player);
			} else {
				entry.setValue(left);
			}
		}
	}

	private static void passive(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, int amplifier) {
		player.addStatusEffect(new StatusEffectInstance(effect, 60, amplifier, true, false, true));
	}

	/** Protected from the blight: the purifier's ward, or the purity amulet in the inventory. */
	public static boolean blightProof(LivingEntity entity) {
		return entity.hasStatusEffect(ModEffects.WARD)
				|| (entity instanceof ServerPlayerEntity player && !findArtifact(player, ModItems.PURITY_AMULET).isEmpty());
	}

	private static void applyPassives(ServerPlayerEntity player) {
		if (player.hasStatusEffect(ModEffects.BLIGHT) && !findArtifact(player, ModItems.PURITY_AMULET).isEmpty()) {
			player.removeStatusEffect(ModEffects.BLIGHT);
		}
		if (!findArtifact(player, ModItems.AEGIS_HEART).isEmpty()) {
			passive(player, StatusEffects.RESISTANCE, 0);
			passive(player, StatusEffects.ABSORPTION, 0);
		}
		if (!findArtifact(player, ModItems.BLINK_RING).isEmpty()) {
			passive(player, StatusEffects.SPEED, 0);
		}

		if (armorPieces(player, ModArmorMaterials.AMETHYST) == 4) {
			passive(player, ModEffects.RESONANCE, 0);
		}
		if (armorPieces(player, ModArmorMaterials.PURIFIED) == 4) {
			passive(player, ModEffects.WARD, 0);
		}
	}

	// ---- damage and death --------------------------------------------------

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (reducingDamage || !(entity instanceof ServerPlayerEntity)) {
			return true;
		}
		// Amethyst shield: whatever it stops gets a crystal burst back
		if (entity.isBlocking() && entity.getActiveItem().getItem() instanceof com.amethystmod.item.AmethystShieldItem
				&& source.getAttacker() instanceof LivingEntity attacker && attacker != entity
				&& entity.getWorld() instanceof ServerWorld world) {
			attacker.damage(world.getDamageSources().indirectMagic(entity, entity), 5.0f);
			Fx.push(attacker, attacker.getPos().subtract(entity.getPos()).multiply(1, 0, 1).normalize().multiply(0.9).add(0, 0.3, 0));
			Fx.particles(world, ModParticles.CRYSTAL_SPARK, attacker.getPos().add(0, attacker.getHeight() / 2, 0), 14, 0.4, 0.15);
			Fx.sound(world, entity, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.6f);
		}
		Entity attacker = source.getAttacker();
		if (attacker == null || !attacker.getType().isIn(AMETHYST_CREATURES)) {
			return true;
		}
		int pieces = armorPieces(entity, ModArmorMaterials.PURIFIED);
		boolean amulet = !findArtifact((ServerPlayerEntity) entity, ModItems.PURITY_AMULET).isEmpty();
		if (pieces == 0 && !amulet) {
			return true;
		}
		float reduction = pieces == 4 ? 0.8f : pieces * 0.15f;
		if (amulet) {
			reduction = 1.0f - (1.0f - reduction) * 0.75f;
		}
		reducingDamage = true;
		try {
			entity.damage(source, amount * (1.0f - reduction));
		} finally {
			reducingDamage = false;
		}
		return false;
	}

	/** Crystal Totem: cheats death once, with a shockwave. */
	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}
		ItemStack totem = findArtifact(player, ModItems.CRYSTAL_TOTEM);
		if (totem.isEmpty()) {
			return true;
		}
		totem.decrement(1);
		player.setHealth(10.0f);
		player.clearStatusEffects();
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 15, 2));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 10, 3));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 10, 0));

		ServerWorld world = player.getServerWorld();
		Vec3d center = player.getPos();
		for (LivingEntity e : Fx.around(world, center, 8, Fx.hostileTo(player))) {
			e.damage(world.getDamageSources().indirectMagic(player, player), 20.0f);
			Vec3d away = e.getPos().subtract(center).normalize().multiply(1.6).add(0, 0.5, 0);
			Fx.push(e, away);
		}
		Fx.particles(world, ModParticles.CRYSTAL_SPARK, center.add(0, 1, 0), 120, 3.0, 0.3);
		Fx.particles(world, ParticleTypes.TOTEM_OF_UNDYING, center.add(0, 1, 0), 80, 1.0, 0.5);
		Fx.sound(world, player, SoundEvents.ITEM_TOTEM_USE, 1.0f, 1.0f);
		Fx.sound(world, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.5f);
		return false;
	}

	/** Vampiric Shard: kills restore the killer's health. */
	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (source.getAttacker() instanceof ServerPlayerEntity player
				&& !findArtifact(player, ModItems.VAMPIRIC_SHARD).isEmpty()) {
			player.heal(Math.min(entity.getMaxHealth() * 0.25f, 10.0f));
			Fx.particles(player.getServerWorld(), ParticleTypes.HEART, player.getPos().add(0, 1.5, 0), 6, 0.4, 0.0);
		}
	}
}
