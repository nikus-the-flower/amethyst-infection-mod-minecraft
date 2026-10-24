package com.amethystmod.item;

import com.amethystmod.AmethystMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A piece of the world's story: a scroll, a parchment, an old book, a diary, a torn page or a letter.
 * Which text it holds is the {@link ModComponents#LORE_ID} on the stack; right-click to read it. The
 * texts are "lore.amethystmod.&lt;id&gt;.title" and ".text" (pages split with "\n\n\n").
 */
public class LoreItem extends Item {
	/** Every story text and the kind of item it is written on. */
	public static final Map<String, String> TEXTS = new LinkedHashMap<>();

	static {
		TEXTS.put("ruins_tower", "ancient_parchment");
		TEXTS.put("ruins_house", "torn_page");
		TEXTS.put("crater_room", "scroll");
		TEXTS.put("crater_library", "ancient_book");
		TEXTS.put("crater_diary", "diary");
		TEXTS.put("crater_secret", "ancient_book");
		TEXTS.put("castle_throne", "ancient_book");
		TEXTS.put("castle_letter", "letter");
		TEXTS.put("castle_parchment", "ancient_parchment");
		TEXTS.put("ricardo_journal", "diary");
	}

	/** The client opens the reading screen through this (the server never touches it). */
	public static Consumer<String> opener = id -> {
	};

	public LoreItem(Settings settings) {
		super(settings);
	}

	/** A stack carrying the story text {@code id}, on the right kind of paper. */
	public static ItemStack create(String id) {
		String kind = TEXTS.getOrDefault(id, "scroll");
		ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.of(AmethystMod.MOD_ID, kind)));
		stack.set(ModComponents.LORE_ID, id);
		return stack;
	}

	public static String id(ItemStack stack) {
		return stack.getOrDefault(ModComponents.LORE_ID, "");
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			opener.accept(id(stack));
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public Text getName(ItemStack stack) {
		String id = id(stack);
		return id.isEmpty() ? super.getName(stack) : Text.translatable("lore.amethystmod." + id + ".title");
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey()).formatted(Formatting.GRAY, Formatting.ITALIC));
		tooltip.add(Text.translatable("lore.amethystmod.read").formatted(Formatting.DARK_PURPLE));
	}
}
