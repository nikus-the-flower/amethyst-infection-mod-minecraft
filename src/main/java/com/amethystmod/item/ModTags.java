package com.amethystmod.item;

import com.amethystmod.AmethystMod;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModTags {
	/** Everything made of purified amethyst: the anti-amethyst that pierces the barrier of an infection core. */
	public static final TagKey<Item> PURIFIED_AMETHYST = TagKey.of(RegistryKeys.ITEM, Identifier.of(AmethystMod.MOD_ID, "purified_amethyst"));

	private ModTags() {
	}
}
