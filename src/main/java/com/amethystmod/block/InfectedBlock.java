package com.amethystmod.block;

import com.amethystmod.InfectionSpread;
import com.amethystmod.ModGameRules;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Infected ground (and crystal leaves). It does not spread by itself any more: the infection grows only
 * around the cores ({@link com.amethystmod.CoreManager}), which the {@link com.amethystmod.InfectionManager}
 * paints outward with a fixed budget per tick. Once the Heart of the Amethyst has fallen, infected blocks
 * near players turn back into their old selves, one now and then: very slowly.
 */
public class InfectedBlock extends Block {
	/** One random tick in this many cleanses a block after the Heart's fall (a few hours for an area). */
	public static final int CLEANSE_CHANCE = 150;

	private final int crystalChance;

	public InfectedBlock(AbstractBlock.Settings settings, int crystalChance) {
		super(settings.ticksRandomly());
		this.crystalChance = crystalChance;
	}

	public int crystalChance() {
		return crystalChance;
	}

	/** True while the infection must not advance (stopped by the gamerule, or by the Heart's defeat). */
	public static boolean paused(ServerWorld world) {
		if (com.amethystmod.world.StoryState.infectionStopped && world.getRegistryKey() == World.OVERWORLD) {
			return true;
		}
		return world.getGameRules().getInt(ModGameRules.INFECTION_SPEED) <= 0;
	}

	/** After the Heart's fall: now and then an infected block in the normal world becomes what it was. */
	public static void slowlyCleanse(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (com.amethystmod.world.StoryState.infectionStopped && world.getRegistryKey() == World.OVERWORLD
				&& random.nextInt(CLEANSE_CHANCE) == 0) {
			BlockState clean = InfectionSpread.cleanse(state);
			if (clean != null) {
				world.setBlockState(pos, clean, Block.NOTIFY_LISTENERS);
			}
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		slowlyCleanse(state, world, pos, random);
	}
}
