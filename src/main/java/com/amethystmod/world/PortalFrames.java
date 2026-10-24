package com.amethystmod.world;

import com.amethystmod.block.AmethystPortalBlock;
import com.amethystmod.block.ModBlocks;
import com.amethystmod.block.PortalKeystoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The square amethyst portal: a 5x5 frame with a 3x3 opening. The keystone is the middle block of
 * the bottom row; the frame runs along X or Z.
 */
public final class PortalFrames {
	private PortalFrames() {
	}

	private static Direction.Axis axisAt(World world, BlockPos keystone) {
		return world.getBlockState(keystone.east()).isOf(ModBlocks.PORTAL_FRAME) ? Direction.Axis.X : Direction.Axis.Z;
	}

	private static Direction along(Direction.Axis axis) {
		return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
	}

	/** Fills the opening with the portal veil. */
	public static void fill(World world, BlockPos keystone) {
		Direction.Axis axis = axisAt(world, keystone);
		BlockState veil = ModBlocks.AMETHYST_PORTAL.getDefaultState().with(AmethystPortalBlock.AXIS, axis);
		for (int d = -1; d <= 1; d++) {
			for (int h = 1; h <= 3; h++) {
				world.setBlockState(keystone.offset(along(axis), d).up(h), veil, Block.NOTIFY_LISTENERS);
			}
		}
	}

	/** Places a whole frame (keystone at {@code keystone}) running along {@code axis}; lit frames get the veil. */
	public static void build(World world, BlockPos keystone, Direction.Axis axis, boolean lit) {
		for (int d = -2; d <= 2; d++) {
			for (int h = 0; h <= 4; h++) {
				BlockPos p = keystone.offset(along(axis), d).up(h);
				boolean edge = Math.abs(d) == 2 || h == 0 || h == 4;
				if (d == 0 && h == 0) {
					world.setBlockState(p, ModBlocks.PORTAL_KEYSTONE.getDefaultState().with(PortalKeystoneBlock.LIT, lit), Block.NOTIFY_LISTENERS);
				} else if (edge) {
					world.setBlockState(p, ModBlocks.PORTAL_FRAME.getDefaultState(), Block.NOTIFY_LISTENERS);
				} else if (!lit) {
					world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
		if (lit) {
			fill(world, keystone);
		}
	}

	/** Takes a frame (and its veil) away: the old portals were replaced by rifts. */
	public static void remove(World world, BlockPos keystone) {
		Direction.Axis axis = axisAt(world, keystone);
		for (int d = -2; d <= 2; d++) {
			for (int h = 0; h <= 4; h++) {
				world.setBlockState(keystone.offset(along(axis), d).up(h), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}

	/** Lights an existing frame (as if the core were put in). */
	public static void open(World world, BlockPos keystone) {
		BlockState state = world.getBlockState(keystone);
		if (state.isOf(ModBlocks.PORTAL_KEYSTONE)) {
			world.setBlockState(keystone, state.with(PortalKeystoneBlock.LIT, true), Block.NOTIFY_LISTENERS);
		}
		fill(world, keystone);
	}
}
