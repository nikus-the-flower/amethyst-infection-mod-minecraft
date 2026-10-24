package com.amethystmod.world;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.CrystalKin;
import com.amethystmod.entity.HeartEntity;
import com.amethystmod.entity.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.ChunkStatus;

/** Builds the Temple of the Amethyst at the centre of the Amethyst world once, a few layers per tick. */
public final class TempleBuilder {
	private static final int LAYERS_PER_TICK = 2;
	private static final int CHUNK_MIN = -3;
	private static final int CHUNK_MAX = 2;
	private static Job job;

	/** Per-world flag: is the temple built? */
	public static class State extends PersistentState {
		public boolean built;

		public static final PersistentState.Type<State> TYPE = new PersistentState.Type<>(State::new, State::load, null);

		private static State load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
			State s = new State();
			s.built = nbt.getBoolean("built");
			return s;
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
			nbt.putBoolean("built", built);
			return nbt;
		}

		public static State get(ServerWorld world) {
			return world.getPersistentStateManager().getOrCreate(TYPE, "amethystmod_temple");
		}
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerWorld world = server.getWorld(AmethystWorld.WORLD);
			if (world != null && !State.get(world).built) {
				start(world);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (job != null && job.tick()) {
				job = null;
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> commands(dispatcher));
	}

	public static boolean busy() {
		return job != null;
	}

	private static void commands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("amethyst").requires(s -> s.hasPermissionLevel(2))
				.then(CommandManager.literal("temple")
						.then(CommandManager.literal("build").executes(ctx -> {
							ServerWorld world = ctx.getSource().getServer().getWorld(AmethystWorld.WORLD);
							if (world == null || job != null) {
								return 0;
							}
							start(world);
							ctx.getSource().sendFeedback(() -> Text.translatable("commands.amethystmod.temple.building"), true);
							return 1;
						}))
						.then(CommandManager.literal("tp").executes(ctx -> {
							ServerWorld world = ctx.getSource().getServer().getWorld(AmethystWorld.WORLD);
							ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
							if (world == null) {
								return 0;
							}
							player.teleport(world, 0.5, TempleDesign.FLOOR_Y + 1, 40.5, 180.0f, 0.0f);
							return 1;
						}))));
	}

	public static void start(ServerWorld world) {
		if (job == null) {
			job = new Job(world);
		}
	}


	private static final class Job {
		private final ServerWorld world;
		private int y = TempleDesign.bottomY();

		Job(ServerWorld world) {
			this.world = world;
			for (int cx = CHUNK_MIN; cx <= CHUNK_MAX; cx++) {
				for (int cz = CHUNK_MIN; cz <= CHUNK_MAX; cz++) {
					world.setChunkForced(cx, cz, true);
					world.getChunk(cx, cz, ChunkStatus.FULL, true);
				}
			}
			State state = State.get(world);
			state.built = false;
			state.markDirty();
		}

		boolean tick() {
			BlockPos.Mutable pos = new BlockPos.Mutable();
			for (int n = 0; n < LAYERS_PER_TICK && y <= TempleDesign.topY(); n++, y++) {
				for (int dx = -47; dx <= 47; dx++) {
					for (int dz = -47; dz <= 47; dz++) {
						BlockState state = TempleDesign.stateAt(dx, y, dz);
						if (state != null && !world.getBlockState(pos.set(dx, y, dz)).equals(state)) {
							world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
						}
					}
				}
			}
			if (y <= TempleDesign.topY()) {
				return false;
			}
			finish();
			return true;
		}

		private void finish() {
			boolean defeated = StoryState.get(world.getServer()).heartDefeated;

			Box box = new Box(-50, TempleDesign.FLOOR_Y - 5, -50, 50, TempleDesign.topY(), 50);
			for (MobEntity old : world.getEntitiesByClass(MobEntity.class, box, e -> e instanceof CrystalKin)) {
				old.discard();
			}
			if (!defeated) {
				HeartEntity heart = ModEntities.HEART.create(world);
				if (heart != null) {
					BlockPos home = TempleDesign.heartHome();
					heart.refreshPositionAndAngles(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0f, 0.0f);
					heart.setHome(home);
					heart.initialize(world, world.getLocalDifficulty(home), SpawnReason.STRUCTURE, null);
					world.spawnEntity(heart);
				}
			}
			for (int cx = CHUNK_MIN; cx <= CHUNK_MAX; cx++) {
				for (int cz = CHUNK_MIN; cz <= CHUNK_MAX; cz++) {
					world.setChunkForced(cx, cz, false);
				}
			}
			State state = State.get(world);
			state.built = true;
			state.markDirty();
			AmethystMod.LOGGER.info("The Temple of the Amethyst stands at 0,0 (floor y={})", TempleDesign.FLOOR_Y);
		}
	}
}
