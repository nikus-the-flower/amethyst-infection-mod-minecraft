package com.amethystmod.item;

import net.minecraft.util.Formatting;
import net.minecraft.item.Items;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.component.type.FoodComponent;
import com.amethystmod.AmethystMod;
import com.amethystmod.entity.ModEntities;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
	/** Every item of the mod in registration order (used for the creative tab). */
	public static final List<Item> ALL = new ArrayList<>();

	// Materials
	public static final Item AMETHYST_ESSENCE = register("amethyst_essence", new Item(new Item.Settings()));
	public static final Item FROST_CRYSTAL = register("frost_crystal", new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
	public static final Item FLAME_CRYSTAL = register("flame_crystal", new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
	public static final Item PURIFIED_AMETHYST = register("purified_amethyst", new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));
	/** Dropped now and then by every creature of the Amethyst: what good gear is made with. */
	public static final Item RESONANT_SHARD = register("resonant_shard", new Item(new Item.Settings().rarity(Rarity.RARE)));

	// Amethyst tools
	public static final Item AMETHYST_SWORD = register("amethyst_sword", new AmethystBladeItem(ModToolMaterials.AMETHYST,
			new Item.Settings().rarity(Rarity.RARE).attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterials.AMETHYST, 3, -2.4f))));
	public static final Item AMETHYST_PICKAXE = register("amethyst_pickaxe", new PickaxeItem(ModToolMaterials.AMETHYST,
			new Item.Settings().attributeModifiers(PickaxeItem.createAttributeModifiers(ModToolMaterials.AMETHYST, 1.0f, -2.8f))));
	public static final Item AMETHYST_AXE = register("amethyst_axe", new AxeItem(ModToolMaterials.AMETHYST,
			new Item.Settings().attributeModifiers(AxeItem.createAttributeModifiers(ModToolMaterials.AMETHYST, 5.0f, -3.0f))));
	public static final Item AMETHYST_SHOVEL = register("amethyst_shovel", new ShovelItem(ModToolMaterials.AMETHYST,
			new Item.Settings().attributeModifiers(ShovelItem.createAttributeModifiers(ModToolMaterials.AMETHYST, 1.5f, -3.0f))));
	public static final Item AMETHYST_HOE = register("amethyst_hoe", new HoeItem(ModToolMaterials.AMETHYST,
			new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(ModToolMaterials.AMETHYST, -3.0f, 0.0f))));

	// Armor
	public static final Item AMETHYST_HELMET = armor("amethyst_helmet", ModArmorMaterials.AMETHYST, ArmorItem.Type.HELMET, 38);
	public static final Item AMETHYST_CHESTPLATE = armor("amethyst_chestplate", ModArmorMaterials.AMETHYST, ArmorItem.Type.CHESTPLATE, 38);
	public static final Item AMETHYST_LEGGINGS = armor("amethyst_leggings", ModArmorMaterials.AMETHYST, ArmorItem.Type.LEGGINGS, 38);
	public static final Item AMETHYST_BOOTS = armor("amethyst_boots", ModArmorMaterials.AMETHYST, ArmorItem.Type.BOOTS, 38);
	public static final Item PURIFIED_HELMET = armor("purified_helmet", ModArmorMaterials.PURIFIED, ArmorItem.Type.HELMET, 42);
	public static final Item PURIFIED_CHESTPLATE = armor("purified_chestplate", ModArmorMaterials.PURIFIED, ArmorItem.Type.CHESTPLATE, 42);
	public static final Item PURIFIED_LEGGINGS = armor("purified_leggings", ModArmorMaterials.PURIFIED, ArmorItem.Type.LEGGINGS, 42);
	public static final Item PURIFIED_BOOTS = armor("purified_boots", ModArmorMaterials.PURIFIED, ArmorItem.Type.BOOTS, 42);

	// Staffs and hammer
	public static final Item FLAME_STAFF = register("flame_staff", new StaffItem(
			new Item.Settings().maxCount(1).maxDamage(600).rarity(Rarity.EPIC), 10, 80, 1.5f, Weapons::flamethrower));
	public static final Item PRISM_SCEPTER = register("prism_scepter", new StaffItem(
			new Item.Settings().maxCount(1).maxDamage(700).rarity(Rarity.EPIC), 8, 60, 2.0f, Weapons::prismBeam));
	public static final Item FROST_STAFF = register("frost_staff", new AbilityItem(
			new Item.Settings().maxCount(1).maxDamage(300).rarity(Rarity.EPIC), 100, Weapons::frostWave));
	public static final Item CRYSTAL_HAMMER = register("crystal_hammer", new CrystalHammerItem(
			new Item.Settings().maxCount(1).maxDamage(1500).rarity(Rarity.EPIC).attributeModifiers(hammerAttributes(11.0, -3.1))));

	public static final Item AMITARIEL_SPEAR = register("amitariel_spear", new AmitarielSpearItem(
			new Item.Settings().maxCount(1).maxDamage(1800).rarity(Rarity.EPIC).attributeModifiers(hammerAttributes(10.0, -2.8))));

	public static final Item PURIFIED_SPEAR = register("purified_spear", new PurifiedSpearItem(
			new Item.Settings().maxCount(1).maxDamage(1600).rarity(Rarity.EPIC).attributeModifiers(hammerAttributes(8.0, -2.8))));
	public static final Item CRESCENT_BLADE = register("crescent_blade", new CrescentBladeItem(ModToolMaterials.AMETHYST,
			new Item.Settings().rarity(Rarity.EPIC).attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterials.AMETHYST, 4, -2.4f))));

	public static final Item TALIS_HAMMER = register("talis_hammer", new TalisHammerItem(
			new Item.Settings().maxCount(1).maxDamage(1200).rarity(Rarity.EPIC).attributeModifiers(hammerAttributes(10.0, -3.0))));

	// Relic swords: each has an on-hit effect and a right-click ability
	public static final Item FLAME_BRAND = sword("flame_brand", 3, SwordAbilities::flameHit, 160, SwordAbilities::flameWave);
	public static final Item FROST_EDGE = sword("frost_edge", 3, SwordAbilities::frostHit, 220, SwordAbilities::frostNova);
	public static final Item STORM_BLADE = sword("storm_blade", 3, SwordAbilities::stormHit, 200, SwordAbilities::chainLightning);
	public static final Item VOID_EDGE = sword("void_edge", 2, SwordAbilities::voidHit, 100, SwordAbilities::voidStep);
	public static final Item PURIFIER_BLADE = sword("purifier_blade", 3, SwordAbilities::purifierHit, 140, SwordAbilities::purgingFlash);

	// Bow, arrows, shield, book
	public static final Item CRYSTAL_BOW = register("crystal_bow", new CrystalBowItem(new Item.Settings().maxDamage(900).rarity(Rarity.RARE)));
	public static final Item CRYSTAL_ARROW = register("crystal_arrow", new CrystalArrowItem(new Item.Settings()));
	public static final Item AMETHYST_SHIELD = register("amethyst_shield", new AmethystShieldItem(new Item.Settings().maxDamage(750).rarity(Rarity.RARE)));
	public static final Item GUIDE_BOOK = register("guide_book", new GuideBookItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

	// Artifacts
	public static final Item AEGIS_HEART = artifact("aegis_heart", 20 * 90, Abilities::aegis);
	public static final Item AMETHYST_WINGS = artifact("amethyst_wings", 20 * 60, Abilities::wings);
	public static final Item BLINK_RING = artifact("blink_ring", 20 * 5, Abilities::blink);
	public static final Item VAMPIRIC_SHARD = artifact("vampiric_shard", 20, (w, p, s) -> false);
	public static final Item BERSERKER_CORE = artifact("berserker_core", 20 * 120, Abilities::berserk);
	public static final Item STASIS_CRYSTAL = artifact("stasis_crystal", 20 * 45, Abilities::stasis);
	public static final Item GRAVITY_ORB = artifact("gravity_orb", 20 * 30, Abilities::gravity);
	public static final Item CRYSTAL_TOTEM = artifact("crystal_totem", 20, (w, p, s) -> false);
	public static final Item THUNDER_SHARD = artifact("thunder_shard", 20 * 10, Abilities::thunder);
	/** Carried: the blight cannot take hold, and creatures of the Amethyst hit a quarter softer. */
	public static final Item PURITY_AMULET = artifact("purity_amulet", 20, (w, p, s) -> false);
	/** Charged with purified amethyst: a dome of light and a row of white hearts. */
	public static final Item PURITY_AEGIS = register("purity_aegis", new PurityAegisItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
			.component(ModComponents.SHIELD_CHARGE, 0).component(ModComponents.SHIELD_HEALTH, 0)));

	// The three relics of the altar, one in each ruin
	public static final Item PURITY_SHARD = relic("purity_shard");
	public static final Item EMPEROR_CROWN = relic("emperor_crown");
	public static final Item CHAOS_FLASK = relic("chaos_flask");

	// Pages of the world's story (which text: the lore_id on the stack)
	public static final Item SCROLL = lore("scroll");
	public static final Item ANCIENT_PARCHMENT = lore("ancient_parchment");
	public static final Item ANCIENT_BOOK = lore("ancient_book");
	public static final Item DIARY = lore("diary");
	public static final Item TORN_PAGE = lore("torn_page");
	public static final Item LETTER = lore("letter");

	// Food
	public static final Item PURIFIED_CRYSTAL_APPLE = register("purified_crystal_apple", new CrystalFoodItem(new Item.Settings()
			.rarity(Rarity.RARE).food(new FoodComponent.Builder().nutrition(4).saturationModifier(1.2f).alwaysEdible()
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1), 1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 0), 1.0f).build()), true, Formatting.AQUA));
	public static final Item PURIFIED_CRYSTAL_BREAD = register("purified_crystal_bread", new CrystalFoodItem(new Item.Settings()
			.food(new FoodComponent.Builder().nutrition(7).saturationModifier(0.9f)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0), 1.0f).build()), true, Formatting.AQUA));
	public static final Item CEREAL_WITH_MILK = register("cereal_with_milk", new CrystalFoodItem(new Item.Settings().maxCount(16)
			.rarity(Rarity.EPIC).food(new FoodComponent.Builder().nutrition(8).saturationModifier(1.0f).alwaysEdible()
					.statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1), 1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1), 1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 1200, 0), 1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200, 0), 1.0f)
					.usingConvertsTo(Items.BOWL).build()), true, Formatting.GOLD));
	public static final Item COUNTRY_POTATOES = register("country_potatoes", new CrystalFoodItem(new Item.Settings().maxCount(16)
			.rarity(Rarity.UNCOMMON).food(new FoodComponent.Builder().nutrition(10).saturationModifier(1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 2400, 0), 1.0f)
					.statusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 2400, 0), 1.0f)
					.usingConvertsTo(Items.BOWL).build()), false, Formatting.GOLD));

	// Boss drops and the shard that creatures shoot
	public static final Item CRYSTAL_SHARD = register("crystal_shard", new Item(new Item.Settings()));
	public static final Item PORTAL_CORE = register("portal_core", new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
			.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));
	public static final Item HEART_CORE = register("heart_core", new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
			.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

	/** Plants an infection core of its level (creative and commands only; the cores throw their own). */
	public static final Item CRYSTAL_GEODE = register("crystal_geode", new CrystalGeodeItem(new Item.Settings().rarity(Rarity.RARE)
			.component(ModComponents.CORE_PHASE, 1)));

	// Spawn eggs
	public static final Item STALKER_EGG = egg(ModEntities.STALKER, 0x3F2A6B, 0xC48BFF);
	public static final Item WEAVER_EGG = egg(ModEntities.WEAVER, 0x2E2150, 0xFF5BE0);
	public static final Item WALKER_EGG = egg(ModEntities.WALKER, 0x4A3380, 0xE0B0FF);
	public static final Item LURKER_EGG = egg(ModEntities.LURKER, 0x241A40, 0x9B6BFF);
	public static final Item COLOSSUS_EGG = egg(ModEntities.COLOSSUS, 0x57408F, 0xFF70E8);
	public static final Item DRIFTER_EGG = egg(ModEntities.DRIFTER, 0x6A4AB0, 0xF0D0FF);
	public static final Item AMITARIEL_EGG = egg(ModEntities.AMITARIEL, 0x1C1030, 0xFF4FD8);
	public static final Item HEART_EGG = egg(ModEntities.HEART, 0xA040E0, 0xFFA0F0);

	private static Item egg(net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type, int primary, int secondary) {
		return register(Registries.ENTITY_TYPE.getId(type).getPath() + "_spawn_egg", new SpawnEggItem(type, primary, secondary, new Item.Settings()));
	}

	private static Item sword(String name, int baseDamage, RelicSwordItem.HitEffect hit, int cooldown, AbilityItem.Ability ability) {
		return register(name, new RelicSwordItem(ModToolMaterials.AMETHYST, new Item.Settings().rarity(Rarity.EPIC)
				.attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterials.AMETHYST, baseDamage, -2.4f)), hit, cooldown, ability));
	}

	private static AttributeModifiersComponent hammerAttributes(double damage, double speed) {
		return AttributeModifiersComponent.builder()
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
						new EntityAttributeModifier(Identifier.ofVanilla("base_attack_damage"), damage,
								EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
				.add(EntityAttributes.GENERIC_ATTACK_SPEED,
						new EntityAttributeModifier(Identifier.ofVanilla("base_attack_speed"), speed,
								EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
				.build();
	}

	private static Item armor(String name, RegistryEntry<net.minecraft.item.ArmorMaterial> material, ArmorItem.Type type, int durabilityMultiplier) {
		return register(name, new ArmorItem(material, type,
				new Item.Settings().maxDamage(type.getMaxDamage(durabilityMultiplier)).rarity(Rarity.RARE)));
	}

	private static Item artifact(String name, int cooldownTicks, AbilityItem.Ability ability) {
		return register(name, new AbilityItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
				.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true), cooldownTicks, ability));
	}

	private static Item relic(String name) {
		return register(name, new AbilityItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()
				.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true), 20, (w, p, s) -> false));
	}

	private static Item lore(String name) {
		return register(name, new LoreItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
	}

	private static Item register(String name, Item item) {
		ALL.add(item);
		return Registry.register(Registries.ITEM, Identifier.of(AmethystMod.MOD_ID, name), item);
	}

	public static void register() {
	}
}
