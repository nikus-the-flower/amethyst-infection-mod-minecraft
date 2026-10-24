package com.amethystmod.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A crystal shield. Blocks like a normal shield and throws a crystal burst back at whoever it stops (see ArtifactEvents). */
public class AmethystShieldItem extends ShieldItem {
	public AmethystShieldItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(ModItems.AMETHYST_ESSENCE);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
