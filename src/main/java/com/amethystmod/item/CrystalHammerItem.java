package com.amethystmod.item;

import com.amethystmod.ModParticles;
import com.amethystmod.entity.BossFx;
import com.amethystmod.entity.Cutscenes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Crystal Hammer. Hold right-click: you leap up and hang in the air while the blow charges for
 * three seconds, then you dive into the ground. The impact hurls enemies away, crystals burst out
 * around you and every enemy hit heals you. Letting go early just drops you without a blow.
 */
public class CrystalHammerItem extends Item {
	public static final int CHARGE = 60;
	private static final int RISE = 7;
	private static final int COOLDOWN = 160;

	/** Players diving down after a full charge: ticks left before the blow lands anyway. */
	private static final Map<UUID, Integer> DIVING = new HashMap<>();
	/** Players who just landed a blow: no fall damage for a moment. */
	private static final Map<UUID, Integer> LANDED = new HashMap<>();

	public CrystalHammerItem(Settings settings) {
		super(settings);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CrystalHammerItem::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.isOf(DamageTypes.FALL) && (DIVING.containsKey(entity.getUuid()) || LANDED.containsKey(entity.getUuid()))));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this) || DIVING.containsKey(user.getUuid())) {
			return TypedActionResult.fail(stack);
		}
		user.setCurrentHand(hand);
		if (world instanceof ServerWorld serverWorld) {
			Fx.sound(serverWorld, user, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 1.6f);
		}
		return TypedActionResult.consume(stack);
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		// the movement runs on both sides: the client moves its own player
		Vec3d v = user.getVelocity();
		if (used <= RISE) {
			user.setVelocity(v.x * 0.6, 0.55, v.z * 0.6);
		} else {
			user.setVelocity(v.x * 0.5, 0.02 * Math.sin(used * 0.3), v.z * 0.5);
		}
		user.fallDistance = 0;
		if (used >= CHARGE) {
			user.stopUsingItem();
			return;
		}
		if (world instanceof ServerWorld serverWorld) {
			float frac = used / (float) CHARGE;
			if (used == 1) {
				BossFx.marker(serverWorld, user.getPos(), 7.0, CHARGE + 20);
			}
			if (used % 3 == 0) {
				Fx.particles(serverWorld, ModParticles.CRYSTAL_SPARK, user.getPos().add(0, 2.3, 0), 1 + (int) (frac * 3), 0.4, 0.05);
			}
			if (used % 15 == 0) {
				Fx.sound(serverWorld, user, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f + frac, 0.6f + frac);
			}
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (used < CHARGE) {
			// let go too early: just drop down, no blow
			if (user instanceof PlayerEntity player) {
				player.getItemCooldownManager().set(this, 20);
			}
			return;
		}
		Vec3d v = user.getVelocity();
		user.setVelocity(v.x * 0.3, -2.2, v.z * 0.3);
		if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
			DIVING.put(player.getUuid(), 40);
			player.getItemCooldownManager().set(this, COOLDOWN);
			stack.damage(2, player, player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			Fx.sound(serverWorld, player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(), 1.5f, 0.6f);
		}
	}

	private static void tick(MinecraftServer server) {
		LANDED.replaceAll((id, t) -> t - 1);
		LANDED.values().removeIf(t -> t <= 0);
		Iterator<Map.Entry<UUID, Integer>> it = DIVING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> e = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			int left = e.getValue() - 1;
			e.setValue(left);
			player.fallDistance = 0;
			if (player.isOnGround() || player.isTouchingWater() || left <= 0) {
				it.remove();
				LANDED.put(player.getUuid(), 10);
				impact(player.getServerWorld(), player);
			}
		}
	}

	private static void impact(ServerWorld world, ServerPlayerEntity player) {
		Vec3d center = player.getPos();
		int hits = 0;
		for (LivingEntity e : Fx.around(world, center, 7, Fx.hostileTo(player))) {
			double dist = Math.sqrt(e.squaredDistanceTo(center));
			e.damage(world.getDamageSources().playerAttack(player), 18.0f * (float) (1.0 - 0.4 * dist / 7));
			Vec3d away = e.getPos().subtract(center).multiply(1, 0, 1);
			Vec3d dir = away.lengthSquared() < 0.01 ? Vec3d.ZERO : away.normalize();
			Fx.push(e, dir.multiply(1.6).add(0, 0.8, 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
			hits++;
		}
		player.heal(4.0f + Math.min(5, hits) * 2.0f);
		BossFx.spike(world, center, 1.2f, 24);
		BossFx.spikeRing(world, center, 3.5, 6, 0.9f);
		BossFx.spikeRing(world, center, 6.0, 9, 0.7f);
		Fx.particles(world, ModParticles.PRISM_FLARE, center.add(0, 0.6, 0), 12, 2.5, 0.1);
		Fx.particles(world, net.minecraft.particle.ParticleTypes.HEART, center.add(0, 2.0, 0), 3, 0.4, 0.1);
		Cutscenes.shake(world, center, 10, 2.0f, 10);
		Fx.sound(world, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.4f, 0.8f);
		Fx.sound(world, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.5f, 0.5f);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
