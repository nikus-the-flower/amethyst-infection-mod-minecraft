package com.amethystmod;

import com.amethystmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.List;

/** A seed of the infection: a blob of infected blocks with crystals already growing on it. */
public class InfectionPatchFeature extends Feature<DefaultFeatureConfig> {
	public InfectionPatchFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		if (com.amethystmod.world.StoryState.infectionStopped
				&& world.toServerWorld().getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
			return false;
		}
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		int radius = 3 + random.nextInt(4);
		double limit = radius * radius * (0.65 + random.nextDouble() * 0.5);
		List<BlockPos> infected = new ArrayList<>();

		for (BlockPos p : BlockPos.iterate(origin.add(-radius, -radius, -radius), origin.add(radius, radius, radius))) {
			double dx = p.getX() - origin.getX();
			double dy = p.getY() - origin.getY();
			double dz = p.getZ() - origin.getZ();
			if (dx * dx + dy * dy * 1.4 + dz * dz > limit) {
				continue;
			}
			BlockState converted = InfectionSpread.convert(world.getBlockState(p));
			if (converted == null) {
				continue;
			}
			if (random.nextInt(8) == 0) {
				Block crystallized = ModBlocks.crystallized(converted.getBlock());
				if (crystallized != null) {
					converted = crystallized.getDefaultState();
				}
			}
			BlockPos immutable = p.toImmutable();
			world.setBlockState(immutable, converted, Block.NOTIFY_LISTENERS);
			infected.add(immutable);
		}

		for (BlockPos pos : infected) {
			for (Direction face : Direction.values()) {
				if (random.nextInt(6) == 0) {
					int roll = random.nextInt(10);
					int stage = roll < 4 ? 0 : roll < 8 ? 1 : 2;
					InfectionSpread.tryPlaceCrystal(world, pos, face, random, stage);
				}
			}
		}
		return !infected.isEmpty();
	}
}
