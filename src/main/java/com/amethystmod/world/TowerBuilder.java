package com.amethystmod.world;

import com.amethystmod.AmethystMod;
import com.amethystmod.block.ModBlocks;
import com.amethystmod.entity.CrystalKin;
import com.amethystmod.entity.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Amethyst Tower at world 0,0 a few layers per tick (so the server does not stall), fills it
 * with creatures, chests and the apostle Amitariel, and remembers that it is done.
 * It builds itself once when a world starts; {@code /amethyst tower build} rebuilds it. A tower built by
 * an older version of the mod is rebuilt in the new shape (the old one is cleared away first).
 */
public final class TowerBuilder {
	/** Chests get better the higher you climb. */
	private static RegistryKey<LootTable> chestLoot(int floor) {
		String tier = floor <= 1 ? "tower_low" : floor <= 4 ? "tower_mid" : "tower_high";
		return RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(AmethystMod.MOD_ID, "chests/" + tier));
	}
	private static final int LAYERS_PER_TICK = 2;
	private static final int COLUMNS_PER_TICK = 250;
	/** Chunk range loaded while building (chunk coordinates). */
	private static final int CHUNK_MIN = -4;
	private static final int CHUNK_MAX = 3;
	/** The old round tower (versions 1-2): seven floors, radius 15, up to this height above its base. */
	private static final int OLD_HEIGHT = 157;

	private static Job job;

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerWorld overworld = server.getOverworld();
			TowerState state = TowerState.get(overworld);
			if (!state.built) {
				start(overworld, false);
			} else if (state.version < TowerState.CURRENT_VERSION) {
				// the old round tower: if its portal was already opened, Amitariel has fallen
				BlockPos oldKeystone = new BlockPos(0, state.baseY + 7 * 14 + 1, 8);
				overworld.getChunk(oldKeystone);
				BlockState key = overworld.getBlockState(oldKeystone);
				if (key.isOf(ModBlocks.PORTAL_KEYSTONE) && key.get(com.amethystmod.block.PortalKeystoneBlock.LIT)) {
					StoryState.markAmitarielDefeated(server);
				}
				start(overworld, true);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (job != null && job.tick()) {
				job = null;
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> commands(dispatcher));
	}

	private static void commands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("amethyst").requires(s -> s.hasPermissionLevel(2))
				.then(CommandManager.literal("tower")
						.then(CommandManager.literal("build").executes(ctx -> {
							ServerCommandSource source = ctx.getSource();
							if (job != null) {
								source.sendFeedback(() -> Text.translatable("commands.amethystmod.tower.busy"), false);
								return 0;
							}
							start(source.getServer().getOverworld(), true);
							source.sendFeedback(() -> Text.translatable("commands.amethystmod.tower.building"), true);
							return 1;
						}))
						.then(CommandManager.literal("tp").executes(ctx -> {
							ServerCommandSource source = ctx.getSource();
							ServerWorld overworld = source.getServer().getOverworld();
							TowerState state = TowerState.get(overworld);
							if (!state.built) {
								source.sendFeedback(() -> Text.translatable("commands.amethystmod.tower.missing"), false);
								return 0;
							}
							ServerPlayerEntity player = source.getPlayerOrThrow();
							player.teleport(overworld, 0.5, state.baseY + 1, 18.5, 180.0f, 0.0f);
							return 1;
						}))));
	}

	public static boolean busy() {
		return job != null;
	}

	public static void start(ServerWorld world, boolean force) {
		if (job == null) {
			job = new Job(world, force);
		}
	}

	private static final class Job {
		private final ServerWorld world;
		private final boolean force;
		private final TowerDesign design;
		private final List<int[]> columns = new ArrayList<>();
		private final List<Object[]> entrance = new ArrayList<>();
		private int stage;
		private int index;
		private int y;
		private final boolean firstBuild;
		/** Rebuilding over an older tower: whatever of it the new shape does not cover is cleared. */
		private final boolean clearOld;
		private final int lastY;

		Job(ServerWorld world, boolean force) {
			this.world = world;
			this.force = force;
			TowerState old = TowerState.get(world);
			this.firstBuild = !old.built;
			this.clearOld = old.built && old.version < 3;
			for (int cx = CHUNK_MIN; cx <= CHUNK_MAX; cx++) {
				for (int cz = CHUNK_MIN; cz <= CHUNK_MAX; cz++) {
					world.setChunkForced(cx, cz, true);
					world.getChunk(cx, cz, ChunkStatus.FULL, true);
				}
			}

			int top = 0;
			for (int i = -1; i < 16; i++) {
				double radius = i < 0 ? 0 : (i < 8 ? 6 : 13);
				double a = Math.PI * 2 * i / 8;
				int x = (int) Math.round(Math.cos(a) * radius);
				int z = (int) Math.round(Math.sin(a) * radius);
				top = Math.max(top, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z));
			}
			int baseY = firstBuild ? Math.min(Math.max(world.getSeaLevel() + 1, top), 140) : old.baseY;
			this.design = new TowerDesign(baseY);
			this.y = design.bottomY();
			this.lastY = clearOld ? Math.max(design.topY(), baseY + OLD_HEIGHT) : design.topY();

			TowerState state = TowerState.get(world);
			state.baseY = baseY;
			state.built = false;
			state.markDirty();

			for (int dx = -16; dx <= 16; dx++) {
				for (int dz = -16; dz <= 16; dz++) {
					if (dx * dx + dz * dz <= 250) {
						columns.add(new int[]{dx, dz});
					}
				}
			}
			planEntrance();
		}

		/** A short stair of blocks leads from the door down to the ground. */
		private void planEntrance() {
			int baseY = design.baseY;
			for (int dz = 15; dz <= 31; dz++) {
				int stepY = baseY - Math.max(0, dz - 15);
				boolean reachedGround = false;
				for (int dx = -3; dx <= 3; dx++) {
					int ground = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, dx, dz) - 1;
					if (stepY <= ground + 1) {
						reachedGround = true;
					}
					for (int yy = Math.min(ground + 1, stepY); yy <= stepY; yy++) {
						entrance.add(new Object[]{new BlockPos(dx, yy, dz), ModBlocks.AMETHYST_BRICKS.getDefaultState()});
					}
					for (int yy = stepY + 1; yy <= stepY + 6; yy++) {
						entrance.add(new Object[]{new BlockPos(dx, yy, dz), Blocks.AIR.getDefaultState()});
					}
				}
				if (reachedGround && dz > 15) {
					break;
				}
			}
		}

		private void place(BlockPos pos, BlockState state) {
			if (state != null && !world.getBlockState(pos).equals(state)) {
				world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
			}
		}

		/** @return true when the job is finished. */
		boolean tick() {
			evacuate();
			switch (stage) {
				case 0 -> {
					for (int n = 0; n < COLUMNS_PER_TICK && index < columns.size(); n++, index++) {
						int[] c = columns.get(index);
						int ground = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, c[0], c[1]) - 1;
						for (int yy = ground; yy < design.baseY; yy++) {
							place(new BlockPos(c[0], yy, c[1]),
									yy == design.baseY - 1 ? ModBlocks.AMETHYST_BRICKS.getDefaultState() : ModBlocks.INFECTED_STONE.getDefaultState());
						}
					}
					if (index >= columns.size()) {
						stage = 1;
						index = 0;
					}
				}
				case 1 -> {
					int reach = TowerDesign.REACH;
					for (int n = 0; n < LAYERS_PER_TICK && y <= lastY; n++, y++) {
						BlockPos.Mutable pos = new BlockPos.Mutable();
						boolean oldLayer = clearOld && y >= design.baseY && y <= design.baseY + OLD_HEIGHT;
						for (int dx = -reach; dx <= reach; dx++) {
							for (int dz = -reach; dz <= reach; dz++) {
								BlockState state = design.stateAt(dx, y, dz);
								if (state == null && oldLayer && dx * dx + dz * dz <= 230) {
									state = Blocks.AIR.getDefaultState();
								}
								if (state != null) {
									place(pos.set(dx, y, dz), state);
								}
							}
						}
					}
					if (y > lastY) {
						stage = 2;
					}
				}
				case 2 -> {
					for (Object[] e : entrance) {
						place((BlockPos) e[0], (BlockState) e[1]);
					}
					stage = 3;
				}
				default -> {
					populate();
					finish();
					return true;
				}
			}
			return false;
		}

		/** Players standing where the tower is rising would be buried: move them to the front door. */
		private void evacuate() {
			int r = TowerDesign.REACH + 2;
			for (ServerPlayerEntity p : world.getPlayers()) {
				if (p.getX() * p.getX() + p.getZ() * p.getZ() < r * r && p.getY() > design.bottomY() - 4) {
					p.teleport(world, 0.5, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, 0, r + 2) + 0.1, r + 2.5, 180.0f, 0.0f);
				}
			}
		}

		private void populate() {
			Box box = new Box(-18, design.baseY - 2, -18, 18, design.topY() + 2, 18);
			for (MobEntity old : world.getEntitiesByClass(MobEntity.class, box, e -> e instanceof CrystalKin)) {
				old.discard();
			}
			Random random = world.getRandom();

			for (int floor = 0; floor < TowerDesign.BOSS_FLOOR; floor++) {
				int floorY = design.floorY(floor) + 1;
				placeChest(floor, floorY, random);
				// one guard per floor, nothing that flies
				switch (floor) {
					case 0 -> spawn(ModEntities.STALKER, 2, floorY, random, 0);
					case 1 -> spawn(ModEntities.LURKER, 1, floorY, random, 0);
					case 2 -> spawn(ModEntities.WALKER, 1, floorY, random, 0);
					case 3 -> spawn(ModEntities.WEAVER, 2, floorY, random, 0);
					default -> spawn(ModEntities.COLOSSUS, 1, floorY, random, 0);
				}
			}

			MobEntity boss = StoryState.get(world.getServer()).amitarielDefeated ? null : ModEntities.AMITARIEL.create(world);
			if (boss != null) {
				BlockPos pos = new BlockPos(0, design.floorY(TowerDesign.BOSS_FLOOR) + 3, 0);
				boss.refreshPositionAndAngles(0.5, pos.getY(), 0.5, 180.0f, 0.0f);
				boss.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
				boss.setPersistent();
				world.spawnEntityAndPassengers(boss);
			}
		}

		private void placeChest(int floor, int floorY, Random random) {
			double angle = floor * 0.9 + 0.3;
			int dx = (int) Math.round(Math.cos(angle) * 5.5);
			int dz = (int) Math.round(Math.sin(angle) * 5.5);
			Direction facing = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST)
					: (dz > 0 ? Direction.NORTH : Direction.SOUTH);
			BlockPos pos = new BlockPos(dx, floorY, dz);
			world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing), Block.NOTIFY_LISTENERS);
			BlockEntity entity = world.getBlockEntity(pos);
			if (entity instanceof ChestBlockEntity chest) {
				chest.setLootTable(chestLoot(floor), random.nextLong());
			}
		}

		private void spawn(EntityType<? extends MobEntity> type, int count, int floorY, Random random, int lift) {
			for (int i = 0; i < count; i++) {
				MobEntity mob = type.create(world);
				if (mob == null) {
					continue;
				}
				double a = random.nextDouble() * Math.PI * 2;
				double r = 2.5 + random.nextDouble() * 3.5;
				BlockPos pos = new BlockPos((int) Math.floor(Math.cos(a) * r), floorY + lift, (int) Math.floor(Math.sin(a) * r));
				mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
				mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
				mob.setPersistent();
				world.spawnEntityAndPassengers(mob);
			}
		}

		private void finish() {
			TowerState state = TowerState.get(world);
			state.built = true;
			state.version = TowerState.CURRENT_VERSION;
			state.markDirty();
			for (int cx = CHUNK_MIN; cx <= CHUNK_MAX; cx++) {
				for (int cz = CHUNK_MIN; cz <= CHUNK_MAX; cz++) {
					world.setChunkForced(cx, cz, false);
				}
			}
			AmethystMod.LOGGER.info("The Amethyst Tower stands at 0,0 (ground floor y={})", design.baseY);
		}
	}
}
