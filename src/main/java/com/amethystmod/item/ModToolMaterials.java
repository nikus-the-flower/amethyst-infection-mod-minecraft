package com.amethystmod.item;

import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Supplier;

public enum ModToolMaterials implements ToolMaterial {
	AMETHYST(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2200, 9.0f, 3.5f, 25,
			() -> Ingredient.ofItems(ModItems.AMETHYST_ESSENCE));

	private final TagKey<Block> inverseTag;
	private final int durability;
	private final float miningSpeed;
	private final float attackDamage;
	private final int enchantability;
	private final Supplier<Ingredient> repair;

	ModToolMaterials(TagKey<Block> inverseTag, int durability, float miningSpeed, float attackDamage,
					 int enchantability, Supplier<Ingredient> repair) {
		this.inverseTag = inverseTag;
		this.durability = durability;
		this.miningSpeed = miningSpeed;
		this.attackDamage = attackDamage;
		this.enchantability = enchantability;
		this.repair = repair;
	}

	@Override
	public int getDurability() {
		return durability;
	}

	@Override
	public float getMiningSpeedMultiplier() {
		return miningSpeed;
	}

	@Override
	public float getAttackDamage() {
		return attackDamage;
	}

	@Override
	public TagKey<Block> getInverseTag() {
		return inverseTag;
	}

	@Override
	public int getEnchantability() {
		return enchantability;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return repair.get();
	}
}
