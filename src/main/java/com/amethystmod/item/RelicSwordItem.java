package com.amethystmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** A sword with an on-hit effect and a right-click ability that has its own cooldown. */
public class RelicSwordItem extends SwordItem {
	@FunctionalInterface
	public interface HitEffect {
		void onHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker);
	}

	private final HitEffect hitEffect;
	private final AbilityItem.Ability ability;
	private final int cooldownTicks;

	public RelicSwordItem(ToolMaterial material, Settings settings, HitEffect hitEffect, int cooldownTicks, AbilityItem.Ability ability) {
		super(material, settings);
		this.hitEffect = hitEffect;
		this.ability = ability;
		this.cooldownTicks = cooldownTicks;
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.getWorld() instanceof ServerWorld world) {
			hitEffect.onHit(world, stack, target, attacker);
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}
		if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
			if (ability.activate(serverWorld, player, stack)) {
				player.getItemCooldownManager().set(this, cooldownTicks);
				stack.damage(2, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			}
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
