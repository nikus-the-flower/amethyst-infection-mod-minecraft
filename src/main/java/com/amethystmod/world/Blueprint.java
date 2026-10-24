package com.amethystmod.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure drawn in local coordinates (y = 0 is the ground floor, built on the terrain at the site),
 * with a little drawing kit for medieval stone: walls that crumble, mossy and cracked bricks, stairs and
 * slabs. It also remembers chests (with what goes in them) and markers (where creatures, Ricardo or the
 * altar stand). {@link SiteBuilder} places it a few thousand blocks per tick.
 */
public final class Blueprint {
	public final Map<BlockPos, BlockState> blocks = new HashMap<>();
	public final List<Chest> chests = new ArrayList<>();
	public final List<Marker> markers = new ArrayList<>();
	/** Columns (x, z) whose lowest block gets a foundation down to the terrain. */
	public boolean foundations = true;
	public final Random random;

	/** A chest at a local spot, filled from a loot table, with special items added. */
	public record Chest(BlockPos pos, Direction facing, String lootTable, List<String> extras) {
	}

	/** A named spot: "guard:<entity id>", "ricardo", "altar", "pedestal", ... */
	public record Marker(BlockPos pos, String kind) {
	}

	public Blueprint(long seed) {
		this.random = Random.create(seed);
	}

	public void set(int x, int y, int z, BlockState state) {
		blocks.put(new BlockPos(x, y, z), state);
	}

	public void set(int x, int y, int z, Block block) {
		set(x, y, z, block.getDefaultState());
	}

	public BlockState get(int x, int y, int z) {
		return blocks.get(new BlockPos(x, y, z));
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, state);
				}
			}
		}
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		fill(x0, y0, z0, x1, y1, z1, block.getDefaultState());
	}

	/** Air in a box (clears trees and terrain). */
	public void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(x0, y0, z0, x1, y1, z1, Blocks.AIR.getDefaultState());
	}

	public void remove(int x, int y, int z) {
		blocks.remove(new BlockPos(x, y, z));
	}

	// ---- medieval stone ---------------------------------------------------------------------

	/** Stone bricks, now and then mossy or cracked. */
	public BlockState brick() {
		int r = random.nextInt(100);
		return r < 58 ? Blocks.STONE_BRICKS.getDefaultState()
				: r < 76 ? Blocks.MOSSY_STONE_BRICKS.getDefaultState()
				: r < 92 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
				: Blocks.COBBLESTONE.getDefaultState();
	}

	/** Rough stone: cobblestone, mossy cobblestone, andesite, stone. */
	public BlockState rubble() {
		int r = random.nextInt(100);
		return r < 45 ? Blocks.COBBLESTONE.getDefaultState()
				: r < 70 ? Blocks.MOSSY_COBBLESTONE.getDefaultState()
				: r < 85 ? Blocks.ANDESITE.getDefaultState()
				: Blocks.STONE.getDefaultState();
	}

	/** Old paving: stone bricks, cobble and gravel, with grass coming through. */
	public BlockState paving() {
		int r = random.nextInt(100);
		return r < 40 ? brick()
				: r < 65 ? Blocks.COBBLESTONE.getDefaultState()
				: r < 80 ? Blocks.ANDESITE.getDefaultState()
				: r < 92 ? Blocks.GRAVEL.getDefaultState()
				: Blocks.MOSS_BLOCK.getDefaultState();
	}

	public static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
		return block.getDefaultState().with(StairsBlock.FACING, facing)
				.with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
	}

	public static BlockState slab(Block block, boolean top) {
		return block.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	/**
	 * A ruined wall from (x0, z0) to (x1, z1) (straight along x or z), {@code height} high: the top is
	 * broken unevenly and blocks are missing here and there, more of them higher up.
	 */
	public void ruinedWall(int x0, int z0, int x1, int z1, int y0, int height, float decay) {
		int len = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
		int dx = Integer.signum(x1 - x0);
		int dz = Integer.signum(z1 - z0);
		// a smooth broken skyline: a random walk of the top
		int top = height;
		for (int i = 0; i <= len; i++) {
			if (random.nextFloat() < decay) {
				top += random.nextBoolean() ? -1 : 1;
			}
			top = Math.max(Math.max(1, height / 3), Math.min(height, top));
			int x = x0 + dx * i;
			int z = z0 + dz * i;
			for (int y = 0; y < top; y++) {
				float gone = decay * 0.35f * y / Math.max(1, height);
				if (y > 0 && random.nextFloat() < gone) {
					continue;
				}
				set(x, y0 + y, z, brick());
			}
		}
	}

	/** A round tower, broken at the top; hollow inside, with its floor. */
	public void ruinedTower(int cx, int cz, int radius, int y0, int height, float decay) {
		int top = height;
		for (int a = 0; a < 64; a++) {
			double ang = a * Math.PI * 2 / 64;
			if (random.nextFloat() < decay) {
				top += random.nextBoolean() ? -2 : 2;
			}
			top = Math.max(height / 3, Math.min(height, top));
			int x = cx + (int) Math.round(Math.cos(ang) * radius);
			int z = cz + (int) Math.round(Math.sin(ang) * radius);
			for (int y = 0; y < top; y++) {
				if (y > 0 && random.nextFloat() < decay * 0.3f * y / height) {
					continue;
				}
				set(x, y0 + y, z, brick());
			}
		}
		for (int x = -radius + 1; x < radius; x++) {
			for (int z = -radius + 1; z < radius; z++) {
				if (x * x + z * z < radius * radius - radius) {
					set(cx + x, y0 - 1, cz + z, paving());
					for (int y = 0; y < height + 2; y++) {
						if (get(cx + x, y0 + y, cz + z) == null) {
							set(cx + x, y0 + y, cz + z, Blocks.AIR.getDefaultState());
						}
					}
				}
			}
		}
	}

	/** Scatters rubble (single stones, slabs, mossy blocks) on the ground in a box. */
	public void rubbleField(int x0, int z0, int x1, int z1, int y, int count) {
		for (int i = 0; i < count; i++) {
			int x = x0 + random.nextInt(Math.max(1, x1 - x0 + 1));
			int z = z0 + random.nextInt(Math.max(1, z1 - z0 + 1));
			if (get(x, y, z) != null && !get(x, y, z).isAir()) {
				continue;
			}
			int r = random.nextInt(10);
			BlockState s = r < 4 ? rubble() : r < 7 ? slab(Blocks.COBBLESTONE_SLAB, false)
					: r < 9 ? slab(Blocks.STONE_BRICK_SLAB, false) : Blocks.MOSSY_COBBLESTONE.getDefaultState();
			set(x, y, z, s);
		}
	}

	public void chest(int x, int y, int z, Direction facing, String lootTable, String... extras) {
		chests.add(new Chest(new BlockPos(x, y, z), facing, lootTable, List.of(extras)));
		set(x, y, z, Blocks.CHEST.getDefaultState().with(net.minecraft.block.ChestBlock.FACING, facing));
	}

	public void marker(int x, int y, int z, String kind) {
		markers.add(new Marker(new BlockPos(x, y, z), kind));
	}

	/** Lowest and highest local coordinates used. */
	public int[] bounds() {
		int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
		for (BlockPos p : blocks.keySet()) {
			b[0] = Math.min(b[0], p.getX());
			b[1] = Math.min(b[1], p.getY());
			b[2] = Math.min(b[2], p.getZ());
			b[3] = Math.max(b[3], p.getX());
			b[4] = Math.max(b[4], p.getY());
			b[5] = Math.max(b[5], p.getZ());
		}
		return b;
	}
}
