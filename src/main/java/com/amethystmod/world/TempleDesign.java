package com.amethystmod.world;

import com.amethystmod.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * The Temple of the Amethyst at the centre of the Amethyst world, as a pure function of position
 * (dx, dz from 0,0; y absolute). It floats on its own rock island: a round cathedral (arena of
 * radius 30, ten columns, five pylon pedestals, the return portal on the north wall) under a
 * pointed crystal dome, on a plaza with four giant crystal spires.
 */
public final class TempleDesign {
	public static final int FLOOR_Y = 100;
	public static final int ARENA_RADIUS = 30;
	public static final int WALL_HEIGHT = 20;
	public static final int DOME_HEIGHT = 42;
	public static final int ISLAND_RADIUS = 48;
	public static final int PORTAL_Z = -29;
	public static final double PYLON_RADIUS = 21.0;
	private static final int SPIRE_TOP = 13;

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	public static BlockPos portalKeystone() {
		return new BlockPos(0, FLOOR_Y + 1, PORTAL_Z);
	}

	/** Feet position of the Heart (standing on the arena floor at the centre). */
	public static BlockPos heartHome() {
		return new BlockPos(0, FLOOR_Y + 1, 0);
	}

	/** Pylon pedestal centres (the first one is north, between the Heart and the portal). */
	public static double[] pylonAngles() {
		double[] angles = new double[5];
		for (int i = 0; i < 5; i++) {
			angles[i] = Math.toRadians(270 + 72 * i);
		}
		return angles;
	}

	public static int bottomY() {
		return FLOOR_Y - 50;
	}

	public static int topY() {
		return FLOOR_Y + WALL_HEIGHT + 1 + DOME_HEIGHT + SPIRE_TOP;
	}

	private static int hash(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return h & 0x7fffffff;
	}

	private static BlockState stone(int x, int y, int z) {
		int h = hash(x, y, z) % 100;
		if (h < 50) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (h < 72) {
			return ModBlocks.CRYSTALLIZED_STONE.getDefaultState();
		}
		if (h < 88) {
			return ModBlocks.INFECTED_STONE.getDefaultState();
		}
		return ModBlocks.CRYSTALLIZED_DEEPSLATE.getDefaultState();
	}

	private static double angleDeg(int dx, int dz) {
		return (Math.toDegrees(Math.atan2(dz, dx)) + 360) % 360;
	}

	public static BlockState stateAt(int dx, int y, int dz) {
		int ry = y - FLOOR_Y;
		double r = Math.sqrt(dx * dx + dz * dz);
		if (r > ISLAND_RADIUS) {
			return null;
		}
		if (ry < 0) {
			return foundation(dx, ry, dz, r);
		}
		if (ry == 0) {
			return floor(dx, dz, r);
		}
		BlockState spire = outerSpire(dx, ry, dz);
		if (spire != null) {
			return spire;
		}
		if (ry > WALL_HEIGHT) {
			return dome(dx, ry - WALL_HEIGHT - 1, dz, r);
		}
		if (r > 33) {
			return AIR;
		}
		if (r > ARENA_RADIUS + 0.5) {
			return wall(dx, ry, dz, r);
		}
		return interior(dx, ry, dz);
	}

	private static BlockState foundation(int dx, int ry, int dz, double r) {
		double depth = 6 + (ISLAND_RADIUS - r) * 0.85 + hash(dx, 7, dz) % 4;
		if (-ry > depth) {
			return null;
		}
		if (ry >= -2) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		int h = hash(dx, ry, dz) % 100;
		if (h < 4) {
			return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
		}
		if (h < 40) {
			return ModBlocks.INFECTED_STONE.getDefaultState();
		}
		if (h < 70) {
			return ModBlocks.CRYSTALLIZED_STONE.getDefaultState();
		}
		return ModBlocks.INFECTED_DEEPSLATE.getDefaultState();
	}

	private static BlockState floor(int dx, int dz, double r) {
		if (r <= ARENA_RADIUS) {
			if (r <= 5) {
				return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
			if (Math.abs(r - 10) < 0.55 || Math.abs(r - 18) < 0.55 || Math.abs(r - 28) < 0.55) {
				return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
			}
			return hash(dx, 1, dz) % 4 == 0 ? ModBlocks.CRYSTALLIZED_STONE.getDefaultState() : ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (r <= 33) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (dz > 0 && Math.abs(dx) <= 3) {
			return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
		}
		int h = hash(dx, 2, dz) % 100;
		if (h < 55) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (h < 80) {
			return ModBlocks.INFECTED_GRASS_BLOCK.getDefaultState();
		}
		return ModBlocks.CRYSTALLIZED_STONE.getDefaultState();
	}

	private static BlockState outerSpire(int dx, int ry, int dz) {
		if (ry > 22) {
			return null;
		}
		for (int k = 0; k < 4; k++) {
			double a = Math.toRadians(45 + 90 * k);
			double cx = Math.cos(a) * 41;
			double cz = Math.sin(a) * 41;
			double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cz) * (dz - cz));
			double radius = 3.2 * (1.0 - ry / 23.0);
			if (d <= radius) {
				if (ry >= 18) {
					return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
				}
				return d > radius - 1.0 ? ModBlocks.CLEAR_AMETHYST.getDefaultState() : ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
		}
		return null;
	}

	private static BlockState wall(int dx, int ry, int dz, double r) {
		if (dz > 0) {
			int half = ry <= 8 ? 3 : ry == 9 ? 2 : ry == 10 ? 1 : -1;
			if (Math.abs(dx) <= half) {
				return AIR;
			}
		}
		double a = angleDeg(dx, dz);
		if (ry >= 7 && ry <= 15 && a % 22.5 >= 9 && a % 22.5 < 13.5) {
			return ModBlocks.CLEAR_AMETHYST.getDefaultState();
		}
		if (ry == 4 && r <= 31.6 && a % 15 < 1.5) {
			return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
		}
		if (ry == WALL_HEIGHT || ry == 1) {
			return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
		}
		return stone(dx, ry, dz);
	}

	private static BlockState interior(int dx, int ry, int dz) {
		if (dz == PORTAL_Z && Math.abs(dx) <= 2 && ry <= 5) {
			return Blocks.AIR.getDefaultState();
		}
		for (int k = 0; k < 10; k++) {
			double a = Math.toRadians(36 * k);
			double cx = Math.cos(a) * 26;
			double cz = Math.sin(a) * 26;
			double d2 = (dx - cx) * (dx - cx) + (dz - cz) * (dz - cz);
			if (d2 <= 2.7) {
				if (ry == 1 || ry == WALL_HEIGHT) {
					return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
				}
				return ry == WALL_HEIGHT - 1 ? ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState() : ModBlocks.AMETHYST_BRICKS.getDefaultState();
			}
		}
		if (ry == 1) {
			for (double a : pylonAngles()) {
				double cx = Math.cos(a) * PYLON_RADIUS;
				double cz = Math.sin(a) * PYLON_RADIUS;
				double d2 = (dx - cx) * (dx - cx) + (dz - cz) * (dz - cz);
				if (d2 <= 2.3) {
					return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
				}
				if (d2 <= 5.2) {
					return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
				}
			}
		}
		return AIR;
	}

	private static BlockState dome(int dx, int h, int dz, double r) {
		if (h > DOME_HEIGHT) {
			int s = h - DOME_HEIGHT;
			double radius = 1.6 - s * 0.1;
			if (s <= SPIRE_TOP && r <= Math.max(0.5, radius)) {
				return s >= SPIRE_TOP - 3 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState() : ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
			return h - DOME_HEIGHT < 20 ? AIR : null;
		}
		double outer = 33.0 * (1.0 - Math.pow(h / (double) DOME_HEIGHT, 1.6));
		if (r > outer) {
			return AIR;
		}
		double inner = outer - 2.3;
		if (r > inner || outer < 2.5) {
			if (outer < 3.5) {
				return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
			}
			double a = angleDeg(dx, dz);
			if (a % 30 < 2.6 || a % 30 > 27.4) {
				return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
			if (h % 9 == 0) {
				return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
			}
			return ModBlocks.CLEAR_AMETHYST.getDefaultState();
		}
		return AIR;
	}
}
