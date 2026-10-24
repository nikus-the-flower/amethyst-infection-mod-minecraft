package com.amethystmod.world;

import com.amethystmod.AmethystMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The places of the story in the normal world: the starting house and the three ruins. They are chosen
 * once per world from its seed (on land, 250 to 1200 blocks from the centre, far apart), the house is
 * built when the world starts (and players appear in it in a new world), and each ruin is built the
 * first time a player comes near.
 */
public final class WorldSites {
	public enum Kind {
		HOUSE(250, 420, 5), RUINS(380, 720, 7), CRATER(600, 950, 6), CASTLE(900, 1200, 7);

		final int min;
		final int max;
		final int flatness;

		Kind(int min, int max, int flatness) {
			this.min = min;
			this.max = max;
			this.flatness = flatness;
		}

		public String id() {
			return name().toLowerCase();
		}

		Blueprint design(long seed) {
			return switch (this) {
				case HOUSE -> SiteDesigns.house(seed);
				case RUINS -> SiteDesigns.surfaceRuins(seed);
				case CRATER -> SiteDesigns.crater(seed);
				case CASTLE -> SiteDesigns.castle(seed);
			};
		}
	}

	/** One chosen place. */
	public static final class Site {
		public final Kind kind;
		public int x;
		public int z;
		public int y;
		public boolean built;

		Site(Kind kind, int x, int z) {
			this.kind = kind;
			this.x = x;
			this.z = z;
		}

		public BlockPos origin() {
			return new BlockPos(x, y, z);
		}
	}

	public static final class State extends PersistentState {
		public final Map<Kind, Site> sites = new LinkedHashMap<>();

		public static final PersistentState.Type<State> TYPE = new PersistentState.Type<>(State::new, State::load, null);

		private static State load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
			State s = new State();
			for (Kind k : Kind.values()) {
				if (nbt.contains(k.id())) {
					NbtCompound c = nbt.getCompound(k.id());
					Site site = new Site(k, c.getInt("x"), c.getInt("z"));
					site.y = c.getInt("y");
					site.built = c.getBoolean("built");
					s.sites.put(k, site);
				}
			}
			return s;
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
			for (Site site : sites.values()) {
				NbtCompound c = new NbtCompound();
				c.putInt("x", site.x);
				c.putInt("z", site.z);
				c.putInt("y", site.y);
				c.putBoolean("built", site.built);
				nbt.put(site.kind.id(), c);
			}
			return nbt;
		}

		public static State get(ServerWorld overworld) {
			return overworld.getPersistentStateManager().getOrCreate(TYPE, "amethystmod_sites");
		}
	}

	private static SiteBuilder job;

	private WorldSites() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(WorldSites::start);
		ServerTickEvents.END_SERVER_TICK.register(WorldSites::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> job = null);
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> commands(dispatcher));
	}

	public static Site site(MinecraftServer server, Kind kind) {
		return State.get(server.getOverworld()).sites.get(kind);
	}

	public static boolean busy() {
		return job != null;
	}

	private static void start(MinecraftServer server) {
		ServerWorld world = server.getOverworld();
		State state = State.get(world);
		boolean fresh = world.getTime() < 200 && !state.sites.containsKey(Kind.HOUSE);
		if (state.sites.size() < Kind.values().length) {
			choose(world, state);
		}
		Site house = state.sites.get(Kind.HOUSE);
		if (house != null && !house.built) {
			// the house is small: built right away, so that players can appear in it
			SiteBuilder b = new SiteBuilder(world, house, house.kind.design(seed(world, house.kind)));
			while (!b.tick()) {
				// keep building
			}
			if (fresh) {
				world.setSpawnPos(house.origin().add(-3, 0, 1), 180.0f);
				world.getGameRules().get(GameRules.SPAWN_RADIUS).set(0, server);
			}
		}
	}

	private static long seed(ServerWorld world, Kind kind) {
		return world.getSeed() * 31 + kind.ordinal() * 7919L + 0x5EEDL;
	}

	/** Picks every missing place: random angles and distances, on flat land, far from each other. */
	private static void choose(ServerWorld world, State state) {
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		NoiseConfig noise = world.getChunkManager().getNoiseConfig();
		Random random = Random.create(world.getSeed() ^ 0x5EED_A11EL);
		for (Kind kind : Kind.values()) {
			int bestX = 0;
			int bestZ = kind.min;
			for (int attempt = 0; attempt < 120; attempt++) {
				double a = random.nextDouble() * Math.PI * 2;
				double r = kind.min + random.nextDouble() * (kind.max - kind.min);
				int x = (int) Math.round(Math.cos(a) * r);
				int z = (int) Math.round(Math.sin(a) * r);
				if (attempt == 0) {
					bestX = x;
					bestZ = z;
				}
				boolean far = true;
				for (Site other : state.sites.values()) {
					long dx = other.x - x;
					long dz = other.z - z;
					if (other.kind != kind && dx * dx + dz * dz < 220L * 220L) {
						far = false;
					}
				}
				if (far && goodLand(world, generator, noise, x, z, kind.flatness)) {
					bestX = x;
					bestZ = z;
					break;
				}
			}
			if (!state.sites.containsKey(kind)) {
				state.sites.put(kind, new Site(kind, bestX, bestZ));
			}
		}
		state.markDirty();
		for (Site s : state.sites.values()) {
			AmethystMod.LOGGER.info("Amethyst site {} at {}, {}", s.kind.id(), s.x, s.z);
		}
	}

	private static boolean goodLand(ServerWorld world, ChunkGenerator generator, NoiseConfig noise, int x, int z, int flatness) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int[] o : new int[][]{{0, 0}, {14, 0}, {-14, 0}, {0, 14}, {0, -14}, {10, 10}, {-10, -10}, {10, -10}, {-10, 10}}) {
			int h = generator.getHeight(x + o[0], z + o[1], Heightmap.Type.WORLD_SURFACE_WG, world, noise);
			min = Math.min(min, h);
			max = Math.max(max, h);
		}
		if (min <= world.getSeaLevel() + 1 || max > 150 || max - min > flatness) {
			return false;
		}
		RegistryEntry<Biome> biome = generator.getBiomeSource().getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(max),
				BiomeCoords.fromBlock(z), noise.getMultiNoiseSampler());
		return !biome.isIn(BiomeTags.IS_OCEAN) && !biome.isIn(BiomeTags.IS_RIVER) && !biome.isIn(BiomeTags.IS_BEACH)
				&& !biome.isIn(BiomeTags.IS_DEEP_OCEAN);
	}

	/** Ruins are built when somebody first comes within about two hundred blocks. */
	private static void tick(MinecraftServer server) {
		if (job != null) {
			if (job.tick()) {
				job = null;
			}
			return;
		}
		if (server.getTicks() % 40 != 0) {
			return;
		}
		ServerWorld world = server.getOverworld();
		State state = State.get(world);
		for (Site site : state.sites.values()) {
			if (site.built) {
				continue;
			}
			for (ServerPlayerEntity p : world.getPlayers()) {
				double dx = p.getX() - site.x;
				double dz = p.getZ() - site.z;
				if (dx * dx + dz * dz < 200 * 200) {
					build(world, site);
					return;
				}
			}
		}
	}

	public static void build(ServerWorld world, Site site) {
		if (job == null) {
			job = new SiteBuilder(world, site, site.kind.design(seed(world, site.kind)));
		}
	}

	/** Runs a site's build to the end right now (the self-test and the command use it). */
	public static void buildNow(ServerWorld world, Site site) {
		SiteBuilder b = new SiteBuilder(world, site, site.kind.design(seed(world, site.kind)));
		while (!b.tick()) {
			// keep building
		}
	}

	private static void commands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("amethyst").requires(s -> s.hasPermissionLevel(2))
				.then(CommandManager.literal("sites")
						.executes(ctx -> {
							ServerCommandSource source = ctx.getSource();
							for (Site s : State.get(source.getServer().getOverworld()).sites.values()) {
								source.sendFeedback(() -> Text.literal(s.kind.id() + ": " + s.x + " " + s.z + (s.built ? " (built)" : "")), false);
							}
							return 1;
						})
						.then(CommandManager.literal("tp").then(CommandManager.argument("site", StringArgumentType.word())
								.executes(ctx -> {
									ServerCommandSource source = ctx.getSource();
									Kind kind = Kind.valueOf(StringArgumentType.getString(ctx, "site").toUpperCase());
									Site s = site(source.getServer(), kind);
									ServerWorld world = source.getServer().getOverworld();
									world.getChunk(s.x >> 4, s.z >> 4);
									int y = s.built ? s.y : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, s.x, s.z);
									source.getPlayerOrThrow().teleport(world, s.x + 0.5, y + 12, s.z + 0.5, 0, 30);
									return 1;
								})))
						.then(CommandManager.literal("rebuild").then(CommandManager.argument("site", StringArgumentType.word())
								.executes(ctx -> {
									ServerCommandSource source = ctx.getSource();
									Kind kind = Kind.valueOf(StringArgumentType.getString(ctx, "site").toUpperCase());
									Site s = site(source.getServer(), kind);
									s.built = false;
									build(source.getServer().getOverworld(), s);
									source.sendFeedback(() -> Text.literal("building " + kind.id()), true);
									return 1;
								})))));
	}
}
