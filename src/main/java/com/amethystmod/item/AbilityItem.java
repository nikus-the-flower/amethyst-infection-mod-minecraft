package com.amethystmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** An item with a right-click ability and a cooldown. The tooltip comes from "<translation key>.tooltip". */
public class AbilityItem extends Item {
	@FunctionalInterface
	public interface Ability {
		/** @return true if the ability fired (starts the cooldown). */
		boolean activate(ServerWorld world, ServerPlayerEntity player, ItemStack stack);
	}

	private final int cooldownTicks;
	private final Ability ability;

	public AbilityItem(Settings settings, int cooldownTicks, Ability ability) {
		super(settings);
		this.cooldownTicks = cooldownTicks;
		this.ability = ability;
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
				if (stack.isDamageable()) {
					stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
				}
			}
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
