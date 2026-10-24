package com.amethystmod.item;

import com.amethystmod.effect.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** Food of the mod: its effects come from its food component; purified food also cures the blight. */
public class CrystalFoodItem extends Item {
	private final boolean cures;
	private final Formatting color;

	public CrystalFoodItem(Settings settings, boolean curesBlight, Formatting tooltipColor) {
		super(settings);
		this.cures = curesBlight;
		this.color = tooltipColor;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (cures && !world.isClient) {
			user.removeStatusEffect(ModEffects.BLIGHT);
		}
		return super.finishUsing(stack, world, user);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(color));
	}
}
