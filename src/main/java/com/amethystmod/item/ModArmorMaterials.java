package com.amethystmod.item;

import com.amethystmod.AmethystMod;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ModArmorMaterials {
	/** Armor texture layers: textures/models/armor/amethyst_layer_1.png and _layer_2.png */
	public static final RegistryEntry<ArmorMaterial> AMETHYST = register("amethyst",
			3, 6, 8, 3, 11, 28, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 2.5f, 0.05f,
			() -> Ingredient.ofItems(ModItems.AMETHYST_ESSENCE));

	/** The counter-amethyst set: heavier protection, made to fight the infection. */
	public static final RegistryEntry<ArmorMaterial> PURIFIED = register("purified",
			3, 7, 9, 3, 12, 30, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 3.5f, 0.1f,
			() -> Ingredient.ofItems(ModItems.PURIFIED_AMETHYST));

	private static RegistryEntry<ArmorMaterial> register(String name, int boots, int leggings, int chestplate,
														 int helmet, int body, int enchantability,
														 RegistryEntry<SoundEvent> equipSound, float toughness,
														 float knockbackResistance, Supplier<Ingredient> repair) {
		EnumMap<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
			map.put(ArmorItem.Type.BOOTS, boots);
			map.put(ArmorItem.Type.LEGGINGS, leggings);
			map.put(ArmorItem.Type.CHESTPLATE, chestplate);
			map.put(ArmorItem.Type.HELMET, helmet);
			map.put(ArmorItem.Type.BODY, body);
		});
		Identifier id = Identifier.of(AmethystMod.MOD_ID, name);
		List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(id));
		return Registry.registerReference(Registries.ARMOR_MATERIAL, id,
				new ArmorMaterial(defense, enchantability, equipSound, repair, layers, toughness, knockbackResistance));
	}

	public static void register() {
	}
}
