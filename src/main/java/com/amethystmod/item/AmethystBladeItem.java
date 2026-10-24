package com.amethystmod.item;

import com.amethystmod.ModParticles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Amethyst Blade. Strikes sometimes shatter into a crystal burst that hits everything nearby;
 * right-click is a crystal dash that cuts through enemies in the path.
 */
public class AmethystBladeItem extends SwordItem {
	public AmethystBladeItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.getWorld() instanceof ServerWorld world && attacker.getRandom().nextFloat() < 0.35f) {
			Vec3d center = target.getPos().add(0, target.getHeight() / 2, 0);
			for (LivingEntity e : Fx.around(world, center, 3.5, Fx.hostileTo(attacker))) {
				if (e != target) {
					e.damage(attacker instanceof PlayerEntity p
							? world.getDamageSources().playerAttack(p) : world.getDamageSources().mobAttack(attacker), 5.0f);
				}
			}
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 6, 0));
			Fx.particles(world, ModParticles.CRYSTAL_SPARK, center, 30, 0.8, 0.25);
			Fx.sound(world, target, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.2f, 1.4f);
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}
		if (world instanceof ServerWorld serverWorld) {
			Vec3d look = user.getRotationVec(1.0f);
			Vec3d dir = new Vec3d(look.x, Math.max(look.y * 0.4, 0.05), look.z).normalize();
			Box path = user.getBoundingBox().stretch(dir.multiply(7)).expand(1.0);
			for (LivingEntity e : serverWorld.getEntitiesByClass(LivingEntity.class, path, Fx.hostileTo(user))) {
				e.damage(serverWorld.getDamageSources().playerAttack(user), 10.0f);
				Fx.push(e, dir.multiply(0.9).add(0, 0.4, 0));
				Fx.particles(serverWorld, ParticleTypes.CRIT, e.getPos().add(0, 1, 0), 15, 0.4, 0.3);
			}
			Fx.push(user, dir.multiply(2.4));
			user.fallDistance = 0;
			Fx.particles(serverWorld, ModParticles.CRYSTAL_SPARK, user.getPos().add(0, 1, 0), 30, 0.4, 0.3);
			Fx.sound(serverWorld, user, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.6f);
			user.getItemCooldownManager().set(this, 120);
			stack.damage(2, user, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
