package com.amethystmod.block;

import com.amethystmod.AmethystMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {
	/** Every block of the mod in registration order (used for the creative tab). */
	public static final List<Block> ALL = new ArrayList<>();
	/** Every crystal of every color and stage (rendered with the cutout layer). */
	public static final List<Block> CRYSTALS = new ArrayList<>();

	private static final Map<CrystalColor, Block[]> CRYSTAL_STAGES = new EnumMap<>(CrystalColor.class);

	// Infected terrain
	public static final Block INFECTED_STONE = register("infected_stone", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(2.0f, 6.0f)
					.requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK), 12));
	public static final Block INFECTED_DEEPSLATE = register("infected_deepslate", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(4.0f, 8.0f)
					.requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK), 12));
	public static final Block INFECTED_DIRT = register("infected_dirt", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(0.8f)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK), 8));
	public static final Block INFECTED_GRASS_BLOCK = register("infected_grass_block", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(0.9f)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK), 8));
	public static final Block INFECTED_SAND = register("infected_sand", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PINK).strength(0.7f)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK), 8));

	public static final Block INFECTED_LOG = register("infected_log", new InfectedLogBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(2.2f)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK).luminance(state -> 3)));
	/** Crystal leaves: they never decay (infected logs are not logs any more) and glow faintly. */
	public static final Block INFECTED_LEAVES = register("infected_leaves", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(0.3f).nonOpaque()
					.sounds(BlockSoundGroup.AMETHYST_CLUSTER).luminance(state -> 2)
					.allowsSpawning((state, world, pos, type) -> false)
					.suffocates((state, world, pos) -> false)
					.blockVision((state, world, pos) -> false), 0));

	// Heavily crystallized terrain: still spreads, sprouts crystals more often
	public static final Block CRYSTALLIZED_STONE = register("crystallized_stone", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(2.5f, 6.0f)
					.requiresTool().luminance(state -> 4).sounds(BlockSoundGroup.AMETHYST_BLOCK), 25));
	public static final Block CRYSTALLIZED_DEEPSLATE = register("crystallized_deepslate", new InfectedBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(4.5f, 8.0f)
					.requiresTool().luminance(state -> 4).sounds(BlockSoundGroup.AMETHYST_BLOCK), 25));

	// Amethyst building blocks
	public static final Block AMETHYST_CRYSTAL_BLOCK = register("amethyst_crystal_block", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(1.5f)
					.requiresTool().luminance(state -> 6).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block BRILLIANT_AMETHYST = register("brilliant_amethyst", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.PINK).strength(1.5f)
					.requiresTool().luminance(state -> 15).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block AMETHYST_BRICKS = register("amethyst_bricks", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(1.5f, 6.0f)
					.requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block AMETHYST_BRICK_SLAB = register("amethyst_brick_slab", new SlabBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(1.5f, 6.0f)
					.requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block CLEAR_AMETHYST = register("clear_amethyst", new ClearAmethystBlock(
			clearSettings().luminance(state -> 5)));
	public static final Block CLEAR_AMETHYST_PANE = register("clear_amethyst_pane", new PaneBlock(
			clearSettings().luminance(state -> 5)));

	// The square portal to the world of the Amethyst (unbreakable; only exists in the tower and the temple)
	public static final Block PORTAL_FRAME = register("portal_frame", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(-1.0f, 3600000.0f)
					.dropsNothing().luminance(state -> 4).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block PORTAL_KEYSTONE = register("portal_keystone", new PortalKeystoneBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(-1.0f, 3600000.0f)
					.dropsNothing().luminance(state -> state.get(PortalKeystoneBlock.LIT) ? 15 : 6)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Block AMETHYST_PORTAL = registerNoItem("amethyst_portal", new AmethystPortalBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).noCollision().strength(-1.0f)
					.luminance(state -> 11).sounds(BlockSoundGroup.GLASS).pistonBehavior(PistonBehavior.BLOCK)
					.dropsNothing()));

	static {
		for (CrystalColor color : CrystalColor.values()) {
			Block large = registerCrystal("crystal_large_" + color.id, new GrowingCrystalBlock(
					11, 3, crystalSettings(1.8f, 11), null));
			Block medium = registerCrystal("crystal_" + color.id, new GrowingCrystalBlock(
					7, 3, crystalSettings(1.4f, 7), large));
			Block bud = registerCrystal("crystal_bud_" + color.id, new GrowingCrystalBlock(
					3, 4, crystalSettings(1.0f, 3), medium));
			CRYSTAL_STAGES.put(color, new Block[]{bud, medium, large});
		}
	}

	private static AbstractBlock.Settings clearSettings() {
		return AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(0.4f)
				.nonOpaque().sounds(BlockSoundGroup.AMETHYST_BLOCK)
				.allowsSpawning((state, world, pos, type) -> false)
				.solidBlock((state, world, pos) -> false)
				.suffocates((state, world, pos) -> false)
				.blockVision((state, world, pos) -> false);
	}

	private static AbstractBlock.Settings crystalSettings(float strength, int light) {
		return AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).solid().nonOpaque()
				.strength(strength).luminance(state -> light)
				.sounds(BlockSoundGroup.AMETHYST_CLUSTER).pistonBehavior(PistonBehavior.DESTROY);
	}

	/** Crystal of a color at a growth stage: 0 bud, 1 crystal, 2 large crystal. */
	public static Block crystal(CrystalColor color, int stage) {
		return CRYSTAL_STAGES.get(color)[stage];
	}

	/** The crystallized version of an infected block, or null if there is none. */
	public static Block crystallized(Block block) {
		if (block == INFECTED_STONE) {
			return CRYSTALLIZED_STONE;
		}
		if (block == INFECTED_DEEPSLATE) {
			return CRYSTALLIZED_DEEPSLATE;
		}
		return null;
	}

	// The old altar of the sunken ruins: three pedestals for the relics and the altar that purifies
	public static final Block ALTAR_PEDESTAL = register("altar_pedestal", new AltarBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(-1.0f, 3600000.0f).dropsNothing()
					.nonOpaque().luminance(state -> 5).sounds(BlockSoundGroup.STONE), false));
	public static final Block PURIFICATION_ALTAR = register("purification_altar", new AltarBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(-1.0f, 3600000.0f).dropsNothing()
					.nonOpaque().luminance(state -> 9).sounds(BlockSoundGroup.STONE), true));

	private static Block registerCrystal(String name, Block block) {
		Block registered = register(name, block);
		CRYSTALS.add(registered);
		return registered;
	}

	private static Block register(String name, Block block) {
		Identifier id = Identifier.of(AmethystMod.MOD_ID, name);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
		ALL.add(block);
		return Registry.register(Registries.BLOCK, id, block);
	}

	private static Block registerNoItem(String name, Block block) {
		return Registry.register(Registries.BLOCK, Identifier.of(AmethystMod.MOD_ID, name), block);
	}

	public static void register() {
	}

	/** Glass-like amethyst: hides the faces between neighbouring blocks, like glass. */
	private static class ClearAmethystBlock extends TransparentBlock {
		ClearAmethystBlock(AbstractBlock.Settings settings) {
			super(settings);
		}
	}
}
