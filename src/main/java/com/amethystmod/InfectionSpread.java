package com.amethystmod;

import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.InfectedBlock;
import com.amethystmod.block.InfectedLogBlock;
import com.amethystmod.block.ModBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

public class InfectionSpread {
	private static final int MAX_CRYSTALS_NEARBY = 3;

	/** The infected version of a block, or null if the block cannot be infected. */
	public static BlockState convert(BlockState state) {
		Block block = state.getBlock();
		if (state.isIn(BlockTags.LOGS) && state.contains(net.minecraft.block.PillarBlock.AXIS)) {
			return ModBlocks.INFECTED_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, state.get(net.minecraft.block.PillarBlock.AXIS));
		}
		if (state.isIn(BlockTags.LEAVES)) {
			return ModBlocks.INFECTED_LEAVES.getDefaultState();
		}
		if (block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.MYCELIUM) {
			return ModBlocks.INFECTED_GRASS_BLOCK.getDefaultState();
		}
		if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT) {
			return ModBlocks.INFECTED_DIRT.getDefaultState();
		}
		if (block == Blocks.SAND || block == Blocks.RED_SAND) {
			return ModBlocks.INFECTED_SAND.getDefaultState();
		}
		if (block == Blocks.DEEPSLATE) {
			return ModBlocks.INFECTED_DEEPSLATE.getDefaultState();
		}
		if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
			return ModBlocks.INFECTED_STONE.getDefaultState();
		}
		return null;
	}

	/**
	 * The infected version of a block for a core of the given phase. A young core (phase 1) only takes the
	 * living surface: soil, sand and trees. Older cores also crystallize bare stone.
	 */
	public static BlockState convertSurface(BlockState state, int phase) {
		if (phase < 2 && (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isOf(Blocks.DEEPSLATE))) {
			return null;
		}
		return convert(state);
	}

	/** Is this block already the infection (infected ground, wood, leaves or one of its crystals)? */
	public static boolean isInfected(BlockState state) {
		Block block = state.getBlock();
		return block instanceof InfectedBlock || block instanceof InfectedLogBlock || ModBlocks.CRYSTALS.contains(block);
	}

	/** The original block an infected block came from, or null if the block is not infected. */
	public static BlockState cleanse(BlockState state) {
		Block block = state.getBlock();
		if (block == ModBlocks.INFECTED_LOG) {
			return Blocks.OAK_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, state.get(net.minecraft.block.PillarBlock.AXIS));
		}
		if (block == ModBlocks.INFECTED_LEAVES) {
			return Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
		}
		if (block == ModBlocks.INFECTED_STONE || block == ModBlocks.CRYSTALLIZED_STONE) {
			return Blocks.STONE.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_DEEPSLATE || block == ModBlocks.CRYSTALLIZED_DEEPSLATE) {
			return Blocks.DEEPSLATE.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_DIRT) {
			return Blocks.DIRT.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_GRASS_BLOCK) {
			return Blocks.GRASS_BLOCK.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_SAND) {
			return Blocks.SAND.getDefaultState();
		}
		return null;
	}

	/** Burns the infection out of a few random blocks around {@code center}, shattering crystals too. */
	public static void purgeAround(ServerWorld world, BlockPos center, int radius, Random random, int attempts) {
		for (int i = 0; i < attempts; i++) {
			BlockPos pos = center.add(random.nextBetween(-radius, radius),
					random.nextBetween(-radius, radius), random.nextBetween(-radius, radius));
			BlockState state = world.getBlockState(pos);
			BlockState clean = cleanse(state);
			if (clean != null) {
				world.setBlockState(pos, clean);
			} else if (ModBlocks.CRYSTALS.contains(state.getBlock())) {
				world.breakBlock(pos, true);
			}
		}
	}

	/**
	 * Tries to grow a crystal of the given stage (0 bud, 1 crystal, 2 large) on the face of
	 * the block at {@code pos} that looks in {@code face}.
	 */
	public static boolean tryPlaceCrystal(WorldAccess world, BlockPos pos, Direction face, Random random, int stage) {
		BlockPos at = pos.offset(face);
		BlockState current = world.getBlockState(at);
		boolean water = current.isOf(Blocks.WATER);
		if (!current.isAir() && !water) {
			return false;
		}

		int nearby = 0;
		for (BlockPos p : BlockPos.iterate(at.add(-1, -1, -1), at.add(1, 1, 1))) {
			if (world.getBlockState(p).getBlock() instanceof AmethystClusterBlock && ++nearby >= MAX_CRYSTALS_NEARBY) {
				return false;
			}
		}

		BlockState crystal = ModBlocks.crystal(CrystalColor.random(random), stage).getDefaultState()
				.with(AmethystClusterBlock.FACING, face)
				.with(AmethystClusterBlock.WATERLOGGED, water);
		if (!crystal.canPlaceAt(world, at)) {
			return false;
		}
		world.setBlockState(at, crystal, Block.NOTIFY_ALL);
		return true;
	}
}
