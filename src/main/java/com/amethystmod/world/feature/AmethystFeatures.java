package com.amethystmod.world.feature;

import com.amethystmod.AmethystMod;
import com.amethystmod.InfectionSpread;
import com.amethystmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Decoration features of the Amethyst world. */
public final class AmethystFeatures {
	private AmethystFeatures() {
	}

	public static void register() {
		Registry.register(Registries.FEATURE, Identifier.of(AmethystMod.MOD_ID, "crystal_spire"), new CrystalSpire(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, Identifier.of(AmethystMod.MOD_ID, "crystal_scatter"), new CrystalScatter(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, Identifier.of(AmethystMod.MOD_ID, "floating_shard"), new FloatingShard(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, Identifier.of(AmethystMod.MOD_ID, "crystal_pool"), new CrystalPool(DefaultFeatureConfig.CODEC));
	}

	/**
	 * A small lake of water rimmed with clear amethyst. Replaces the vanilla lake feature, which looks up
	 * biomes around it and could ask for a neighbouring chunk that was not ready yet (the whole chunk then
	 * failed to generate and the game hung waiting for it). This one only touches blocks within 6 of its
	 * centre and never asks for biomes.
	 */
	public static class CrystalPool extends Feature<DefaultFeatureConfig> {
		public CrystalPool(Codec<DefaultFeatureConfig> codec) {
			super(codec);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
			StructureWorldAccess world = context.getWorld();
			Random random = context.getRandom();
			BlockPos origin = context.getOrigin();
			int rx = 3 + random.nextInt(4);
			int rz = 3 + random.nextInt(4);
			int depth = 2 + random.nextInt(3);
			int surface = origin.getY() - 1;
			// only on fairly flat solid ground
			for (int dx = -rx; dx <= rx; dx++) {
				for (int dz = -rz; dz <= rz; dz++) {
					BlockPos p = new BlockPos(origin.getX() + dx, surface, origin.getZ() + dz);
					if (!world.getBlockState(p).isSolidBlock(world, p) || !world.getBlockState(p.down(depth)).isSolidBlock(world, p.down(depth))) {
						return false;
					}
				}
			}
			java.util.List<BlockPos> water = new java.util.ArrayList<>();
			for (int dx = -rx; dx <= rx; dx++) {
				for (int dz = -rz; dz <= rz; dz++) {
					double e = (dx * dx) / (double) (rx * rx) + (dz * dz) / (double) (rz * rz);
					if (e > 1.0) {
						continue;
					}
					int d = Math.max(1, (int) Math.round(depth * (1.0 - e * 0.7)));
					for (int y = 0; y < d; y++) {
						BlockPos p = new BlockPos(origin.getX() + dx, surface - y, origin.getZ() + dz);
						world.setBlockState(p, net.minecraft.block.Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS);
						water.add(p);
					}
					for (int y = 1; y <= 3; y++) {
						BlockPos p = new BlockPos(origin.getX() + dx, surface + y, origin.getZ() + dz);
						if (!world.getBlockState(p).isAir()) {
							world.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
						}
					}
				}
			}
			// a rim of clear amethyst wherever the water would run out
			for (BlockPos p : water) {
				for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
					BlockPos n = p.offset(dir);
					BlockState s = world.getBlockState(n);
					if (!s.isOf(net.minecraft.block.Blocks.WATER) && !s.isSolidBlock(world, n)) {
						world.setBlockState(n, ModBlocks.CLEAR_AMETHYST.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
			}
			return !water.isEmpty();
		}
	}

	private static boolean replaceable(BlockState state) {
		return state.isAir() || state.isReplaceable();
	}

	/**
	 * A giant crystal growing out of the ground, leaning a little, with smaller crystals around its
	 * foot: the "trees" of the crystal groves.
	 */
	public static class CrystalSpire extends Feature<DefaultFeatureConfig> {
		public CrystalSpire(Codec<DefaultFeatureConfig> codec) {
			super(codec);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
			StructureWorldAccess world = context.getWorld();
			Random random = context.getRandom();
			BlockPos origin = context.getOrigin();
			if (world.getBlockState(origin.down()).isAir()) {
				return false;
			}
			spike(world, origin, 12 + random.nextInt(18), 1.8 + random.nextDouble() * 1.6, random);
			for (int i = 0; i < 2 + random.nextInt(4); i++) {
				BlockPos side = origin.add(random.nextBetween(-5, 5), 0, random.nextBetween(-5, 5));
				for (int dy = 3; dy > -4; dy--) {
					if (!world.getBlockState(side.up(dy - 1)).isAir() && world.getBlockState(side.up(dy)).isAir()) {
						spike(world, side.up(dy), 4 + random.nextInt(7), 0.8 + random.nextDouble() * 0.8, random);
						break;
					}
				}
			}
			return true;
		}

		private static void spike(StructureWorldAccess world, BlockPos base, int height, double radius, Random random) {
			double leanX = (random.nextDouble() - 0.5) * 0.5;
			double leanZ = (random.nextDouble() - 0.5) * 0.5;
			int r = (int) Math.ceil(radius) + 1;
			for (int y = -2; y < height; y++) {
				double t = Math.max(0, y) / (double) height;
				double rad = radius * (1.0 - t * 0.92);
				double cx = leanX * y;
				double cz = leanZ * y;
				for (int dx = -r - 3; dx <= r + 3; dx++) {
					for (int dz = -r - 3; dz <= r + 3; dz++) {
						double ddx = dx - cx;
						double ddz = dz - cz;
						double d = Math.sqrt(ddx * ddx + ddz * ddz);
						if (d > rad) {
							continue;
						}
						BlockPos p = base.add(dx, y, dz);
						if (y < 0 && !world.getBlockState(p).isAir()) {
							continue;
						}
						BlockState state;
						if (y >= height - 2) {
							state = ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
						} else if (d > rad - 1.0) {
							state = (dx + y + dz) % 5 == 0 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState() : ModBlocks.CLEAR_AMETHYST.getDefaultState();
						} else {
							state = ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
						}
						world.setBlockState(p, state, Block.NOTIFY_LISTENERS);
					}
				}
			}
		}
	}

	/** Crystal buds and clusters on every free face of the ground around. */
	public static class CrystalScatter extends Feature<DefaultFeatureConfig> {
		public CrystalScatter(Codec<DefaultFeatureConfig> codec) {
			super(codec);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
			StructureWorldAccess world = context.getWorld();
			Random random = context.getRandom();
			BlockPos origin = context.getOrigin();
			boolean any = false;
			for (int i = 0; i < 40; i++) {
				BlockPos p = origin.add(random.nextBetween(-7, 7), random.nextBetween(-4, 4), random.nextBetween(-7, 7));
				BlockState state = world.getBlockState(p);
				if (state.isAir() || !state.isOpaqueFullCube(world, p)) {
					continue;
				}
				Direction face = random.nextInt(3) == 0 ? Direction.random(random) : Direction.UP;
				any |= InfectionSpread.tryPlaceCrystal(world, p, face, random, random.nextInt(10) < 5 ? 0 : random.nextInt(10) < 7 ? 1 : 2);
			}
			return any;
		}
	}

	/** A small chunk of crystal rock hanging in the air, its underside bristling with crystals. */
	public static class FloatingShard extends Feature<DefaultFeatureConfig> {
		public FloatingShard(Codec<DefaultFeatureConfig> codec) {
			super(codec);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
			StructureWorldAccess world = context.getWorld();
			Random random = context.getRandom();
			BlockPos origin = context.getOrigin();
			int r = 2 + random.nextInt(3);
			for (BlockPos p : BlockPos.iterate(origin.add(-r - 1, -r * 2, -r - 1), origin.add(r + 1, 1, r + 1))) {
				if (!world.getBlockState(p).isAir()) {
					return false;
				}
			}
			for (int dy = 1; dy >= -r * 2; dy--) {
				double rad = dy > 0 ? r - 0.5 : r * (1.0 + dy / (double) (r * 2)) + 0.3;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz > rad * rad) {
							continue;
						}
						BlockPos p = origin.add(dx, dy, dz);
						BlockState state = dy == 1 ? ModBlocks.INFECTED_GRASS_BLOCK.getDefaultState()
								: random.nextInt(9) == 0 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState()
								: random.nextBoolean() ? ModBlocks.CRYSTALLIZED_STONE.getDefaultState() : ModBlocks.INFECTED_STONE.getDefaultState();
						if (replaceable(world.getBlockState(p))) {
							world.setBlockState(p, state, Block.NOTIFY_LISTENERS);
						}
					}
				}
			}
			for (int i = 0; i < 6; i++) {
				BlockPos under = origin.add(random.nextBetween(-r + 1, r - 1), -random.nextInt(r * 2), random.nextBetween(-r + 1, r - 1));
				InfectionSpread.tryPlaceCrystal(world, under, Direction.DOWN, random, 1 + random.nextInt(2));
				InfectionSpread.tryPlaceCrystal(world, origin.add(random.nextBetween(-r + 1, r - 1), 1, random.nextBetween(-r + 1, r - 1)),
						Direction.UP, random, random.nextInt(3));
			}
			return true;
		}
	}
}
