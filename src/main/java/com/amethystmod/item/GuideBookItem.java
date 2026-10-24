package com.amethystmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** The Amethyst Compendium. The client sets {@link #opener} to show the book screen. */
public class GuideBookItem extends Item {
	public static Runnable opener = () -> {
	};

	public GuideBookItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient) {
			opener.run();
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
	}
}
