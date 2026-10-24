package com.amethystmod.world;

import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.Direction;

/**
 * The four places of the story, drawn in local coordinates (y = 0 is the ground floor):
 * the starting house, the surface ruins (a square, a broken tower and houses), the sunken ruins
 * (a crater with a spiral stair, rooms under the ground and the old altar where Ricardo lives) and the
 * ruined castle.
 */
public final class SiteDesigns {
	private SiteDesigns() {
	}

	// ---- the starting house ------------------------------------------------------------------------

	public static Blueprint house(long seed) {
		Blueprint b = new Blueprint(seed);
		b.clear(-9, 0, -7, 9, 13, 8);
		// floor and stone base
		b.fill(-7, -1, -5, 7, -1, 5, Blocks.COBBLESTONE);
		b.fill(-6, -1, -4, 6, -1, 4, Blocks.SPRUCE_PLANKS);
		for (int x = -7; x <= 7; x++) {
			for (int z = -5; z <= 5; z++) {
				boolean edge = Math.abs(x) == 7 || Math.abs(z) == 5;
				if (!edge) {
					continue;
				}
				for (int y = 0; y <= 4; y++) {
					BlockState s;
					boolean corner = Math.abs(x) == 7 && Math.abs(z) == 5;
					boolean post = corner || (Math.abs(z) == 5 && x % 4 == 1) || (Math.abs(x) == 7 && z == 0);
					if (post) {
						s = Blocks.SPRUCE_LOG.getDefaultState();
					} else if (y <= 1) {
						s = b.random.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
					} else if (y == 4) {
						s = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Math.abs(z) == 5 ? Direction.Axis.X : Direction.Axis.Z);
					} else {
						s = Blocks.OAK_PLANKS.getDefaultState();
					}
					b.set(x, y, z, s);
				}
			}
		}
		// windows
		for (int[] w : new int[][]{{-5, 5}, {-1, 5}, {5, 5}, {-5, -5}, {3, -5}, {5, -5}, {-7, -2}, {7, -3}, {7, 3}}) {
			for (int y = 2; y <= 3; y++) {
				Direction.Axis axis = Math.abs(w[1]) == 5 ? Direction.Axis.X : Direction.Axis.Z;
				b.set(w[0], y, w[1], Blocks.GLASS_PANE.getDefaultState()
						.with(axis == Direction.Axis.X ? PaneBlock.EAST : PaneBlock.NORTH, true)
						.with(axis == Direction.Axis.X ? PaneBlock.WEST : PaneBlock.SOUTH, true));
			}
		}
		// front door (south), and the inner doors
		door(b, -3, 0, 5, Direction.NORTH);
		b.fill(1, 0, -4, 1, 4, 4, Blocks.OAK_PLANKS);
		b.fill(1, 0, 0, 1, 4, 0, Blocks.SPRUCE_LOG);
		door(b, 1, 0, -2, Direction.EAST);
		door(b, 1, 0, 2, Direction.EAST);
		b.fill(2, 0, 0, 6, 4, 0, Blocks.OAK_PLANKS);
		// ceiling, attic floor and the gable roof (ridge along x)
		b.fill(-6, 5, -4, 6, 5, 4, Blocks.SPRUCE_PLANKS);
		for (int k = 0; k <= 6; k++) {
			int y = 5 + k;
			int zn = -6 + k;
			int zs = 6 - k;
			for (int x = -8; x <= 8; x++) {
				if (zn < zs) {
					b.set(x, y, zn, Blueprint.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
					b.set(x, y, zs, Blueprint.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
				} else {
					b.set(x, y, zn, Blueprint.slab(Blocks.SPRUCE_SLAB, false));
				}
			}
			// the gable ends
			for (int z = zn + 1; z < zs; z++) {
				b.set(-7, y, z, Blocks.OAK_PLANKS.getDefaultState());
				b.set(7, y, z, Blocks.OAK_PLANKS.getDefaultState());
			}
		}
		b.set(-7, 7, 0, Blocks.GLASS_PANE.getDefaultState().with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true));
		b.set(7, 7, 0, Blocks.GLASS_PANE.getDefaultState().with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true));
		// chimney with a fireplace
		b.fill(-6, 0, -4, -6, 12, -4, Blocks.COBBLESTONE);
		b.set(-6, 0, -3, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		b.set(-6, 12, -4, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, Direction.NORTH));
		b.set(-6, 13, -4, Blocks.AIR.getDefaultState());

		// --- the main room: kitchen and table (west) ---
		b.set(-6, 0, -1, Blocks.FURNACE.getDefaultState().with(net.minecraft.block.FurnaceBlock.FACING, Direction.EAST));
		b.set(-6, 0, 0, Blocks.SMOKER.getDefaultState().with(net.minecraft.block.SmokerBlock.FACING, Direction.EAST));
		b.set(-6, 0, 1, Blocks.CRAFTING_TABLE);
		b.set(-6, 0, 2, Blocks.BARREL.getDefaultState());
		b.chest(-6, 0, 3, Direction.EAST, "house", "item:amethystmod:country_potatoes:3", "item:amethystmod:guide_book:1");
		b.set(-6, 0, 4, Blocks.COMPOSTER);
		// the table, with stools
		b.set(-3, 0, -1, Blocks.OAK_FENCE);
		b.set(-3, 1, -1, Blocks.OAK_PRESSURE_PLATE);
		b.set(-2, 0, -1, Blocks.OAK_FENCE);
		b.set(-2, 1, -1, Blocks.OAK_PRESSURE_PLATE);
		b.set(-4, 0, -1, Blueprint.stairs(Blocks.OAK_STAIRS, Direction.WEST, false));
		b.set(-1, 0, -1, Blueprint.stairs(Blocks.OAK_STAIRS, Direction.EAST, false));
		b.set(-3, 0, -3, Blueprint.stairs(Blocks.OAK_STAIRS, Direction.NORTH, false));
		// the note on its lectern
		b.set(-1, 0, -4, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
		b.marker(-1, 0, -4, "note");
		b.set(-4, 0, -4, Blocks.BARREL);
		b.set(-5, 0, -4, Blocks.FLOWER_POT);
		b.fill(-5, -1, 1, -2, -1, 3, Blocks.BROWN_WOOL);
		b.set(-3, 4, 0, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		b.set(-3, 4, 3, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

		// --- the bedroom (north-east): four beds ---
		for (int x : new int[]{2, 3, 5, 6}) {
			bed(b, x, 0, -4, Direction.NORTH, x < 4 ? Blocks.RED_BED : Blocks.BLUE_BED);
		}
		b.chest(4, 0, -4, Direction.SOUTH, "house");
		b.set(4, 4, -2, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		b.set(6, 0, -1, Blocks.BOOKSHELF);

		// --- the storeroom (south-east) ---
		b.set(6, 0, 4, Blocks.HAY_BLOCK);
		b.set(6, 1, 4, Blocks.HAY_BLOCK);
		b.set(5, 0, 4, Blocks.BARREL);
		b.set(6, 0, 3, Blocks.BARREL);
		b.chest(2, 0, 4, Direction.NORTH, "house", "item:minecraft:baked_potato:8", "item:amethystmod:country_potatoes:2");
		b.set(4, 4, 2, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		b.set(2, 0, 1, Blocks.SMITHING_TABLE);

		// --- outside: a path, a little fenced garden, lanterns on posts ---
		for (int z = 6; z <= 8; z++) {
			b.set(-3, -1, z, Blocks.DIRT_PATH);
			b.set(-4, -1, z, Blocks.DIRT_PATH);
		}
		for (int x = 1; x <= 6; x++) {
			b.set(x, 0, 7, Blocks.OAK_FENCE);
			b.set(x, 0, 6, Blocks.AIR.getDefaultState());
		}
		b.fill(2, -1, 6, 5, -1, 6, Blocks.FARMLAND);
		b.fill(2, 0, 6, 5, 0, 6, Blocks.POTATOES);
		b.set(-6, 0, 7, Blocks.OAK_FENCE);
		b.set(-6, 1, 7, Blocks.LANTERN);
		return b;
	}

	private static void door(Blueprint b, int x, int y, int z, Direction facing) {
		BlockState door = Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, facing);
		b.set(x, y, z, door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b.set(x, y + 1, z, door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static void bed(Blueprint b, int x, int y, int z, Direction headward, Block bed) {
		BlockState s = bed.getDefaultState().with(BedBlock.FACING, headward);
		b.set(x, y, z, s.with(BedBlock.PART, BedPart.HEAD));
		b.set(x - headward.getOffsetX(), y, z - headward.getOffsetZ(), s.with(BedBlock.PART, BedPart.FOOT));
	}

	private static void crystal(Blueprint b, int x, int y, int z, Direction face) {
		b.set(x, y, z, ModBlocks.crystal(CrystalColor.values()[b.random.nextInt(3)], b.random.nextInt(2))
				.getDefaultState().with(AmethystClusterBlock.FACING, face));
	}

	// ---- ruins 1: the old square ---------------------------------------------------------------------

	public static Blueprint surfaceRuins(long seed) {
		Blueprint b = new Blueprint(seed);
		// the square: old paving, cleared of plants
		for (int x = -11; x <= 11; x++) {
			for (int z = -11; z <= 11; z++) {
				if (x * x + z * z > 140 && b.random.nextInt(3) == 0) {
					continue;
				}
				b.set(x, -1, z, b.paving());
				b.clear(x, 0, z, x, 4, z);
			}
		}
		// the well in the middle
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) == 2 || Math.abs(z) == 2) {
					b.set(x, 0, z, b.brick());
				} else {
					b.fill(x, -4, z, x, -1, z, Blocks.WATER);
				}
			}
		}
		b.set(-2, 1, -2, Blocks.STONE_BRICK_WALL);
		b.set(2, 1, 2, Blocks.STONE_BRICK_WALL);
		b.set(2, 2, 2, Blocks.STONE_BRICK_WALL);

		// the broken tower (north-east): the shard of purity lies inside
		b.ruinedTower(14, -13, 4, 0, 17, 0.45f);
		b.chest(14, 0, -13, Direction.SOUTH, "ruins", "artifact:purity_shard", "lore:ruins_tower");
		b.set(12, 0, -14, Blocks.COBWEB);
		crystal(b, 17, 3, -13, Direction.EAST);
		// the houses, three of them, open to the sky
		ruinedHouse(b, -16, 5, 7, 6, 4);
		b.chest(-13, 0, 8, Direction.EAST, "ruins", "lore:ruins_house");
		ruinedHouse(b, -15, -15, 6, 6, 5);
		b.set(-12, 0, -12, Blocks.CAULDRON);
		ruinedHouse(b, 5, 13, 7, 5, 4);
		b.chest(9, 0, 15, Direction.WEST, "ruins");
		b.set(7, 0, 16, Blocks.COBWEB);
		b.rubbleField(-14, -14, 14, 14, 0, 40);
		// broken columns along the square
		for (int[] c : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
			int h = 1 + b.random.nextInt(4);
			for (int y = 0; y < h; y++) {
				b.set(c[0], y, c[1], Blocks.STONE_BRICK_WALL.getDefaultState());
			}
		}
		b.marker(10, 0, -9, "guard:amethystmod:amethyst_stalker");
		b.marker(-6, 0, 6, "guard:amethystmod:amethyst_stalker");
		b.marker(4, 0, -6, "guard:amethystmod:crystal_weaver");
		return b;
	}

	/** A ruined house: four crumbling walls with a doorway, a floor, rubble inside, a bit of the old roof. */
	private static void ruinedHouse(Blueprint b, int x0, int z0, int w, int d, int h) {
		int x1 = x0 + w;
		int z1 = z0 + d;
		for (int x = x0 + 1; x < x1; x++) {
			for (int z = z0 + 1; z < z1; z++) {
				b.set(x, -1, z, b.paving());
				b.clear(x, 0, z, x, h + 1, z);
			}
		}
		b.ruinedWall(x0, z0, x1, z0, 0, h, 0.5f);
		b.ruinedWall(x0, z1, x1, z1, 0, h, 0.5f);
		b.ruinedWall(x0, z0, x0, z1, 0, h, 0.5f);
		b.ruinedWall(x1, z0, x1, z1, 0, h, 0.5f);
		// the doorway
		int door = x0 + w / 2;
		b.clear(door, 0, z1, door, 2, z1);
		// a few slabs of roof still hanging on
		for (int x = x0 + 1; x < x1; x++) {
			if (b.random.nextInt(3) == 0) {
				b.set(x, h - 1, z0 + 1, Blueprint.slab(Blocks.STONE_BRICK_SLAB, true));
			}
		}
		b.rubbleField(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, 3);
	}

	// ---- ruins 2: the sunken ruins, the altar and Ricardo ---------------------------------------------

	public static final int CRATER_DEPTH = 10;
	private static final int FLOOR_R = 14;
	private static final int RIM_R = 18;

	public static Blueprint crater(long seed) {
		Blueprint b = new Blueprint(seed);
		b.foundations = false;
		int floor = -CRATER_DEPTH;
		// dig the crater: a sheer wall down to a round floor, rounded off at the rim
		for (int x = -RIM_R; x <= RIM_R; x++) {
			for (int z = -RIM_R; z <= RIM_R; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > RIM_R) {
					continue;
				}
				int depth = d <= FLOOR_R ? CRATER_DEPTH : (int) Math.round(CRATER_DEPTH * Math.pow((RIM_R - d) / (RIM_R - FLOOR_R), 2.2));
				b.clear(x, -depth, z, x, 14, z);
				if (d <= FLOOR_R) {
					b.set(x, floor - 1, z, d < 6 ? b.brick() : b.paving());
				}
			}
		}
		// a ruined ring wall at the foot of the crater wall
		for (int a = 0; a < 96; a++) {
			double ang = a * Math.PI * 2 / 96;
			int x = (int) Math.round(Math.cos(ang) * (FLOOR_R + 0.5));
			int z = (int) Math.round(Math.sin(ang) * (FLOOR_R + 0.5));
			int h = 3 + b.random.nextInt(4);
			for (int y = 0; y < h; y++) {
				b.set(x, floor + y, z, b.brick());
			}
		}
		// the spiral stair down the crater wall
		spiral(b, floor);
		// the broken bridge over the crater, and the pool beneath it to drop into
		for (int r = FLOOR_R - 4; r <= RIM_R + 1; r++) {
			b.set(r, -1, 0, b.brick());
			b.set(r, -1, 1, r < FLOOR_R - 1 && b.random.nextBoolean() ? Blocks.AIR.getDefaultState() : b.brick());
			if (r % 3 == 0) {
				b.set(r, 0, -1, Blocks.STONE_BRICK_WALL.getDefaultState());
			}
		}
		for (int x = FLOOR_R - 5; x <= FLOOR_R - 2; x++) {
			for (int z = -2; z <= 2; z++) {
				b.fill(x, floor - 3, z, x, floor - 1, z, Blocks.WATER);
				b.set(x, floor - 4, z, b.brick());
			}
		}
		// the altar: a round dais in two steps, three pedestals and the altar in the middle
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				int d2 = x * x + z * z;
				if (d2 <= 25) {
					b.set(x, floor, z, d2 <= 9 ? b.brick() : Blueprint.slab(Blocks.STONE_BRICK_SLAB, false));
				}
				if (d2 <= 9) {
					b.set(x, floor + 1, z, Blueprint.slab(Blocks.POLISHED_ANDESITE_SLAB, false));
				}
			}
		}
		b.set(0, floor + 1, 0, ModBlocks.PURIFICATION_ALTAR.getDefaultState());
		for (int i = 0; i < 3; i++) {
			double ang = Math.toRadians(90 + i * 120);
			int px = (int) Math.round(Math.cos(ang) * 3);
			int pz = (int) Math.round(Math.sin(ang) * 3);
			b.set(px, floor + 1, pz, ModBlocks.ALTAR_PEDESTAL.getDefaultState());
		}
		// broken columns around the altar
		for (int i = 0; i < 8; i++) {
			double ang = i * Math.PI / 4 + Math.PI / 8;
			int x = (int) Math.round(Math.cos(ang) * 8.5);
			int z = (int) Math.round(Math.sin(ang) * 8.5);
			int h = 2 + b.random.nextInt(5);
			for (int y = 0; y < h; y++) {
				b.set(x, floor + y, z, y == h - 1 && b.random.nextBoolean() ? Blocks.CHISELED_STONE_BRICKS.getDefaultState() : b.brick());
			}
		}
		// rooms under the ground, through the ring wall
		room(b, 40, floor, "crater", "lore:crater_room");
		room(b, 160, floor, "crater", "lore:crater_library");
		int[] last = room(b, 280, floor, "crater", "lore:crater_diary");
		secretRoom(b, 280, floor);
		// Ricardo's camp, by the wall
		camp(b, 210, floor);
		b.marker(-5, floor, 0, "guard:amethystmod:void_lurker");
		b.rubbleField(-12, -12, 12, 12, floor, 25);
		return b;
	}

	/** The point at {@code r} blocks along angle {@code deg} from the centre. */
	private static int[] polar(double deg, double r) {
		double a = Math.toRadians(deg);
		return new int[]{(int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r)};
	}

	private static void spiral(Blueprint b, int floor) {
		// from the rim (angle 100) down to the floor (angle 100 + 300), two blocks wide along the wall
		int steps = 64;
		for (int i = 0; i <= steps; i++) {
			double deg = 100 + i * 300.0 / steps;
			int y = -(int) Math.round((double) i * CRATER_DEPTH / steps);
			double r = FLOOR_R + 0.5 + 2.6 * (1.0 - (double) i / steps);
			for (double w = -0.6; w <= 0.6; w += 0.6) {
				int[] p = polar(deg, r + w);
				for (int yy = y; yy >= floor; yy--) {
					if (b.get(p[0], yy - 1, p[1]) == null || b.get(p[0], yy - 1, p[1]).isAir()) {
						b.set(p[0], yy - 1, p[1], yy == y ? b.brick() : b.rubble());
					}
				}
				b.set(p[0], y - 1, p[1], b.brick());
				b.clear(p[0], y, p[1], p[0], y + 3, p[1]);
			}
		}
		// a stone rail on the outer side, here and there
		for (int i = 0; i <= steps; i += 3) {
			double deg = 100 + i * 300.0 / steps;
			int y = -(int) Math.round((double) i * CRATER_DEPTH / steps);
			int[] p = polar(deg, FLOOR_R + 0.5 + 2.6 * (1.0 - (double) i / steps) - 1.3);
			if (b.random.nextBoolean() && y > floor) {
				b.set(p[0], y, p[1], Blocks.STONE_BRICK_WALL.getDefaultState());
			}
		}
	}

	/** A ruined room under the ground, reached by a short tunnel from the crater floor at angle {@code deg}. */
	private static int[] room(Blueprint b, double deg, int floor, String loot, String lore) {
		for (int r = FLOOR_R - 1; r <= FLOOR_R + 6; r++) {
			for (double w = -1; w <= 1; w += 0.5) {
				int[] p = polar(deg, r);
				int[] q = polar(deg + 90, w);
				int x = p[0] + q[0];
				int z = p[1] + q[1];
				b.clear(x, floor, z, x, floor + 2, z);
				b.set(x, floor - 1, z, b.paving());
				b.set(x, floor + 3, z, b.brick());
			}
		}
		int[] c = polar(deg, FLOOR_R + 10);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				boolean wall = Math.abs(x) == 4 || Math.abs(z) == 4;
				b.set(c[0] + x, floor - 1, c[1] + z, b.paving());
				b.set(c[0] + x, floor + 4, c[1] + z, b.brick());
				for (int y = 0; y <= 3; y++) {
					b.set(c[0] + x, floor + y, c[1] + z, wall ? b.brick() : Blocks.AIR.getDefaultState());
				}
			}
		}
		// the tunnel goes through the room's wall
		int[] in = polar(deg, FLOOR_R + 6);
		b.clear(in[0], floor, in[1], in[0], floor + 2, in[1]);
		int[] in2 = polar(deg, FLOOR_R + 7);
		b.clear(in2[0], floor, in2[1], in2[0], floor + 2, in2[1]);
		// a collapsed corner, cobwebs, bookshelves, a lantern
		b.fill(c[0] + 2, floor, c[1] + 2, c[0] + 3, floor + 1, c[1] + 3, Blocks.COBBLESTONE);
		b.set(c[0] + 3, floor + 2, c[1] + 3, Blocks.MOSSY_COBBLESTONE);
		b.set(c[0] - 3, floor + 3, c[1] - 3, Blocks.COBWEB);
		b.set(c[0] - 3, floor, c[1] + 3, Blocks.BOOKSHELF);
		b.set(c[0] - 3, floor + 1, c[1] + 3, Blocks.BOOKSHELF);
		b.set(c[0] - 2, floor, c[1] + 3, Blocks.CHISELED_BOOKSHELF);
		b.set(c[0], floor + 3, c[1], Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		b.chest(c[0] - 3, floor, c[1] - 1, Direction.EAST, loot, lore);
		b.marker(c[0], floor, c[1] - 2, "guard:amethystmod:amethyst_stalker");
		return c;
	}

	/** Behind the far wall of the third room: a hidden chamber with the flask of chaos. */
	private static void secretRoom(Blueprint b, double deg, int floor) {
		int[] c = polar(deg, FLOOR_R + 16);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
				b.set(c[0] + x, floor - 1, c[1] + z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
				b.set(c[0] + x, floor + 3, c[1] + z, Blocks.DEEPSLATE_BRICKS.getDefaultState());
				for (int y = 0; y <= 2; y++) {
					b.set(c[0] + x, floor + y, c[1] + z, wall ? Blocks.DEEPSLATE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState());
				}
			}
		}
		// the way in: a short passage closed by cracked bricks that look like the rest of the wall
		for (int r = FLOOR_R + 13; r <= FLOOR_R + 14; r++) {
			int[] p = polar(deg, r);
			b.clear(p[0], floor, p[1], p[0], floor + 1, p[1]);
			b.set(p[0], floor - 1, p[1], b.paving());
		}
		int[] seal = polar(deg, FLOOR_R + 14);
		b.set(seal[0], floor, seal[1], Blocks.CRACKED_STONE_BRICKS.getDefaultState());
		b.set(seal[0], floor + 1, seal[1], Blocks.CRACKED_STONE_BRICKS.getDefaultState());
		int[] gap = polar(deg, FLOOR_R + 15);
		b.clear(gap[0], floor, gap[1], gap[0], floor + 1, gap[1]);
		b.chest(c[0], floor, c[1], Direction.NORTH, "crater", "artifact:chaos_flask", "lore:crater_secret");
		b.set(c[0] + 1, floor + 2, c[1] + 1, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		crystal(b, c[0] - 1, floor, c[1] + 1, Direction.UP);
	}

	private static void camp(Blueprint b, double deg, int floor) {
		int[] c = polar(deg, FLOOR_R - 4);
		// a canvas tent: wool on fences
		for (int dx = -1; dx <= 1; dx++) {
			b.set(c[0] + dx, floor + 2, c[1], Blocks.WHITE_WOOL);
			b.set(c[0] + dx, floor + 1, c[1] - 1, Blocks.WHITE_WOOL);
			b.set(c[0] + dx, floor + 1, c[1] + 1, Blocks.WHITE_WOOL);
		}
		b.set(c[0] - 1, floor, c[1] - 1, Blocks.OAK_FENCE);
		b.set(c[0] + 1, floor, c[1] + 1, Blocks.OAK_FENCE);
		bed(b, c[0], floor, c[1], Direction.EAST, Blocks.BROWN_BED);
		int[] f = polar(deg, FLOOR_R - 7);
		b.set(f[0], floor, f[1], Blocks.CAMPFIRE.getDefaultState());
		b.set(f[0] + 1, floor, f[1], Blocks.BARREL);
		b.set(f[0] - 1, floor, f[1] + 1, Blocks.BARREL);
		b.set(f[0], floor, f[1] - 2, Blocks.CRAFTING_TABLE);
		b.chest(f[0] + 2, floor, f[1] + 1, Direction.WEST, "crater");
		b.set(f[0] - 2, floor, f[1], Blocks.LANTERN);
		b.marker(f[0] + 1, floor, f[1] + 2, "ricardo");
	}

	// ---- ruins 3: the ruined castle ------------------------------------------------------------------

	public static Blueprint castle(long seed) {
		Blueprint b = new Blueprint(seed);
		int s = 17;
		// the courtyard
		for (int x = -s; x <= s; x++) {
			for (int z = -s; z <= s; z++) {
				b.clear(x, 0, z, x, 14, z);
				if (b.random.nextInt(10) < 7) {
					b.set(x, -1, z, b.paving());
				}
			}
		}
		// curtain walls, two blocks thick, crenellated where they still stand
		for (int t = 0; t <= 1; t++) {
			b.ruinedWall(-s + t, -s, -s + t, s, 0, 8, 0.35f);
			b.ruinedWall(s - t, -s, s - t, s, 0, 8, 0.35f);
			b.ruinedWall(-s, -s + t, s, -s + t, 0, 8, 0.35f);
			b.ruinedWall(-s, s - t, s, s - t, 0, 8, 0.35f);
		}
		for (int i = -s; i <= s; i += 2) {
			for (int[] p : new int[][]{{i, -s}, {i, s}, {-s, i}, {s, i}}) {
				if (b.get(p[0], 7, p[1]) != null) {
					b.set(p[0], 8, p[1], b.brick());
				}
			}
		}
		// the gate (south): a gap with an arch
		for (int x = -2; x <= 2; x++) {
			b.clear(x, 0, s - 1, x, 4, s);
		}
		for (int x = -2; x <= 2; x++) {
			b.set(x, 5, s, b.brick());
			b.set(x, 5, s - 1, b.brick());
		}
		b.set(-2, 4, s, Blueprint.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
		b.set(2, 4, s, Blueprint.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
		// corner towers
		for (int[] c : new int[][]{{-s, -s}, {s, -s}, {-s, s}, {s, s}}) {
			b.ruinedTower(c[0], c[1], 4, 0, 14, 0.4f);
		}
		// the keep: three floors, half fallen in
		int kx0 = -8;
		int kx1 = 8;
		int kz0 = -12;
		int kz1 = 2;
		b.ruinedWall(kx0, kz0, kx1, kz0, 0, 13, 0.3f);
		b.ruinedWall(kx0, kz1, kx1, kz1, 0, 11, 0.4f);
		b.ruinedWall(kx0, kz0, kx0, kz1, 0, 12, 0.3f);
		b.ruinedWall(kx1, kz0, kx1, kz1, 0, 12, 0.35f);
		for (int x = kx0 + 1; x < kx1; x++) {
			for (int z = kz0 + 1; z < kz1; z++) {
				b.set(x, -1, z, b.brick());
				if (b.random.nextInt(10) < 6) {
					b.set(x, 4, z, Blueprint.slab(Blocks.STONE_BRICK_SLAB, true));
				}
				if (b.random.nextInt(10) < 3) {
					b.set(x, 8, z, Blueprint.slab(Blocks.STONE_BRICK_SLAB, true));
				}
			}
		}
		// the keep's door and a ladder up
		b.clear(-1, 0, kz1, 1, 2, kz1);
		// (the ladder replaces the floor slabs where it passes through, leaving a hatch)
		for (int y = 0; y <= 9; y++) {
			b.set(kx1, y, kz0 + 1, b.brick());
			b.set(kx1 - 1, y, kz0 + 1, Blocks.LADDER.getDefaultState().with(net.minecraft.block.LadderBlock.FACING, Direction.WEST));
		}
		// the throne room: a carpet up to the throne on its dais, the crown in the chest behind it
		for (int z = kz0 + 3; z < kz1; z++) {
			b.set(0, 0, z, Blocks.RED_CARPET);
		}
		b.fill(-2, 0, kz0 + 1, 2, 0, kz0 + 2, Blocks.POLISHED_ANDESITE);
		b.set(0, 1, kz0 + 2, Blueprint.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		b.set(-1, 1, kz0 + 2, Blocks.STONE_BRICK_WALL);
		b.set(1, 1, kz0 + 2, Blocks.STONE_BRICK_WALL);
		b.set(0, 2, kz0 + 1, Blocks.CHISELED_STONE_BRICKS);
		b.chest(0, 1, kz0 + 1, Direction.SOUTH, "castle", "artifact:emperor_crown", "lore:castle_throne");
		b.set(-3, 0, kz0 + 1, Blocks.RED_BANNER);
		b.set(3, 0, kz0 + 1, Blocks.RED_BANNER);
		// upper floors: a chest each
		b.chest(-6, 5, -10, Direction.EAST, "castle", "lore:castle_letter");
		b.chest(4, 9, -9, Direction.WEST, "castle", "lore:castle_parchment");
		// the courtyard: a well, a broken cart, rubble
		for (int x = 9; x <= 11; x++) {
			for (int z = 8; z <= 10; z++) {
				b.set(x, 0, z, x == 10 && z == 9 ? Blocks.WATER.getDefaultState() : b.brick());
			}
		}
		b.fill(-12, 0, 8, -10, 0, 9, Blocks.SPRUCE_PLANKS);
		b.set(-13, 0, 8, Blocks.SPRUCE_FENCE);
		b.set(-11, 1, 9, Blocks.HAY_BLOCK);
		b.rubbleField(-15, -15, 15, 15, 0, 60);
		// guards: a geode colossus keeps the courtyard, walkers the keep
		b.marker(0, 0, 9, "guard:amethystmod:geode_colossus");
		b.marker(-4, 0, -4, "guard:amethystmod:shard_walker");
		b.marker(4, 0, -6, "guard:amethystmod:shard_walker");
		b.marker(-10, 0, 0, "guard:amethystmod:amethyst_stalker");
		return b;
	}
}
