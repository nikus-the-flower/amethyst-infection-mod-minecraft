package com.amethystmod.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A crystal that grows bud -> crystal -> large crystal. A large crystal crystallizes the infected block
 * it grows from. Growth is driven by the {@link com.amethystmod.InfectionManager}, which takes a few
 * crystals per tick, so it is quick but never lags.
 */
public class GrowingCrystalBlock extends AmethystClusterBlock {
	private final Block next;

	public GrowingCrystalBlock(int height, int xzOffset, AbstractBlock.Settings settings, Block next) {
		super(height, xzOffset, settings.ticksRandomly());
		this.next = next;
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		if (world instanceof ServerWorld server && !oldState.isOf(this)) {
			com.amethystmod.InfectionManager.addGrowth(server, pos, this, 2);
		}
	}

	/** Safety net for crystals placed during world generation or loaded from old saves. */
	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (com.amethystmod.world.StoryState.infectionStopped && world.getRegistryKey() == World.OVERWORLD) {
			// after the Heart's fall the crystals of the normal world crumble away, one now and then
			if (random.nextInt(InfectedBlock.CLEANSE_CHANCE) == 0) {
				world.breakBlock(pos, false);
			}
			return;
		}
		com.amethystmod.InfectionManager.offerGrowth(world, pos, this, random);
	}

	/** One growth step: the next stage, or (for a large crystal) crystallizing the block it grows from. */
	public void grow(ServerWorld world, BlockPos pos, BlockState state) {
		if (next != null) {
			world.setBlockState(pos, next.getDefaultState()
					.with(FACING, state.get(FACING))
					.with(WATERLOGGED, state.get(WATERLOGGED)), Block.NOTIFY_LISTENERS);
			return;
		}
		BlockPos support = pos.offset(state.get(FACING).getOpposite());
		Block crystallized = ModBlocks.crystallized(world.getBlockState(support).getBlock());
		if (crystallized != null) {
			world.setBlockState(support, crystallized.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}
}
