package com.amethystmod.item;

import com.amethystmod.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Jaybee Talis's Hammer. Hold use to charge for up to five seconds (the hammer crackles and the
 * air hums), release to smash the floor: a thunderclap of electric damage that hurls everything
 * around away. The longer the charge, the wider, harder and louder the blow.
 */
public class TalisHammerItem extends Item {
	public static final int MAX_CHARGE = 100;
	private static final int MIN_CHARGE = 10;

	public TalisHammerItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(user.getStackInHand(hand));
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
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
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		int used = Math.min(MAX_CHARGE, getMaxUseTime(stack, user) - remainingUseTicks);
		float frac = used / (float) MAX_CHARGE;
		Vec3d top = user.getPos().add(0, 2.0, 0);
		if (used % 2 == 0) {
			serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y, top.z, 2 + (int) (frac * 8),
					0.4 + frac * 0.8, 0.5, 0.4 + frac * 0.8, 0.15);
		}
		if (used % 20 == 0 && used > 0) {
			Fx.sound(serverWorld, user, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8f + frac, 0.6f + frac * 1.2f);
		}
		if (used == MAX_CHARGE) {
			Fx.particles(serverWorld, ModParticles.PRISM_FLARE, top, 12, 0.8, 0.2);
			if (user.age % 10 == 0) {
				Fx.sound(serverWorld, user, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
			}
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof PlayerEntity player)) {
			return;
		}
		int used = Math.min(MAX_CHARGE, getMaxUseTime(stack, user) - remainingUseTicks);
		if (used < MIN_CHARGE) {
			return;
		}
		float frac = used / (float) MAX_CHARGE;
		slam(serverWorld, player, frac);
		player.swingHand(player.getActiveHand());
		player.getItemCooldownManager().set(this, 60 + Math.round(frac * 100));
		stack.damage(2 + Math.round(frac * 4), player, player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
	}

	private static void slam(ServerWorld world, PlayerEntity user, float frac) {
		Vec3d center = user.getPos();
		double radius = 4.0 + 8.0 * frac;
		float damage = 8.0f + 32.0f * frac;

		for (LivingEntity e : Fx.around(world, center, radius, t -> t != user && t.isAlive() && !t.isSpectator())) {
			double dist = e.getPos().distanceTo(center);
			float scaled = damage * (float) (1.0 - 0.5 * dist / radius);
			e.damage(world.getDamageSources().lightningBolt(), scaled);
			Vec3d away = e.getPos().subtract(center).multiply(1, 0, 1);
			Vec3d dir = away.lengthSquared() < 0.01 ? new Vec3d(0, 0, 0) : away.normalize();
			Fx.push(e, dir.multiply(0.8 + 1.7 * frac).add(0, 0.5 + 0.5 * frac, 0));
			Fx.particles(world, ParticleTypes.ELECTRIC_SPARK, e.getPos().add(0, e.getHeight() / 2, 0), 20, 0.4, 0.4);
		}

		int bolts = 1 + Math.round(frac * 4);
		for (int i = 0; i < bolts; i++) {
			double a = Math.PI * 2 * i / bolts;
			double r = i == 0 ? 0 : radius * 0.55;
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				bolt.setCosmetic(true);
				bolt.refreshPositionAfterTeleport(center.x + Math.cos(a) * r, center.y, center.z + Math.sin(a) * r);
				world.spawnEntity(bolt);
			}
		}
		for (double ring = 1.5; ring <= radius; ring += 1.5) {
			int points = (int) (ring * 6) + 6;
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x + Math.cos(a) * ring, center.y + 0.2,
						center.z + Math.sin(a) * ring, 1, 0.0, 0.3, 0.0, 0.25);
			}
		}
		Fx.particles(world, ParticleTypes.EXPLOSION, center, 1 + (int) (frac * 3), 0.5, 0.0);
		Fx.particles(world, ModParticles.PRISM_FLARE, center.add(0, 0.5, 0), 20 + (int) (frac * 40), radius * 0.4, 0.2);
		Fx.sound(world, user, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f + frac * 3.0f, 1.2f - frac * 0.5f);
		Fx.sound(world, user, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.8f + frac, 0.9f);
		user.fallDistance = 0;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
