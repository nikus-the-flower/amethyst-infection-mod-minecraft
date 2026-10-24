package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.ModEntities;
import com.amethystmod.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Amethyst Compendium: a table of contents, categories, and one entry per creature, boss,
 * weapon, armor set and artifact, with a 3D preview of the creature or item and its crafting recipes.
 * Text comes from the language files (guide.amethystmod.ID.title / .text).
 */
public class GuideScreen extends Screen {
	private record Entry(String id, String category, EntityType<? extends LivingEntity> entity, Item icon, String... recipes) {
	}

	private static final String[] CATEGORIES = {"world", "creatures", "bosses", "weapons", "armor", "artifacts"};

	private static final List<Entry> ENTRIES = List.of(
			new Entry("world", "world", null, ModItems.AMETHYST_ESSENCE),
			new Entry("cores", "world", ModEntities.INFECTION_CORE, null),
			new Entry("blocks", "world", null, ModItems.AMETHYST_ESSENCE, "amethyst_crystal_block", "brilliant_amethyst", "clear_amethyst", "amethyst_bricks"),
			new Entry("materials", "world", null, ModItems.FLAME_CRYSTAL, "flame_crystal", "frost_crystal", "purified_amethyst"),
			new Entry("resonant_shard", "world", null, ModItems.RESONANT_SHARD),
			new Entry("ruins", "world", null, ModItems.ANCIENT_PARCHMENT),
			new Entry("altar", "world", null, ModItems.PURITY_SHARD),
			new Entry("food", "world", null, ModItems.PURIFIED_CRYSTAL_APPLE, "purified_crystal_apple", "purified_crystal_bread", "country_potatoes"),
			new Entry("tower", "world", null, ModItems.RESONANT_SHARD),
			new Entry("portal", "world", null, ModItems.CRYSTAL_SHARD),
			new Entry("amethyst_world", "world", null, ModItems.CRYSTAL_SHARD),
			new Entry("temple", "bosses", null, ModItems.HEART_CORE),
			new Entry("commands", "world", null, ModItems.GUIDE_BOOK, "guide_book"),

			new Entry("stalker", "creatures", ModEntities.STALKER, null),
			new Entry("weaver", "creatures", ModEntities.WEAVER, null),
			new Entry("walker", "creatures", ModEntities.WALKER, null),
			new Entry("lurker", "creatures", ModEntities.LURKER, null),
			new Entry("colossus", "creatures", ModEntities.COLOSSUS, null),
			new Entry("drifter", "creatures", ModEntities.DRIFTER, null),
			new Entry("ricardo", "creatures", ModEntities.RICARDO, null),

			new Entry("amitariel", "bosses", ModEntities.AMITARIEL, null),
			new Entry("heart", "bosses", ModEntities.HEART, null),

			new Entry("amethyst_tools", "weapons", null, ModItems.AMETHYST_PICKAXE, "amethyst_pickaxe", "amethyst_axe"),
			new Entry("amethyst_sword", "weapons", null, ModItems.AMETHYST_SWORD, "amethyst_sword"),
			new Entry("flame_brand", "weapons", null, ModItems.FLAME_BRAND, "flame_brand"),
			new Entry("frost_edge", "weapons", null, ModItems.FROST_EDGE, "frost_edge"),
			new Entry("storm_blade", "weapons", null, ModItems.STORM_BLADE, "storm_blade"),
			new Entry("void_edge", "weapons", null, ModItems.VOID_EDGE, "void_edge"),
			new Entry("purifier_blade", "weapons", null, ModItems.PURIFIER_BLADE, "purifier_blade"),
			new Entry("flame_staff", "weapons", null, ModItems.FLAME_STAFF, "flame_staff"),
			new Entry("frost_staff", "weapons", null, ModItems.FROST_STAFF, "frost_staff"),
			new Entry("prism_scepter", "weapons", null, ModItems.PRISM_SCEPTER, "prism_scepter"),
			new Entry("amitariel_spear", "weapons", null, ModItems.AMITARIEL_SPEAR),
			new Entry("crescent_blade", "weapons", null, ModItems.CRESCENT_BLADE, "crescent_blade"),
			new Entry("purified_spear", "weapons", null, ModItems.PURIFIED_SPEAR, "purified_spear"),
			new Entry("crystal_hammer", "weapons", null, ModItems.CRYSTAL_HAMMER, "crystal_hammer"),
			new Entry("talis_hammer", "weapons", null, ModItems.TALIS_HAMMER, "talis_hammer"),
			new Entry("crystal_bow", "weapons", null, ModItems.CRYSTAL_BOW, "crystal_bow", "crystal_arrow"),
			new Entry("amethyst_shield", "weapons", null, ModItems.AMETHYST_SHIELD, "amethyst_shield"),

			new Entry("amethyst_armor", "armor", null, ModItems.AMETHYST_CHESTPLATE, "amethyst_helmet", "amethyst_chestplate"),
			new Entry("purified_armor", "armor", null, ModItems.PURIFIED_CHESTPLATE, "purified_helmet", "purified_chestplate"),

			new Entry("aegis_heart", "artifacts", null, ModItems.AEGIS_HEART, "aegis_heart"),
			new Entry("amethyst_wings", "artifacts", null, ModItems.AMETHYST_WINGS, "amethyst_wings"),
			new Entry("blink_ring", "artifacts", null, ModItems.BLINK_RING, "blink_ring"),
			new Entry("vampiric_shard", "artifacts", null, ModItems.VAMPIRIC_SHARD, "vampiric_shard"),
			new Entry("berserker_core", "artifacts", null, ModItems.BERSERKER_CORE, "berserker_core"),
			new Entry("stasis_crystal", "artifacts", null, ModItems.STASIS_CRYSTAL, "stasis_crystal"),
			new Entry("gravity_orb", "artifacts", null, ModItems.GRAVITY_ORB, "gravity_orb"),
			new Entry("crystal_totem", "artifacts", null, ModItems.CRYSTAL_TOTEM, "crystal_totem"),
			new Entry("thunder_shard", "artifacts", null, ModItems.THUNDER_SHARD, "thunder_shard"),
			new Entry("purity_amulet", "artifacts", null, ModItems.PURITY_AMULET),
			new Entry("purity_aegis", "artifacts", null, ModItems.PURITY_AEGIS, "purity_aegis"));

	private static final int W = 300;
	private static final int H = 200;
	private static final int LINES_PER_PAGE = 15;

	private String category;
	private Entry entry;
	private int textPage;
	private int recipeIndex;
	private LivingEntity preview;
	private int left;
	private int top;

	public GuideScreen() {
		super(Text.translatable("item.amethystmod.guide_book"));
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		clearChildren();
		if (entry != null) {
			buildEntryButtons();
		} else if (category != null) {
			buildCategoryButtons();
		} else {
			int y = top + 52;
			for (String cat : CATEGORIES) {
				addDrawableChild(ButtonWidget.builder(Text.translatable("guide.amethystmod.cat." + cat), b -> {
					category = cat;
					clearAndInit();
				}).dimensions(left + W / 2 - 80, y, 160, 18).build());
				y += 22;
			}
		}
	}

	private void buildCategoryButtons() {
		List<Entry> inCategory = ENTRIES.stream().filter(e -> e.category().equals(category)).toList();
		int columns = inCategory.size() > 9 ? 2 : 1;
		int perColumn = (inCategory.size() + columns - 1) / columns;
		int bw = columns == 2 ? 130 : 180;
		for (int i = 0; i < inCategory.size(); i++) {
			Entry e = inCategory.get(i);
			int col = i / perColumn;
			int row = i % perColumn;
			int x = left + (W - columns * (bw + 6) + 6) / 2 + col * (bw + 6);
			addDrawableChild(ButtonWidget.builder(Text.translatable("guide.amethystmod." + e.id() + ".title"), b -> {
				entry = e;
				textPage = 0;
				recipeIndex = 0;
				preview = e.entity() == null ? null : e.entity().create(client.world);
				clearAndInit();
			}).dimensions(x, top + 40 + row * 20, bw, 18).build());
		}
		addDrawableChild(ButtonWidget.builder(Text.translatable("guide.amethystmod.back"), b -> {
			category = null;
			clearAndInit();
		}).dimensions(left + 10, top + H - 26, 60, 18).build());
	}

	private void buildEntryButtons() {
		addDrawableChild(ButtonWidget.builder(Text.translatable("guide.amethystmod.back"), b -> {
			entry = null;
			preview = null;
			clearAndInit();
		}).dimensions(left + 10, top + H - 26, 60, 18).build());
		if (entry.recipes().length > 1) {
			addDrawableChild(ButtonWidget.builder(Text.translatable("guide.amethystmod.nextrecipe"), b -> recipeIndex++)
					.dimensions(left + 75, top + H - 26, 60, 18).build());
		}
		int pages = textPages().size();
		if (pages > 1) {
			addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
				textPage = Math.max(0, textPage - 1);
				clearAndInit();
			}).dimensions(left + W - 80, top + H - 26, 20, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
				textPage = Math.min(pages - 1, textPage + 1);
				clearAndInit();
			}).dimensions(left + W - 30, top + H - 26, 20, 18).build());
		}
	}

	private List<List<OrderedText>> textPages() {
		Text body = Text.translatable("guide.amethystmod." + entry.id() + ".text");
		List<OrderedText> lines = textRenderer.wrapLines(body, 142);
		List<List<OrderedText>> pages = new ArrayList<>();
		for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
			pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
		}
		return pages;
	}

	@Override
	public void tick() {
		if (preview != null) {
			preview.age++;
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// the page is drawn here, after the (blurred) world background and before the buttons
		super.renderBackground(context, mouseX, mouseY, delta);
		context.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB57BFF);
		context.fill(left, top, left + W, top + H, 0xF0160B2B);
		context.fill(left + 1, top + 1, left + W - 1, top + 24, 0xFF2A1450);

		Text title = entry != null ? Text.translatable("guide.amethystmod." + entry.id() + ".title")
				: category != null ? Text.translatable("guide.amethystmod.cat." + category) : this.title;
		context.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), left + W / 2, top + 8, 0xFFFFFF);

		if (entry == null && category == null) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("guide.amethystmod.subtitle").formatted(Formatting.GRAY), left + W / 2, top + 32, 0xAAAAAA);
		}
		if (entry != null) {
			renderEntry(context, mouseX, mouseY);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
	}

	private void renderEntry(DrawContext context, int mouseX, int mouseY) {
		int px = left + 10;
		int py = top + 30;
		boolean mob = preview != null;
		int boxH = mob ? 140 : 46;
		context.fill(px, py, px + 120, py + boxH, 0xFF0C0618);
		if (mob) {
			float tallest = Math.max(preview.getHeight(), preview.getWidth() * 0.9f);
			int size = (int) (112 / tallest);
			InventoryScreen.drawEntity(context, px, py, px + 120, py + boxH, size, 0.0625f, mouseX, mouseY, preview);
		} else if (entry.icon() != null) {
			context.getMatrices().push();
			context.getMatrices().translate(px + 60 - 16, py + boxH / 2 - 16, 0);
			context.getMatrices().scale(2, 2, 1);
			context.drawItem(new ItemStack(entry.icon()), 0, 0);
			context.getMatrices().pop();
		}
		if (entry.recipes().length > 0) {
			drawRecipe(context, px, py + boxH + 14, entry.recipes()[recipeIndex % entry.recipes().length], mouseX, mouseY);
		}

		List<List<OrderedText>> pages = textPages();
		int ty = top + 32;
		if (!pages.isEmpty()) {
			for (OrderedText line : pages.get(Math.min(textPage, pages.size() - 1))) {
				context.drawText(textRenderer, line, left + 148, ty, 0xE8D8FF, false);
				ty += 10;
			}
			if (pages.size() > 1) {
				context.drawCenteredTextWithShadow(textRenderer, Text.literal((textPage + 1) + "/" + pages.size()), left + W - 45, top + H - 21, 0xAAAAAA);
			}
		}
	}

	private boolean drawRecipe(DrawContext context, int x, int y, String recipeId, int mouseX, int mouseY) {
		if (client == null || client.world == null) {
			return false;
		}
		Optional<RecipeEntry<?>> found = client.world.getRecipeManager().get(Identifier.of(AmethystMod.MOD_ID, recipeId));
		if (found.isEmpty()) {
			return false;
		}
		Recipe<?> recipe = found.get().value();
		List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
		int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : Math.min(3, Math.max(1, ingredients.size()));
		ItemStack result = recipe.getResult(client.world.getRegistryManager());

		context.drawText(textRenderer, Text.translatable("guide.amethystmod.recipe"), x, y - 10, 0xB57BFF, false);
		for (int i = 0; i < ingredients.size(); i++) {
			int cx = x + (i % width) * 18;
			int cy = y + (i / width) * 18;
			context.fill(cx, cy, cx + 17, cy + 17, 0xFF2A1450);
			ItemStack[] options = ingredients.get(i).getMatchingStacks();
			if (options.length > 0) {
				ItemStack shown = options[(int) ((System.currentTimeMillis() / 1000) % options.length)];
				context.drawItem(shown, cx + 1, cy + 1);
				if (mouseX >= cx && mouseX < cx + 17 && mouseY >= cy && mouseY < cy + 17) {
					context.drawItemTooltip(textRenderer, shown, mouseX, mouseY);
				}
			}
		}
		int rx = x + 3 * 18 + 22;
		context.drawText(textRenderer, Text.literal("→"), x + 3 * 18 + 5, y + 20, 0xFFFFFF, false);
		context.fill(rx, y + 17, rx + 17, y + 34, 0xFF4A2A80);
		context.drawItem(result, rx + 1, y + 18);
		if (result.getCount() > 1) {
			context.drawItemInSlot(textRenderer, result, rx + 1, y + 18);
		}
		if (mouseX >= rx && mouseX < rx + 17 && mouseY >= y + 17 && mouseY < y + 34) {
			context.drawItemTooltip(textRenderer, result, mouseX, mouseY);
		}
		return true;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	public static void open() {
		MinecraftClient.getInstance().setScreen(new GuideScreen());
	}
}
