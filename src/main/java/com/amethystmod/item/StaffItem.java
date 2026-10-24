package com.amethystmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/** A staff that is channelled while the use button is held (flamethrower, prism beam). */
public class StaffItem extends Item {
	@FunctionalInterface
	public interface Channel {
		void tick(ServerWorld world, LivingEntity user, int usedTicks);
	}

	private final Channel channel;
	private final int damageInterval;
	private final int maxChannelTicks;
	private final float cooldownPerTick;

	/**
	 * @param maxChannelTicks the staff cuts off after this long
	 * @param cooldownPerTick cooldown after use: 20 ticks plus this many per tick channelled
	 */
	public StaffItem(Settings settings, int damageInterval, int maxChannelTicks, float cooldownPerTick, Channel channel) {
		super(settings);
		this.channel = channel;
		this.damageInterval = damageInterval;
		this.maxChannelTicks = maxChannelTicks;
		this.cooldownPerTick = cooldownPerTick;
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
		return UseAction.NONE;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (used >= maxChannelTicks) {
			user.stopUsingItem();
			return;
		}
		channel.tick(serverWorld, user, used);
		if (used > 0 && used % damageInterval == 0) {
			stack.damage(1, user, user.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (user instanceof PlayerEntity player && used > 0) {
			player.getItemCooldownManager().set(this, 20 + Math.round(Math.min(used, maxChannelTicks) * cooldownPerTick));
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
