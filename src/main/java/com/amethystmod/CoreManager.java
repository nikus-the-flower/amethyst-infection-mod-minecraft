package com.amethystmod;

import com.amethystmod.entity.CrystalEffectEntity;
import com.amethystmod.entity.InfectionCoreEntity;
import com.amethystmod.entity.ModEntities;
import com.amethystmod.item.CrystalGeodeItem;
import com.amethystmod.item.Fx;
import com.amethystmod.world.CoreState;
import com.amethystmod.world.StoryState;
import com.amethystmod.world.WorldSites;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The cores of the infection: where they appear, how they age, the geodes they throw, and their crystal
 * entities near players. Everything here runs once a second (or every five seconds) on records, so it
 * costs almost nothing however many cores the world holds; the block work is the
 * {@link InfectionManager}'s.
 */
public final class CoreManager {
	public static final long DAY = 24000L;
	/** The world is split into cells this wide; each may get one starting core, decided by the seed. */
	public static final int CELL = 256;
	/** No two cores closer than this. */
	public static final double SPACING = 64;
	public static final int MAX_CORES = 3000;
	/** The tower's own land (radius 50) is infected from the start; no cores this close to it. */
	public static final double CENTER_FREE = 170;
	public static final double HOUSE_FREE = 110;
	/** Players within this distance see the crystal entity. */
	public static final double ENTITY_RANGE = 80;
	/** A core's land is painted while a player is within this distance of its edge. */
	public static final double ACTIVE_RANGE = 128;
	/** The largest land a core ever has (blocks of radius). */
	public static final double MAX_RADIUS = 220;

	/** The live crystal entity of each core (cores far from players have none). */
	private static final Map<Integer, InfectionCoreEntity> LIVE = new HashMap<>();
	/** Cores near players right now (refreshed every second): the ones whose land the engine paints. */
	public static final List<CoreState.Core> ACTIVE = new ArrayList<>();

	public static long statGeodes;
	public static long statSeeded;

	private CoreManager() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CoreManager::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LIVE.clear();
			ACTIVE.clear();
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> commands(dispatcher));
	}

	// ---- age, phase, land --------------------------------------------------------------------

	public static long phase2Ticks(ServerWorld world) {
		return world.getGameRules().getInt(ModGameRules.CORE_PHASE2_DAYS) * DAY;
	}

	public static long phase3Ticks(ServerWorld world) {
		return Math.max(phase2Ticks(world) + DAY, world.getGameRules().getInt(ModGameRules.CORE_PHASE3_DAYS) * DAY);
	}

	/** The gamerule amethystInfectionSpeed as a factor (1 = 100%): cores also age that much faster. */
	private static double speed(ServerWorld world) {
		return Math.max(1, world.getGameRules().getInt(ModGameRules.INFECTION_SPEED)) / 100.0;
	}

	/** The age of a core in ticks, counted at the speed of the infection. */
	public static double age(CoreState.Core core, ServerWorld world) {
		return (world.getTime() - core.born) * speed(world);
	}

	public static int phase(CoreState.Core core, ServerWorld world) {
		double age = age(core, world);
		return age >= phase3Ticks(world) ? 3 : age >= phase2Ticks(world) ? 2 : 1;
	}

	/** How far the land of a core reaches now: it keeps growing with its age, and ever wider in phase 3. */
	public static double radius(CoreState.Core core, ServerWorld world) {
		double age = age(core, world);
		double p2 = phase2Ticks(world);
		double p3 = phase3Ticks(world);
		if (age < p2) {
			return 8 + 28 * Math.max(0, age) / p2;
		}
		if (age < p3) {
			return 36 + 54 * (age - p2) / (p3 - p2);
		}
		return Math.min(MAX_RADIUS, 90 + 130 * (age - p3) / (2 * (p3 - p2)));
	}

	/** The age a core is given to start in a phase (in infection time). */
	private static long startAge(ServerWorld world, int phase) {
		return phase >= 3 ? phase3Ticks(world) : phase == 2 ? phase2Ticks(world) : 0;
	}

	public static CoreState.Core core(MinecraftServer server, int id) {
		return CoreState.get(server).cores.get(id);
	}

	/** Gives a core the age of the start of a phase (its land then grows to match). */
	public static void setPhase(ServerWorld world, CoreState.Core core, int phase) {
		core.born = world.getTime() - (long) (startAge(world, phase) / speed(world));
		core.nextGeode = 0;
		core.reach = null;
		CoreState.get(world.getServer()).markDirty();
	}

	// ---- the crystal entities ----------------------------------------------------------------

	/** Called by an entity every tick: true if it is (or now becomes) the one entity of its core. */
	public static boolean claim(InfectionCoreEntity entity) {
		InfectionCoreEntity other = LIVE.get(entity.getCoreId());
		if (other != null && other != entity && !other.isRemoved()) {
			return false;
		}
		LIVE.put(entity.getCoreId(), entity);
		return true;
	}

	public static void release(InfectionCoreEntity entity) {
		LIVE.remove(entity.getCoreId(), entity);
	}

	/** The core's crystal was destroyed by a player: its land stops growing (and stays infected). */
	public static void onKilled(ServerWorld world, int id) {
		CoreState state = CoreState.get(world.getServer());
		CoreState.Core core = state.cores.remove(id);
		if (core != null) {
			state.markDirty();
			ACTIVE.remove(core);
		}
	}

	public static void remove(MinecraftServer server, CoreState.Core core) {
		CoreState state = CoreState.get(server);
		if (state.cores.remove(core.id) != null) {
			state.markDirty();
		}
		ACTIVE.remove(core);
	}

	/** Removes a core and breaks its crystal if it stands somewhere loaded. */
	public static void destroy(MinecraftServer server, CoreState.Core core) {
		remove(server, core);
		InfectionCoreEntity entity = LIVE.get(core.id);
		if (entity != null && !entity.isRemoved()) {
			entity.shatter();
		}
	}

	/** A new core of a phase at a place (geodes and commands). Its crystal appears right away if the place is loaded. */
	public static CoreState.Core plant(ServerWorld world, BlockPos pos, int phase) {
		CoreState state = CoreState.get(world.getServer());
		CoreState.Core core = state.add(pos.getX(), pos.getZ(), world.getTime() - (long) (startAge(world, phase) / speed(world)));
		core.y = pos.getY();
		spawnEntity(world, core, pos.getY());
		return core;
	}

	private static InfectionCoreEntity spawnEntity(ServerWorld world, CoreState.Core core, int y) {
		InfectionCoreEntity entity = ModEntities.INFECTION_CORE.create(world);
		if (entity == null) {
			return null;
		}
		entity.refreshPositionAndAngles(core.x + 0.5, y, core.z + 0.5, world.random.nextFloat() * 360, 0);
		entity.bind(core.id, phase(core, world));
		world.spawnEntity(entity);
		LIVE.put(core.id, entity);
		return entity;
	}

	// ---- once a second -----------------------------------------------------------------------

	private static List<ServerPlayerEntity> players(ServerWorld world) {
		List<ServerPlayerEntity> list = new ArrayList<>(world.getPlayers(p -> !p.isSpectator()));
		for (ServerPlayerEntity dummy : SelfTest.DUMMIES) {
			if (dummy.getWorld() == world) {
				list.add(dummy);
			}
		}
		return list;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) {
			return;
		}
		ServerWorld world = server.getOverworld();
		CoreState state = CoreState.get(server);
		List<ServerPlayerEntity> players = players(world);
		long now = world.getTime();
		if (StoryState.infectionStopped) {
			fade(server, state, now);
			ACTIVE.clear();
		} else {
			refreshActive(world, state, players);
			if (server.getTicks() % 100 == 0 && !com.amethystmod.block.InfectedBlock.paused(world)) {
				seedCells(world, state, players);
				geodes(world, state, now);
			}
		}
		syncEntities(world, state, players);
	}

	/** After the Heart's fall each core gets its own last day, a few days from now, and fades away then. */
	private static void fade(MinecraftServer server, CoreState state, long now) {
		Random random = server.getOverworld().getRandom();
		for (CoreState.Core core : new ArrayList<>(state.cores.values())) {
			if (core.dieAt == 0) {
				core.dieAt = now + DAY + (long) (random.nextDouble() * 3 * DAY);
				state.markDirty();
			} else if (now >= core.dieAt && !LIVE.containsKey(core.id)) {
				remove(server, core);
			}
		}
	}

	private static void refreshActive(ServerWorld world, CoreState state, List<ServerPlayerEntity> players) {
		ACTIVE.clear();
		if (players.isEmpty()) {
			return;
		}
		for (CoreState.Core core : state.cores.values()) {
			double reach = radius(core, world) + ACTIVE_RANGE;
			for (ServerPlayerEntity p : players) {
				if (core.distanceSq(p.getX(), p.getZ()) <= reach * reach) {
					ACTIVE.add(core);
					break;
				}
			}
		}
	}

	/** Players near a core see its crystal: spawned on the ground once its chunk and entities are loaded. */
	private static void syncEntities(ServerWorld world, CoreState state, List<ServerPlayerEntity> players) {
		if (players.isEmpty()) {
			return;
		}
		for (CoreState.Core core : state.cores.values()) {
			boolean near = false;
			for (ServerPlayerEntity p : players) {
				if (core.distanceSq(p.getX(), p.getZ()) <= ENTITY_RANGE * ENTITY_RANGE) {
					near = true;
					break;
				}
			}
			InfectionCoreEntity live = LIVE.get(core.id);
			if (!near || (live != null && !live.isRemoved())) {
				continue;
			}
			BlockPos column = new BlockPos(core.x, world.getSeaLevel(), core.z);
			if (!world.isChunkLoaded(ChunkPos.toLong(column)) || !world.shouldTickEntity(column)) {
				continue;
			}
			List<InfectionCoreEntity> found = world.getEntitiesByClass(InfectionCoreEntity.class,
					new Box(core.x - 3, world.getBottomY(), core.z - 3, core.x + 4, world.getTopY(), core.z + 4),
					e -> e.getCoreId() == core.id && !e.isRemoved());
			if (!found.isEmpty()) {
				LIVE.put(core.id, found.get(0));
				continue;
			}
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, core.x, core.z);
			core.y = y;
			state.markDirty();
			spawnEntity(world, core, y);
		}
	}

	// ---- cores from the seed -----------------------------------------------------------------

	/** Decides the starting core of every cell around players (once per cell, from the world seed). */
	private static void seedCells(ServerWorld world, CoreState state, List<ServerPlayerEntity> players) {
		for (ServerPlayerEntity p : players) {
			int pcx = Math.floorDiv(p.getBlockX(), CELL);
			int pcz = Math.floorDiv(p.getBlockZ(), CELL);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					long key = ChunkPos.toLong(pcx + dx, pcz + dz);
					if (state.cells.add(key)) {
						state.markDirty();
						seedCell(world, state, pcx + dx, pcz + dz);
					}
				}
			}
		}
	}

	private static void seedCell(ServerWorld world, CoreState state, int cx, int cz) {
		Random r = Random.create(world.getSeed() ^ (cx * 341873128712L) ^ (cz * 132897987541L) ^ 0x5EEDC0DEL);
		if (r.nextFloat() > 0.7f) {
			return;
		}
		int x = cx * CELL + 24 + r.nextInt(CELL - 48);
		int z = cz * CELL + 24 + r.nextInt(CELL - 48);
		if (!freeSpot(world, state, x, z) || !onLand(world, x, z)) {
			return;
		}
		// the further from the tower, the older (the deeper into the world, the worse)
		double dist = Math.sqrt((double) x * x + (double) z * z);
		// ages are in infection time: the seed never depends on the speed
		long p2 = phase2Ticks(world);
		long p3 = phase3Ticks(world);
		double t = r.nextDouble();
		float roll = r.nextFloat();
		long age;
		if (dist < 600) {
			age = (long) (t * p2 * 0.85);
		} else if (dist < 1200) {
			age = roll < 0.7f ? p2 + (long) (t * (p3 - p2) * 0.9) : (long) (t * p2 * 0.9);
		} else {
			age = roll < 0.5f ? p3 + (long) (t * p2) : roll < 0.85f ? p2 + (long) (t * (p3 - p2) * 0.9) : (long) (t * p2 * 0.9);
		}
		state.add(x, z, world.getTime() - (long) (age / speed(world)));
		statSeeded++;
	}

	/** Far enough from the tower, the starting house, the other places of the story and every other core. */
	public static boolean freeSpot(ServerWorld world, CoreState state, int x, int z) {
		if ((double) x * x + (double) z * z < CENTER_FREE * CENTER_FREE) {
			return false;
		}
		for (WorldSites.Kind kind : WorldSites.Kind.values()) {
			WorldSites.Site site = WorldSites.site(world.getServer(), kind);
			double free = kind == WorldSites.Kind.HOUSE ? HOUSE_FREE : 48;
			if (site != null && (double) (site.x - x) * (site.x - x) + (double) (site.z - z) * (site.z - z) < free * free) {
				return false;
			}
		}
		for (CoreState.Core other : state.cores.values()) {
			if (other.distanceSq(x, z) < SPACING * SPACING) {
				return false;
			}
		}
		return true;
	}

	private static boolean onLand(ServerWorld world, int x, int z) {
		var chunks = world.getChunkManager();
		RegistryEntry<Biome> biome = chunks.getChunkGenerator().getBiomeSource().getBiome(BiomeCoords.fromBlock(x),
				BiomeCoords.fromBlock(world.getSeaLevel()), BiomeCoords.fromBlock(z), chunks.getNoiseConfig().getMultiNoiseSampler());
		return !biome.isIn(BiomeTags.IS_OCEAN) && !biome.isIn(BiomeTags.IS_DEEP_OCEAN) && !biome.isIn(BiomeTags.IS_RIVER);
	}

	// ---- geodes ------------------------------------------------------------------------------

	private static long geodeDelay(ServerWorld world, int phase, Random r) {
		double days = phase >= 3 ? 0.8 + r.nextDouble() * 0.8 : 1.5 + r.nextDouble() * 1.5;
		int speed = Math.max(1, world.getGameRules().getInt(ModGameRules.INFECTION_SPEED));
		return (long) (days * DAY * 100 / speed);
	}

	/** Cores in phase 2 and 3 throw a crystal geode now and then; it lands somewhere free and becomes a new core. */
	private static void geodes(ServerWorld world, CoreState state, long now) {
		Random r = world.getRandom();
		for (CoreState.Core core : new ArrayList<>(state.cores.values())) {
			if (core.dieAt != 0) {
				continue;
			}
			int phase = phase(core, world);
			if (phase < 2) {
				continue;
			}
			if (core.nextGeode == 0) {
				core.nextGeode = now + geodeDelay(world, phase, r);
				state.markDirty();
				continue;
			}
			if (now < core.nextGeode) {
				continue;
			}
			core.nextGeode = now + geodeDelay(world, phase, r);
			state.markDirty();
			throwGeode(world, core);
		}
	}

	/** Throws one geode from a core; returns the new core, or null if there was no free place (or too many cores). */
	public static CoreState.Core throwGeode(ServerWorld world, CoreState.Core parent) {
		CoreState state = CoreState.get(world.getServer());
		if (state.cores.size() >= MAX_CORES) {
			return null;
		}
		Random r = world.getRandom();
		for (int attempt = 0; attempt < 8; attempt++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = 56 + r.nextInt(150);
			int x = MathHelper.floor(parent.x + Math.cos(a) * d);
			int z = MathHelper.floor(parent.z + Math.sin(a) * d);
			if (!freeSpot(world, state, x, z) || !onLand(world, x, z)) {
				continue;
			}
			CoreState.Core child = state.add(x, z, world.getTime());
			statGeodes++;
			showThrow(world, parent, child);
			return child;
		}
		return null;
	}

	/** The geode flies up from its core with a chime, and comes down as a falling crystal where it lands. */
	private static void showThrow(ServerWorld world, CoreState.Core parent, CoreState.Core child) {
		InfectionCoreEntity from = LIVE.get(parent.id);
		if (from != null && !from.isRemoved()) {
			Vec3d top = from.getPos().add(0, from.getHeight(), 0);
			for (int i = 0; i < 12; i++) {
				world.spawnParticles(ModParticles.PRISM_FLARE, top.x, top.y + i * 0.8, top.z, 2, 0.15, 0.2, 0.15, 0.01);
			}
			world.playSound(null, top.x, top.y, top.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.HOSTILE, 3.0f, 1.5f);
		}
		if (world.isChunkLoaded(ChunkPos.toLong(new BlockPos(child.x, 0, child.z)))) {
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, child.x, child.z);
			Vec3d at = new Vec3d(child.x + 0.5, y, child.z + 0.5);
			CrystalEffectEntity.spawn(world, ModEntities.FALLING_CRYSTAL, at, 1.2f, 40);
			world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.HOSTILE, 3.0f, 0.7f);
		}
	}

	// ---- commands ----------------------------------------------------------------------------

	private static CoreState.Core nearest(ServerWorld world, Vec3d pos, double max) {
		CoreState.Core best = null;
		double bestD = max * max;
		for (CoreState.Core core : CoreState.get(world.getServer()).cores.values()) {
			double d = core.distanceSq(pos.x, pos.z);
			if (d < bestD) {
				bestD = d;
				best = core;
			}
		}
		return best;
	}

	private static List<CoreState.Core> within(ServerWorld world, Vec3d pos, double radius) {
		List<CoreState.Core> list = new ArrayList<>();
		for (CoreState.Core core : CoreState.get(world.getServer()).cores.values()) {
			if (core.distanceSq(pos.x, pos.z) <= radius * radius) {
				list.add(core);
			}
		}
		return list;
	}

	private static ServerWorld overworld(ServerCommandSource source) {
		if (source.getWorld().getRegistryKey() != World.OVERWORLD) {
			source.sendError(Text.translatable("commands.amethystmod.core.overworld"));
			return null;
		}
		return source.getWorld();
	}

	private static int upgrade(ServerCommandSource source, List<CoreState.Core> cores) {
		ServerWorld world = overworld(source);
		if (world == null) {
			return 0;
		}
		int n = 0;
		for (CoreState.Core core : cores) {
			int phase = phase(core, world);
			if (phase < 3) {
				setPhase(world, core, phase + 1);
				n++;
			}
		}
		int count = n;
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.upgraded", count), true);
		return n;
	}

	private static int kill(ServerCommandSource source, List<CoreState.Core> cores) {
		for (CoreState.Core core : cores) {
			destroy(source.getServer(), core);
		}
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.killed", cores.size()), true);
		return cores.size();
	}

	private static List<CoreState.Core> nearestList(ServerCommandSource source) {
		ServerWorld world = overworld(source);
		if (world == null) {
			return List.of();
		}
		CoreState.Core core = nearest(world, source.getPosition(), 160);
		if (core == null) {
			source.sendError(Text.translatable("commands.amethystmod.core.none"));
			return List.of();
		}
		return List.of(core);
	}

	private static int spawn(ServerCommandSource source, BlockPos pos, int phase) {
		ServerWorld world = overworld(source);
		if (world == null) {
			return 0;
		}
		if (StoryState.infectionStopped) {
			source.sendError(Text.translatable("commands.amethystmod.core.stopped"));
			return 0;
		}
		CoreState.Core core = plant(world, pos, phase);
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.spawned", core.id, phase, core.x, core.z), true);
		return 1;
	}

	private static int list(ServerCommandSource source, double radius) {
		ServerWorld world = overworld(source);
		if (world == null) {
			return 0;
		}
		Vec3d pos = source.getPosition();
		List<CoreState.Core> cores = within(world, pos, radius);
		cores.sort((a, b) -> Double.compare(a.distanceSq(pos.x, pos.z), b.distanceSq(pos.x, pos.z)));
		int total = CoreState.get(source.getServer()).cores.size();
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.list", total, cores.size(), (int) radius), false);
		for (CoreState.Core core : cores.subList(0, Math.min(20, cores.size()))) {
			int dist = (int) Math.sqrt(core.distanceSq(pos.x, pos.z));
			String days = String.format("%.1f", age(core, world) / (double) DAY);
			source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.line", core.id, phase(core, world),
					core.x, core.z, dist, days, (int) radius(core, world)), false);
		}
		return cores.size();
	}

	private static int geode(ServerCommandSource source, int phase, int count) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		player.giveItemStack(CrystalGeodeItem.stack(phase, count));
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.geode", count, phase), false);
		return count;
	}

	private static int throwNow(ServerCommandSource source) {
		List<CoreState.Core> list = nearestList(source);
		if (list.isEmpty()) {
			return 0;
		}
		CoreState.Core child = throwGeode(source.getWorld(), list.get(0));
		if (child == null) {
			source.sendError(Text.translatable("commands.amethystmod.core.nothrow"));
			return 0;
		}
		source.sendFeedback(() -> Text.translatable("commands.amethystmod.core.thrown", list.get(0).id, child.id, child.x, child.z), true);
		return 1;
	}

	private static int stats(ServerCommandSource source) {
		int total = CoreState.get(source.getServer()).cores.size();
		ServerWorld world = source.getServer().getOverworld();
		int[] mobs = new int[3];
		for (net.minecraft.entity.Entity e : world.iterateEntities()) {
			mobs[0]++;
			if (e instanceof com.amethystmod.entity.AmethystMob && !(e instanceof InfectionCoreEntity)) {
				mobs[1]++;
			}
			if (e instanceof net.minecraft.entity.mob.HostileEntity) {
				mobs[2]++;
			}
		}
		source.sendFeedback(() -> Text.literal("cores " + total + ", active " + ACTIVE.size() + ", live entities " + LIVE.size()
				+ ", seeded " + statSeeded + ", geodes " + statGeodes + "; mobs of the Amethyst " + mobs[1] + ", all hostile " + mobs[2]
				+ ", all entities " + mobs[0] + "; server tick " + String.format("%.1f", source.getServer().getAverageTickTime()) + " ms; "
				+ InfectionManager.stats()), false);
		return total;
	}

	private static int phaseArg(CommandContext<ServerCommandSource> ctx) {
		return IntegerArgumentType.getInteger(ctx, "phase");
	}

	private static void commands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("amethyst").requires(s -> s.hasPermissionLevel(2))
				.then(CommandManager.literal("core")
						.then(CommandManager.literal("list")
								.executes(ctx -> list(ctx.getSource(), 160))
								.then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 100000))
										.executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
						.then(CommandManager.literal("spawn")
								.then(CommandManager.argument("phase", IntegerArgumentType.integer(1, 3))
										.executes(ctx -> spawn(ctx.getSource(), BlockPos.ofFloored(ctx.getSource().getPosition()), phaseArg(ctx)))
										.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
												.executes(ctx -> spawn(ctx.getSource(), BlockPosArgumentType.getBlockPos(ctx, "pos"), phaseArg(ctx))))))
						.then(CommandManager.literal("upgrade")
								.executes(ctx -> upgrade(ctx.getSource(), nearestList(ctx.getSource())))
								.then(CommandManager.literal("all")
										.executes(ctx -> upgrade(ctx.getSource(), new ArrayList<>(CoreState.get(ctx.getSource().getServer()).cores.values()))))
								.then(CommandManager.literal("radius").then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 100000))
										.executes(ctx -> upgrade(ctx.getSource(), within(ctx.getSource().getWorld(), ctx.getSource().getPosition(),
												IntegerArgumentType.getInteger(ctx, "radius")))))))
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("phase", IntegerArgumentType.integer(1, 3))
										.executes(ctx -> {
											List<CoreState.Core> list = nearestList(ctx.getSource());
											if (list.isEmpty()) {
												return 0;
											}
											CoreState.Core core = list.get(0);
											setPhase(ctx.getSource().getWorld(), core, phaseArg(ctx));
											ctx.getSource().sendFeedback(() -> Text.translatable("commands.amethystmod.core.phase", core.id, phaseArg(ctx)), true);
											return 1;
										})))
						.then(CommandManager.literal("kill")
								.executes(ctx -> kill(ctx.getSource(), nearestList(ctx.getSource())))
								.then(CommandManager.literal("all")
										.executes(ctx -> kill(ctx.getSource(), new ArrayList<>(CoreState.get(ctx.getSource().getServer()).cores.values()))))
								.then(CommandManager.literal("radius").then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 100000))
										.executes(ctx -> kill(ctx.getSource(), within(ctx.getSource().getWorld(), ctx.getSource().getPosition(),
												IntegerArgumentType.getInteger(ctx, "radius")))))))
						.then(CommandManager.literal("geode")
								.then(CommandManager.argument("phase", IntegerArgumentType.integer(1, 3))
										.executes(ctx -> geode(ctx.getSource(), phaseArg(ctx), 1))
										.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(ctx -> geode(ctx.getSource(), phaseArg(ctx), IntegerArgumentType.getInteger(ctx, "count"))))))
						.then(CommandManager.literal("throw").executes(ctx -> throwNow(ctx.getSource())))
						.then(CommandManager.literal("stats").executes(ctx -> stats(ctx.getSource())))));
	}
}
