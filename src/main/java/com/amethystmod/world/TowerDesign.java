package com.amethystmod.world;

import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the Amethyst Tower as a pure function: which block belongs at (dx, y, dz), where dx/dz
 * are measured from the tower axis at world 0,0 and y is absolute. Returns null where the tower does not
 * touch the world, air where it must be cleared.
 *
 * <p>The tower looks like a giant amethyst druse: an eight-sided crystal shaft (12 blocks inside,
 * walls out to 14.5) hidden in a cluster of hexagonal crystals with pointed tips. The biggest ones
 * sprawl from a mound of host rock at the bottom; tier after tier they get smaller and steeper toward
 * the top, where only the point of the central crystal rises over them. Inside, five floors 14 blocks apart
 * are joined by a spiral stair of half-steps along the wall; floors have glowing inlays, crystals hang
 * from the ceilings, and the sixth floor is Amitariel's hall with crystal pillars around the dais.
 */
public final class TowerDesign {
	public static final int FLOOR_HEIGHT = 14;
	public static final int BOSS_FLOOR = 5;
	/** Height of the hall above the boss floor before the crystal roof begins. */
	public static final int HALL_HEIGHT = 19;
	/** Height of the hollow crystal point over the hall: the tip of the central crystal. */
	public static final int ROOF_HEIGHT = 20;
	public static final double THETA0 = -Math.PI / 2;
	/** Inner radius and outer radius of the eight-sided shaft (measured as an octagon). */
	private static final double INNER = 12.0;
	private static final double OUTER = 14.5;
	private static final int STEPS = 28;
	/** Steps from this index up are under the ceiling slab and need a hole. */
	private static final int HOLE_FROM_STEP = 19;
	/** Angles (degrees) of the crystal pillars in the boss hall, clear of the stairwell. */
	private static final int[] PILLARS = {-60, -20, 20, 60, 100, 140};
	/** How far out the outer crystals reach (the builder scans this square). */
	public static final int REACH = 52;

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	public final int baseY;
	/**
	 * Outer crystals: base x, y, z, direction x, y, z, length, radius, then the two axes across the
	 * crystal (u, v) and its bounding box (min x, y, z, max x, y, z).
	 */
	private final List<double[]> crystals = new ArrayList<>();
	/** Radius of the mound of host rock the cluster grows from. */
	private static final double MOUND = 30;

	public TowerDesign(int baseY) {
		this.baseY = baseY;
		// tiers of crystals around the shaft: {height above the base, count, tilt from vertical (deg),
		// tilt spread, length, length spread, radius, radius spread}. Big and sprawling at the bottom,
		// smaller and steeper toward the top, like a real druse.
		double[][] tiers = {
				{-4, 8, 56, 10, 31, 8, 6.2, 1.4},
				{8, 9, 42, 9, 29, 6, 5.2, 1.0},
				{22, 9, 32, 8, 27, 6, 4.6, 0.9},
				{38, 8, 24, 7, 25, 5, 4.1, 0.8},
				{54, 8, 17, 6, 23, 5, 3.6, 0.7},
				{68, 7, 10, 5, 21, 5, 3.1, 0.6}};
		for (int k = 0; k < tiers.length; k++) {
			double[] t = tiers[k];
			int count = (int) t[1];
			for (int i = 0; i < count; i++) {
				double deg = (i + (k % 2) * 0.5) * 360.0 / count + hash(k, i, 3) % 17 - 8;
				if (k <= 2 && Math.abs(((deg % 360) + 360) % 360 - 90) < (k == 0 ? 32 : 24)) {
					continue; // keep the way to the door (south) free
				}
				double tilt = t[2] + (hash(k, i, 4) % 100) / 100.0 * t[3] - t[3] / 2;
				double length = t[4] + (hash(k, i, 5) % 100) / 100.0 * t[5] - t[5] / 2;
				double radius = t[6] + (hash(k, i, 6) % 100) / 100.0 * t[7] - t[7] / 2;
				double a = Math.toRadians(deg);
				addCrystal(Math.cos(a) * 12.5, baseY + t[0], Math.sin(a) * 12.5, a, Math.toRadians(tilt), length, radius,
						Math.toRadians(hash(k, i, 7) % 60));
			}
		}
	}

	private void addCrystal(double x, double y, double z, double angle, double tilt, double length, double radius, double spin) {
		double dx = Math.cos(angle) * Math.sin(tilt);
		double dy = Math.cos(tilt);
		double dz = Math.sin(angle) * Math.sin(tilt);
		// two axes across the crystal (u sideways, v = d x u), turned by its own spin,
		// so the six faces sit differently on each crystal
		double ux = -Math.sin(angle);
		double uz = Math.cos(angle);
		double vx = dy * uz;
		double vy = dz * ux - dx * uz;
		double vz = -dy * ux;
		double cs = Math.cos(spin);
		double sn = Math.sin(spin);
		double[] u = {ux * cs + vx * sn, vy * sn, uz * cs + vz * sn};
		double[] v = {-ux * sn + vx * cs, vy * cs, -uz * sn + vz * cs};
		double ex = x + dx * length;
		double ey = y + dy * length;
		double ez = z + dz * length;
		crystals.add(new double[]{x, y, z, dx, dy, dz, length, radius, u[0], u[1], u[2], v[0], v[1], v[2],
				Math.min(x, ex) - radius - 1, Math.min(y, ey) - radius - 1, Math.min(z, ez) - radius - 1,
				Math.max(x, ex) + radius + 1, Math.max(y, ey) + radius + 1, Math.max(z, ez) + radius + 1});
	}

	public int floorY(int floor) {
		return baseY + floor * FLOOR_HEIGHT;
	}

	public int roofStartY() {
		return floorY(BOSS_FLOOR) + HALL_HEIGHT;
	}

	public int topY() {
		int top = roofStartY() + ROOF_HEIGHT;
		for (double[] c : crystals) {
			top = Math.max(top, (int) Math.ceil(c[1] + c[4] * c[6]) + 1);
		}
		return top;
	}

	/** Lowest y the outer crystals reach (they grow out of the ground). */
	public int bottomY() {
		return baseY - 4;
	}

	/** Where the rift opens in Amitariel's hall: on top of the dais. */
	public BlockPos riftPos() {
		return new BlockPos(0, floorY(BOSS_FLOOR) + 3, 0);
	}

	/** Where travellers from the world of the Amethyst arrive: on the hall floor, facing the rift. */
	public BlockPos arrivalPos() {
		return new BlockPos(0, floorY(BOSS_FLOOR) + 1, 8);
	}

	private static int hash(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return h & 0x7fffffff;
	}

	/** Distance from the axis measured as a regular octagon (flat faces north, south, east, west). */
	private static double octagon(int dx, int dz) {
		int ax = Math.abs(dx);
		int az = Math.abs(dz);
		return Math.max(Math.max(ax, az), (ax + az) * 0.7071);
	}

	private static double angleDeg(int dx, int dz) {
		return (Math.toDegrees(Math.atan2(dz, dx)) + 360) % 360;
	}

	private static BlockState stoneMix(int x, int y, int z) {
		int h = hash(x, y, z) % 100;
		if (h < 40) {
			return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
		}
		if (h < 70) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (h < 88) {
			return ModBlocks.CRYSTALLIZED_STONE.getDefaultState();
		}
		return ModBlocks.CRYSTALLIZED_DEEPSLATE.getDefaultState();
	}

	/** Which of the 28 steps of the spiral a column belongs to. */
	private static int stepIndex(int dx, int dz) {
		double theta = Math.atan2(dz, dx) - THETA0;
		theta = ((theta % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
		return Math.min(STEPS - 1, (int) (theta / (Math.PI * 2) * STEPS));
	}

	public BlockState stateAt(int dx, int y, int dz) {
		int ry = y - baseY;
		double r = octagon(dx, dz);
		int hallTop = BOSS_FLOOR * FLOOR_HEIGHT + HALL_HEIGHT;
		if (ry >= hallTop) {
			BlockState roof = roof(dx, ry - hallTop, dz);
			return roof != null ? roof : outerCrystal(dx, y, dz, r, ry);
		}
		if (ry < 0 || r > OUTER) {
			return outerCrystal(dx, y, dz, r, ry);
		}
		if (r > INNER) {
			return wall(dx, ry, dz, r);
		}
		int floor = ry / FLOOR_HEIGHT;
		int rel = ry % FLOOR_HEIGHT;

		// the spiral staircase hugs the inner wall
		if (r >= 8.5) {
			int flight = (ry - 1) / FLOOR_HEIGHT;
			int stepRel = ry - flight * FLOOR_HEIGHT;
			if (ry >= 1 && flight <= BOSS_FLOOR - 1 && stepRel >= 1) {
				int i = stepIndex(dx, dz);
				if (i == 2 * (stepRel - 1)) {
					return ModBlocks.AMETHYST_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
				}
				if (i == 2 * stepRel - 1) {
					return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
				}
			}
		}

		if (rel == 0 && floor <= BOSS_FLOOR) {
			boolean hole = floor >= 1 && r >= 8 && r <= INNER + 0.2 && stepIndex(dx, dz) >= HOLE_FROM_STEP;
			return hole ? AIR : floorBlock(dx, y, dz, r);
		}

		if (floor == BOSS_FLOOR) {
			return hall(dx, ry - BOSS_FLOOR * FLOOR_HEIGHT, dz, r, y);
		}

		boolean stairZone = r >= 8 && stepIndex(dx, dz) >= HOLE_FROM_STEP - 2;
		// a crystal chandelier hangs over the middle of every floor
		if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && rel >= FLOOR_HEIGHT - 3) {
			if (dx == 0 && dz == 0) {
				return rel == FLOOR_HEIGHT - 3
						? ModBlocks.crystal(CrystalColor.VIOLET, 2).getDefaultState().with(AmethystClusterBlock.FACING, Direction.DOWN)
						: ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
			}
			if (rel == FLOOR_HEIGHT - 1 && (dx == 0 || dz == 0)) {
				return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
		}
		// crystals hanging from the ceiling
		if (rel == FLOOR_HEIGHT - 1 && r < INNER - 0.5 && !stairZone && hash(dx, y, dz) % 100 < 5) {
			CrystalColor color = CrystalColor.values()[hash(dz, y, dx) % 3];
			return ModBlocks.crystal(color, 1 + hash(dx + 3, y, dz) % 2).getDefaultState().with(AmethystClusterBlock.FACING, Direction.DOWN);
		}
		// crystals growing on the floor
		if (rel == 1 && r < 8) {
			boolean doorway = floor == 0 && dz > 0 && Math.abs(dx) <= 3;
			boolean centre = r < 2;
			if (!doorway && !centre && hash(dx, y, dz) % 100 < 4) {
				CrystalColor color = CrystalColor.values()[hash(dz, y, dx) % 3];
				return ModBlocks.crystal(color, 1 + hash(dx + 5, y, dz) % 2).getDefaultState()
						.with(AmethystClusterBlock.FACING, Direction.UP);
			}
		}
		// crystals growing sideways out of the walls
		if (r > INNER - 1 && rel >= 3 && rel <= 9 && !stairZone && hash(dx, y + 77, dz) % 100 < 6) {
			Direction facing = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
			return ModBlocks.crystal(CrystalColor.values()[hash(dx, y, dz) % 3], 1).getDefaultState().with(AmethystClusterBlock.FACING, facing);
		}
		return AIR;
	}

	/** Amitariel's hall: the dais in the middle and six crystal pillars around it. */
	private BlockState hall(int dx, int up, int dz, double r, int y) {
		if (up == 1 && r <= 6) {
			return r >= 5 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState() : ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
		}
		if (up == 2 && r <= 3) {
			return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
		}
		for (int a : PILLARS) {
			double px = Math.cos(Math.toRadians(a)) * 9.5;
			double pz = Math.sin(Math.toRadians(a)) * 9.5;
			double d2 = (dx - px) * (dx - px) + (dz - pz) * (dz - pz);
			// pillars widen at the foot and the top, like crystals growing from floor and ceiling
			double width = 1.3 + (up <= 2 || up >= HALL_HEIGHT - 2 ? 0.8 : 0);
			if (d2 <= width * width) {
				return d2 <= 0.5 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState() : ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
		}
		boolean arrival = Math.abs(dx) <= 1 && dz > 5 && dz < 11;
		if (up == 1 && r > 6.5 && r < 8.5 && !arrival && hash(dx, y, dz) % 100 < 6) {
			return ModBlocks.crystal(CrystalColor.ROSE, 2).getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP);
		}
		return AIR;
	}

	private BlockState floorBlock(int dx, int y, int dz, double r) {
		// glowing rings and rays inlaid in the floor
		if (r < 1.5 || Math.abs(r - 6) < 0.5) {
			return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
		}
		double a = angleDeg(dx, dz) % 45;
		if (r > 2 && r < INNER && (a < 2.5 || a > 42.5)) {
			return ModBlocks.CLEAR_AMETHYST.getDefaultState();
		}
		int h = hash(dx, y, dz) % 100;
		if (h < 60) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		if (h < 85) {
			return ModBlocks.CRYSTALLIZED_STONE.getDefaultState();
		}
		return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
	}

	private BlockState wall(int dx, int ry, int dz, double r) {
		// the entrance, on the south side, at ground level
		if (ry >= 1 && ry <= 5 && dz > 0) {
			int half = ry == 5 ? 1 : 2;
			if (Math.abs(dx) <= half) {
				return AIR;
			}
		}
		double angle = angleDeg(dx, dz);
		// the edges of the eight faces are veins of glowing crystal
		double toEdge = Math.abs(((angle - 22.5) % 45 + 45) % 45 - 22.5);
		if (toEdge > 18.5 && r > INNER + 1) {
			return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
		}
		int rel = ry % FLOOR_HEIGHT;
		// tall windows in the middle of each face
		if (rel >= 4 && rel <= 10 && ry > 6 && toEdge < 2.5) {
			return ModBlocks.CLEAR_AMETHYST.getDefaultState();
		}
		if (ry < 2) {
			return ModBlocks.AMETHYST_BRICKS.getDefaultState();
		}
		return stoneMix(dx, ry, dz);
	}

	/** The tip of the central crystal over the hall: hollow inside, with a glowing crystal hanging down. */
	private BlockState roof(int dx, int h, int dz) {
		if (h >= ROOF_HEIGHT) {
			return null;
		}
		double outer = OUTER * (1.0 - (double) h / ROOF_HEIGHT);
		double r = octagon(dx, dz);
		if (r > outer) {
			return null;
		}
		double inner = INNER * (1.0 - (double) h / (ROOF_HEIGHT - 5));
		if (h < ROOF_HEIGHT - 5 && r <= inner) {
			if (dx == 0 && dz == 0 && h > 3) {
				return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
			}
			return AIR;
		}
		if (h >= ROOF_HEIGHT - 3) {
			return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
		}
		double toEdge = Math.abs(((angleDeg(dx, dz) - 22.5) % 45 + 45) % 45 - 22.5);
		return toEdge > 19 ? ModBlocks.CLEAR_AMETHYST.getDefaultState() : ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
	}

	/**
	 * The cluster outside the shaft: hexagonal crystals with pointed tips (pale at the root, deep violet
	 * along the body, a glowing point), and the mound of host rock they grow from. Never inside the tower.
	 */
	private BlockState outerCrystal(int dx, int y, int dz, double r, int ry) {
		if (r <= OUTER && ry >= 0 && ry < BOSS_FLOOR * FLOOR_HEIGHT + HALL_HEIGHT) {
			return null;
		}
		double px = dx + 0.5;
		double py = y + 0.5;
		double pz = dz + 0.5;
		for (double[] c : crystals) {
			if (px < c[14] || py < c[15] || pz < c[16] || px > c[17] || py > c[18] || pz > c[19]) {
				continue;
			}
			double wx = px - c[0];
			double wy = py - c[1];
			double wz = pz - c[2];
			double t = wx * c[3] + wy * c[4] + wz * c[5];
			double length = c[6];
			if (t < 0 || t > length) {
				continue;
			}
			// hexagonal cross-section, sharpening into a six-sided point at the end
			double radius = c[7];
			double tip = radius * 1.7;
			double size = t > length - tip ? radius * (length - t) / tip : radius;
			double a = wx * c[8] + wy * c[9] + wz * c[10];
			double b = wx * c[11] + wy * c[12] + wz * c[13];
			double hex = Math.max(Math.abs(a), Math.max(Math.abs(0.5 * a + 0.866 * b), Math.abs(0.5 * a - 0.866 * b)));
			if (hex <= size * 0.866) {
				if (length - t < 1.6) {
					return ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
				}
				if (t < length * 0.18 && hash(dx, y, dz) % 3 != 0) {
					return ModBlocks.CLEAR_AMETHYST.getDefaultState();
				}
				return ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
			}
		}
		return mound(dx, y, dz, ry);
	}

	/** The low mound of host rock at the foot of the cluster, with small crystals on it. */
	private BlockState mound(int dx, int y, int dz, int ry) {
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d > MOUND || ry < -3 || d <= OUTER) {
			return null;
		}
		if (ry >= 1 && dz > 0 && Math.abs(dx) <= 3) {
			return null; // the way to the door
		}
		double top = 6.5 * (1.0 - (d / MOUND) * (d / MOUND)) + (hash(dx, 0, dz) % 3) * 0.5;
		if (ry <= top) {
			int h = hash(dx, y, dz) % 10;
			return h < 5 ? ModBlocks.CRYSTALLIZED_STONE.getDefaultState()
					: h < 8 ? ModBlocks.INFECTED_STONE.getDefaultState() : ModBlocks.CRYSTALLIZED_DEEPSLATE.getDefaultState();
		}
		if (ry == (int) Math.floor(top) + 1 && hash(dx, y + 9, dz) % 100 < 9) {
			CrystalColor color = CrystalColor.values()[hash(dz, y, dx) % 3];
			return ModBlocks.crystal(color, 1 + hash(dx + 7, y, dz) % 2).getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP);
		}
		return null;
	}

	/** True for blocks the build must never replace with something else when the tower is rebuilt. */
	public static boolean isTowerBlock(Block block) {
		return block == ModBlocks.AMETHYST_BRICKS || block == ModBlocks.AMETHYST_CRYSTAL_BLOCK
				|| block == ModBlocks.BRILLIANT_AMETHYST || block == ModBlocks.AMETHYST_BRICK_SLAB;
	}
}
