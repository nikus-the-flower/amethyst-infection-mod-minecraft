package com.amethystmod.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** The trunk of an infected tree: crystallized wood. After the Heart falls it slowly turns back into wood. */
public class InfectedLogBlock extends PillarBlock {
	public InfectedLogBlock(AbstractBlock.Settings settings) {
		super(settings.ticksRandomly());
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		InfectedBlock.slowlyCleanse(state, world, pos, random);
	}
}
