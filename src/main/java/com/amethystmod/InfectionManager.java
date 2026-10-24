package com.amethystmod;

import com.amethystmod.block.GrowingCrystalBlock;
import com.amethystmod.block.ModBlocks;
import com.amethystmod.world.CoreState;
import com.amethystmod.world.StoryState;
import com.amethystmod.world.WorldSites;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The engine that turns the land around the cores into the infection, with a hard budget per tick.
 *
 * <p>The land of a core is a blob around it whose size grows with the core's age ({@link CoreManager#radius}).
 * Two things paint it:
 * <ul>
 * <li>the <b>march</b>: every tick a few rays go out from the cores near players and infect the first
 * column that is not infected yet, so the infection visibly creeps outward;</li>
 * <li>the <b>catch-up</b>: a chunk that loads inside some core's land is painted in a few ticks, so old
 * cores far from players are already surrounded by their land when somebody arrives.</li>
 * </ul>
 * Columns are painted from the top down: trees, snow and plants, then a few blocks of soil. Young cores
 * (phase 1) only take soil, sand and trees; older ones take bare stone too, raise crystal pikes and turn
 * the biome into the crystal wastes. The tower's land (radius 50 around 0,0) is painted the same way from
 * the start. Crystals that sprout grow through a small queue, as before.
 */
public final class InfectionManager {
	private static final int MAX_GROWTH = 420;
	/** Crystal growth steps per tick at full speed. */
	private static final double GROWTHS_PER_TICK = 3.0;
	/**
	 * What the infection may spend per tick at speed 100 (light: about the old engine's cost), and for painting
	 * chunks that load inside a land. The gamerule scales both; the server's own load takes it back down
	 * (see {@link #loadFactor}), so the game never lags because of the infection.
	 */
	private static final long MARCH_BUDGET_NANOS = 1_000_000L;
	private static final long PAINT_BUDGET_NANOS = 500_000L;
	/** A ray never paints more than this many columns in one visit, so the front grows evenly all round. */
	private static final int RUN = 6;
	private static final int MAX_CORES_PER_TICK = 6;
	public static final int RAYS = 1024;
	public static final int CENTER_RADIUS = 50;
	public static final RegistryKey<Biome> CRYSTAL_WASTES = RegistryKey.of(RegistryKeys.BIOME,
			Identifier.of(AmethystMod.MOD_ID, "crystal_wastes"));
	/** Older cores turn their land into the crystal wastes biome (switch off if it ever costs too much). */
	public static boolean biomesEnabled = true;

	private static final float[] CENTER_SHAPE = shape(0xCE47E5L, 0.86f);

	private static final class Growth {
		final BlockPos pos;
		final Block block;
		final long dueAt;

		Growth(BlockPos pos, Block block, long dueAt) {
			this.pos = pos;
			this.block = block;
			this.dueAt = dueAt;
		}
	}

	/** A loaded chunk inside somebody's land, painted a few columns at a time. */
	private static final class PaintJob {
		final int cx;
		final int cz;
		int next;
		List<CoreState.Core> cores;
		double[] radii;
		int[] phases;

		PaintJob(int cx, int cz) {
			this.cx = cx;
			this.cz = cz;
		}
	}

	private static final class Queues {
		final ArrayDeque<Growth> growth = new ArrayDeque<>();
		final ArrayDeque<PaintJob> paint = new ArrayDeque<>();
		final Set<Long> queued = new HashSet<>();
		/** Chunks already handed to the painter (cleared now and then, painting a painted chunk costs next to nothing). */
		final Set<Long> done = new HashSet<>();
		final Map<Long, Integer> biomeDirty = new LinkedHashMap<>();
		double growthOwed;
		int next;
	}

	private static Queues overworld;
	private static int[][] sites = new int[0][];

	// numbers for the self-test and the stats command
	public static long statTicks;
	public static long statNanos;
	public static long statMaxNanos;
	public static long statSteps;
	public static long statColumns;
	public static long statBlocks;
	public static long statGrown;
	public static long statPainted;
	public static long statPikes;
	public static long statBiomeChunks;
	public static long statBiomeNanos;
	public static long statBiomeMaxNanos;

	private InfectionManager() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(InfectionManager::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> overworld = null);
		ServerChunkEvents.CHUNK_LOAD.register(InfectionManager::onChunkLoad);
	}

	public static String stats() {
		return "ticks " + statTicks + ", average " + (statTicks == 0 ? 0 : statNanos / statTicks / 1000) + " us per tick, worst "
				+ statMaxNanos / 1000 + " us, rays " + statSteps + ", columns " + statColumns + ", blocks " + statBlocks
				+ ", chunks painted " + statPainted + ", crystals grown " + statGrown + ", pikes " + statPikes
				+ ", biome chunks " + statBiomeChunks + " (worst " + statBiomeMaxNanos / 1000 + " us)";
	}

	public static void resetStats() {
		statTicks = statNanos = statMaxNanos = statSteps = statColumns = statBlocks = statGrown = statPainted = statPikes = 0;
		statBiomeChunks = statBiomeNanos = statBiomeMaxNanos = 0;
	}

	private static Queues queues() {
		if (overworld == null) {
			overworld = new Queues();
		}
		return overworld;
	}

	private static boolean active(ServerWorld world) {
		return world.getRegistryKey() == World.OVERWORLD && !com.amethystmod.block.InfectedBlock.paused(world);
	}

	// ---- shapes: no land is a perfect circle -----------------------------------------------------

	/** A smooth random outline: how far the land reaches along each ray, as a fraction of its radius. */
	public static float[] shape(long seed, float min) {
		Random r = Random.create(seed);
		float[] knots = new float[16];
		for (int i = 0; i < knots.length; i++) {
			knots[i] = min + r.nextFloat() * (1.0f - min);
		}
		float[] out = new float[RAYS];
		int per = RAYS / knots.length;
		for (int i = 0; i < RAYS; i++) {
			int k = i / per;
			float t = (i % per) / (float) per;
			float s = (1 - MathHelper.cos(t * MathHelper.PI)) / 2;
			out[i] = MathHelper.lerp(s, knots[k], knots[(k + 1) % knots.length]);
		}
		return out;
	}

	private static int rayOf(double dx, double dz) {
		double a = Math.atan2(dz, dx) + Math.PI;
		return Math.floorMod((int) Math.floor(a / (Math.PI * 2) * RAYS), RAYS);
	}

	private static void prepare(CoreState.Core core) {
		if (core.shape == null) {
			core.shape = shape(core.id * 0x9E3779B97F4A7C15L + 17, 0.68f);
		}
		if (core.reach == null) {
			core.reach = new int[RAYS];
		}
	}

	/** Is (x, z) inside the land of a core whose radius is {@code radius}? */
	private static boolean inLand(CoreState.Core core, double radius, int x, int z) {
		double dx = x + 0.5 - (core.x + 0.5);
		double dz = z + 0.5 - (core.z + 0.5);
		double r = radius * core.shape[rayOf(dx, dz)];
		return dx * dx + dz * dz <= r * r;
	}

	private static boolean inCenter(int x, int z) {
		double r = CENTER_RADIUS * CENTER_SHAPE[rayOf(x, z)];
		return (double) x * x + (double) z * z <= r * r;
	}

	/** The places of the story keep a small circle of clean land, so the infection does not eat them. */
	private static boolean protectedColumn(int x, int z) {
		for (int[] s : sites) {
			long dx = x - s[0];
			long dz = z - s[1];
			if (dx * dx + dz * dz < (long) s[2] * s[2]) {
				return true;
			}
		}
		return false;
	}

	private static void refreshSites(ServerWorld world) {
		List<int[]> list = new ArrayList<>();
		for (WorldSites.Kind kind : WorldSites.Kind.values()) {
			WorldSites.Site site = WorldSites.site(world.getServer(), kind);
			if (site != null) {
				list.add(new int[]{site.x, site.z, kind == WorldSites.Kind.HOUSE ? 34 : 26});
			}
		}
		sites = list.toArray(new int[0][]);
	}

	// ---- painting one column ---------------------------------------------------------------------

	private static boolean isCover(BlockState s) {
		return s.getFluidState().isEmpty() && (s.isOf(Blocks.SNOW) || s.isIn(BlockTags.REPLACEABLE_BY_TREES) || s.isReplaceable());
	}

	private static boolean isGround(BlockState s) {
		Block b = s.getBlock();
		return b != ModBlocks.INFECTED_LOG && b != ModBlocks.INFECTED_LEAVES && !ModBlocks.CRYSTALS.contains(b);
	}

	/** True if the column has nothing more to give a core of this phase (infected, water, or bare rock for a young core). */
	private static boolean isDone(ServerWorld world, int x, int z, int phase, BlockPos.Mutable m) {
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		BlockState s = world.getBlockState(m.set(x, top, z));
		if (!s.getFluidState().isEmpty() || InfectionSpread.isInfected(s)) {
			return true;
		}
		return InfectionSpread.convertSurface(s, phase) == null && !isCover(s);
	}

	/**
	 * Infects one column from the top: leaves and logs, snow and plants (cleared), then a few blocks of soil
	 * (deeper for older cores). Then maybe a crystal, a pike and a mark for the crystal wastes biome.
	 */
	private static void paintColumn(ServerWorld world, int x, int z, int phase, boolean wastes, double fromCore,
									Random random, BlockPos.Mutable m) {
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		int depth = phase >= 3 ? 4 : phase == 2 ? 3 : 2;
		int ground = 0;
		int groundTop = Integer.MIN_VALUE;
		int bottom = world.getBottomY();
		for (int y = top, n = 0; y > bottom && n < 48; y--, n++) {
			BlockState s = world.getBlockState(m.set(x, y, z));
			if (!s.getFluidState().isEmpty()) {
				break;
			}
			if (s.isAir()) {
				if (ground > 0) {
					break;
				}
				continue;
			}
			BlockState infected;
			if (InfectionSpread.isInfected(s)) {
				infected = s;
			} else {
				infected = InfectionSpread.convertSurface(s, phase);
				if (infected != null) {
					world.setBlockState(m, infected, Block.NOTIFY_LISTENERS);
					statBlocks++;
				}
			}
			if (infected != null) {
				if (isGround(infected)) {
					if (groundTop == Integer.MIN_VALUE) {
						groundTop = y;
					}
					if (++ground >= depth) {
						break;
					}
				}
				continue;
			}
			if (ground == 0 && isCover(s)) {
				world.setBlockState(m, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				continue;
			}
			break;
		}
		statColumns++;
		if (groundTop == Integer.MIN_VALUE) {
			return;
		}
		// grass and flowers on the ground are not in the heightmap: clear them by hand
		for (int k = 1; k <= 2; k++) {
			BlockState s = world.getBlockState(m.set(x, groundTop + k, z));
			if (!s.isAir() && isCover(s) && !s.isIn(BlockTags.LEAVES)) {
				world.setBlockState(m, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		BlockPos groundPos = new BlockPos(x, groundTop, z);
		int crystalOdds = phase >= 3 ? 6 : phase == 2 ? 9 : 14;
		if (random.nextInt(crystalOdds) == 0) {
			InfectionSpread.tryPlaceCrystal(world, groundPos, Direction.UP, random, Math.min(2, random.nextInt(phase + 1)));
		}
		if (wastes) {
			if (fromCore > 6 && random.nextInt(phase >= 3 ? 320 : 600) == 0) {
				pike(world, groundPos.up(), 5 + random.nextInt(phase >= 3 ? 9 : 5), 0.9 + random.nextDouble() * (phase >= 3 ? 0.9 : 0.5), random);
			}
			if (biomesEnabled) {
				markBiome(world, x, z, groundTop);
			}
		}
	}

	/** A crystal pike of the crystal wastes, like the giant crystals of the world of the Amethyst but smaller. */
	private static void pike(ServerWorld world, BlockPos base, int height, double radius, Random random) {
		double leanX = (random.nextDouble() - 0.5) * 0.4;
		double leanZ = (random.nextDouble() - 0.5) * 0.4;
		int r = (int) Math.ceil(radius) + 1;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int y = 0; y < height; y++) {
			double t = y / (double) height;
			double rad = radius * (1.0 - t * 0.9);
			double cx = leanX * y;
			double cz = leanZ * y;
			for (int dx = -r - 2; dx <= r + 2; dx++) {
				for (int dz = -r - 2; dz <= r + 2; dz++) {
					double ddx = dx - cx;
					double ddz = dz - cz;
					double d = Math.sqrt(ddx * ddx + ddz * ddz);
					if (d > rad) {
						continue;
					}
					m.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
					BlockState old = world.getBlockState(m);
					if (!old.isAir() && !isCover(old)) {
						continue;
					}
					BlockState state;
					if (y >= height - 2) {
						state = ModBlocks.BRILLIANT_AMETHYST.getDefaultState();
					} else if (d > rad - 1.0) {
						state = Math.floorMod(dx + y + dz, 5) == 0 ? ModBlocks.BRILLIANT_AMETHYST.getDefaultState() : ModBlocks.CLEAR_AMETHYST.getDefaultState();
					} else {
						state = ModBlocks.AMETHYST_CRYSTAL_BLOCK.getDefaultState();
					}
					world.setBlockState(m, state, Block.NOTIFY_LISTENERS);
				}
			}
		}
		statPikes++;
	}

	// ---- the crystal wastes biome ----------------------------------------------------------------

	private static void markBiome(ServerWorld world, int x, int z, int groundY) {
		int bx = BiomeCoords.fromBlock(x);
		int bz = BiomeCoords.fromBlock(z);
		if (world.getBiomeForNoiseGen(bx, BiomeCoords.fromBlock(groundY + 1), bz).matchesKey(CRYSTAL_WASTES)) {
			return;
		}
		long key = ChunkPos.toLong(x >> 4, z >> 4);
		int bit = 1 << ((bx & 3) * 4 + (bz & 3));
		queues().biomeDirty.merge(key, bit, (a, b) -> a | b);
	}

	/** Turns the marked 4x4 cells of a couple of chunks into crystal wastes (from a little under the ground up) and tells the players. */
	private static void flushBiomes(ServerWorld world, Queues q) {
		if (q.biomeDirty.isEmpty()) {
			return;
		}
		var entry = world.getRegistryManager().get(RegistryKeys.BIOME).getEntry(CRYSTAL_WASTES);
		if (entry.isEmpty()) {
			biomesEnabled = false;
			q.biomeDirty.clear();
			return;
		}
		RegistryEntry<Biome> wastes = entry.get();
		long start = System.nanoTime();
		List<Chunk> changed = new ArrayList<>();
		Iterator<Map.Entry<Long, Integer>> it = q.biomeDirty.entrySet().iterator();
		while (it.hasNext() && changed.isEmpty()) {
			Map.Entry<Long, Integer> e = it.next();
			it.remove();
			ChunkPos pos = new ChunkPos(e.getKey());
			WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.x, pos.z);
			if (chunk == null) {
				continue;
			}
			int mask = e.getValue();
			Heightmap surface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
			chunk.populateBiomes((bx, by, bz, noise) -> {
				int lx = bx & 3;
				int lz = bz & 3;
				if ((mask >> (lx * 4 + lz) & 1) != 0) {
					int top = surface.get(lx * 4 + 2, lz * 4 + 2);
					if (by >= BiomeCoords.fromBlock(top) - 3) {
						return wastes;
					}
				}
				return chunk.getBiomeForNoiseGen(bx, by, bz);
			}, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
			chunk.setNeedsSaving(true);
			changed.add(chunk);
		}
		if (!changed.isEmpty()) {
			world.getChunkManager().chunkLoadingManager.sendChunkBiomePackets(changed);
			statBiomeChunks += changed.size();
		}
		long used = System.nanoTime() - start;
		statBiomeNanos += used;
		statBiomeMaxNanos = Math.max(statBiomeMaxNanos, used);
	}

	// ---- catch-up: chunks that load inside some land ---------------------------------------------

	/** How far from a player (in blocks) land is painted at all: far chunks wait until somebody comes near. */
	private static final double PAINT_RANGE = 176;
	private static final int PAINT_QUEUE_MAX = 300;
	/** The players of the current tick (filled in once per tick). */
	private static List<net.minecraft.server.network.ServerPlayerEntity> tickPlayers = List.of();

	private static List<net.minecraft.server.network.ServerPlayerEntity> playersOf(ServerWorld world) {
		List<net.minecraft.server.network.ServerPlayerEntity> list = new ArrayList<>(world.getPlayers(p -> !p.isSpectator()));
		for (net.minecraft.server.network.ServerPlayerEntity dummy : SelfTest.DUMMIES) {
			if (dummy.getWorld() == world) {
				list.add(dummy);
			}
		}
		return list;
	}

	private static boolean nearPlayer(List<net.minecraft.server.network.ServerPlayerEntity> players, int cx, int cz, double range) {
		double x = cx * 16 + 8;
		double z = cz * 16 + 8;
		for (net.minecraft.server.network.ServerPlayerEntity p : players) {
			double dx = p.getX() - x;
			double dz = p.getZ() - z;
			if (dx * dx + dz * dz <= range * range) {
				return true;
			}
		}
		return false;
	}

	/** Is any of these cores' land (or the tower's) over the chunk? */
	private static boolean landOver(ServerWorld world, Iterable<CoreState.Core> cores, int cx, int cz) {
		if (Math.abs(cx) <= 4 && Math.abs(cz) <= 4) {
			return true;
		}
		double x = cx * 16 + 8;
		double z = cz * 16 + 8;
		double far = (CoreManager.MAX_RADIUS + 12) * (CoreManager.MAX_RADIUS + 12);
		for (CoreState.Core core : cores) {
			double d = core.distanceSq(x, z);
			if (d > far) {
				continue;
			}
			double reach = CoreManager.radius(core, world) + 12;
			if (d <= reach * reach) {
				return true;
			}
		}
		return false;
	}

	private static void enqueue(Queues q, int cx, int cz) {
		long key = ChunkPos.toLong(cx, cz);
		if (q.paint.size() < PAINT_QUEUE_MAX && q.done.add(key) && q.queued.add(key)) {
			q.paint.addLast(new PaintJob(cx, cz));
		}
	}

	/** A chunk that loads next to a player is painted at once; the tower's own land always. Far chunks wait. */
	private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
		if (world.getRegistryKey() != World.OVERWORLD || StoryState.infectionStopped) {
			return;
		}
		ChunkPos pos = chunk.getPos();
		boolean tower = Math.abs(pos.x) <= 4 && Math.abs(pos.z) <= 4;
		if ((tower || nearPlayer(playersOf(world), pos.x, pos.z, PAINT_RANGE))
				&& landOver(world, CoreState.get(world.getServer()).cores.values(), pos.x, pos.z)) {
			Queues q = queues();
			if (tower) {
				long key = ChunkPos.toLong(pos.x, pos.z);
				if (q.queued.add(key)) {
					q.paint.addLast(new PaintJob(pos.x, pos.z));
				}
			} else {
				enqueue(q, pos.x, pos.z);
			}
		}
	}

	/**
	 * Chunks that were already loaded when land arrived (or when the player came closer) never fire the load
	 * event again, so every couple of seconds the chunks around each player are looked over.
	 */
	private static void scanAround(ServerWorld world, Queues q) {
		List<net.minecraft.server.network.ServerPlayerEntity> players = playersOf(world);
		if (players.isEmpty() || CoreManager.ACTIVE.isEmpty()) {
			return;
		}
		if (q.done.size() > 40000) {
			q.done.clear();
		}
		int r = (int) (PAINT_RANGE / 16);
		for (net.minecraft.server.network.ServerPlayerEntity p : players) {
			int pcx = p.getBlockX() >> 4;
			int pcz = p.getBlockZ() >> 4;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int cx = pcx + dx;
					int cz = pcz + dz;
					if (dx * dx + dz * dz > r * r || q.done.contains(ChunkPos.toLong(cx, cz))) {
						continue;
					}
					if (world.getChunkManager().isChunkLoaded(cx, cz) && landOver(world, CoreManager.ACTIVE, cx, cz)) {
						enqueue(q, cx, cz);
					}
				}
			}
		}
	}

	private static void preparePaint(ServerWorld world, PaintJob job) {
		job.cores = new ArrayList<>();
		double cx = job.cx * 16 + 8;
		double cz = job.cz * 16 + 8;
		double far = (CoreManager.MAX_RADIUS + 12) * (CoreManager.MAX_RADIUS + 12);
		for (CoreState.Core core : CoreState.get(world.getServer()).cores.values()) {
			double d = core.distanceSq(cx, cz);
			if (d > far) {
				continue;
			}
			double r = CoreManager.radius(core, world);
			if (d <= (r + 12) * (r + 12)) {
				prepare(core);
				job.cores.add(core);
			}
		}
		job.radii = new double[job.cores.size()];
		job.phases = new int[job.cores.size()];
		for (int i = 0; i < job.cores.size(); i++) {
			job.radii[i] = CoreManager.radius(job.cores.get(i), world);
			job.phases[i] = CoreManager.phase(job.cores.get(i), world);
		}
	}

	private static void paintSome(ServerWorld world, Queues q, Random random, long budget) {
		long start = System.nanoTime();
		BlockPos.Mutable m = new BlockPos.Mutable();
		while (!q.paint.isEmpty() && System.nanoTime() - start < budget) {
			PaintJob job = q.paint.peekFirst();
			if (!world.getChunkManager().isChunkLoaded(job.cx, job.cz)
					|| (job.next == 0 && !(Math.abs(job.cx) <= 4 && Math.abs(job.cz) <= 4) && !nearPlayer(playersOf(world), job.cx, job.cz, PAINT_RANGE + 32))) {
				q.paint.pollFirst();
				q.queued.remove(ChunkPos.toLong(job.cx, job.cz));
				q.done.remove(ChunkPos.toLong(job.cx, job.cz));
				continue;
			}
			if (job.cores == null) {
				preparePaint(world, job);
			}
			for (int n = 0; n < 16 && job.next < 256; n++, job.next++) {
				int x = job.cx * 16 + (job.next & 15);
				int z = job.cz * 16 + (job.next >> 4);
				if (protectedColumn(x, z)) {
					continue;
				}
				int phase = 0;
				boolean wastes = false;
				double fromCore = 0;
				if (inCenter(x, z)) {
					phase = 1;
				}
				for (int i = 0; i < job.cores.size(); i++) {
					CoreState.Core core = job.cores.get(i);
					if (job.phases[i] > phase && inLand(core, job.radii[i], x, z)) {
						phase = job.phases[i];
						fromCore = Math.sqrt(core.distanceSq(x + 0.5, z + 0.5));
						wastes = phase >= 2 && inLand(core, job.radii[i] * 0.85, x, z);
					}
				}
				if (phase > 0 && !isDone(world, x, z, phase, m)) {
					paintColumn(world, x, z, phase, wastes, fromCore, random, m);
				}
			}
			if (job.next >= 256) {
				q.paint.pollFirst();
				q.queued.remove(ChunkPos.toLong(job.cx, job.cz));
				statPainted++;
			}
		}
	}

	// ---- the march: the infection creeping out from the cores near players ------------------------

	/**
	 * Walks the rays of a core one after another and paints their next few columns, until the time is up.
	 * The cursor remembers where it stopped, so every tick the front moves a little further all round.
	 */
	private static void march(ServerWorld world, CoreState.Core core, long deadline, Random random, BlockPos.Mutable m) {
		prepare(core);
		int phase = CoreManager.phase(core, world);
		if (core.lastPhase != phase) {
			// older cores take more (stone too): look at every ray again
			core.lastPhase = phase;
			java.util.Arrays.fill(core.reach, 0);
		}
		double radius = CoreManager.radius(core, world);
		for (int n = 0; n < RAYS; n++) {
			if (System.nanoTime() > deadline) {
				return;
			}
			int ray = core.cursor;
			core.cursor = (core.cursor + 1) % RAYS;
			double rEff = radius * core.shape[ray];
			if (core.reach[ray] >= (int) rEff) {
				continue;
			}
			double a = (ray + 0.5) * Math.PI * 2 / RAYS - Math.PI;
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			statSteps++;
			int painted = 0;
			int d = Math.max(0, core.reach[ray] - 1);
			boolean blocked = false;
			for (; d <= rEff && painted < RUN; d++) {
				int x = MathHelper.floor(core.x + 0.5 + cos * d);
				int z = MathHelper.floor(core.z + 0.5 + sin * d);
				if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4) || !nearPlayer(tickPlayers, x >> 4, z >> 4, PAINT_RANGE + 32)) {
					// not loaded, or far from everybody (a distant chunk loaded for the far-away view): wait
					blocked = true;
					break;
				}
				if (!protectedColumn(x, z) && !isDone(world, x, z, phase, m)) {
					boolean wastes = phase >= 2 && d <= rEff * 0.85;
					paintColumn(world, x, z, phase, wastes, d, random, m);
					painted++;
					// a little wider than one column, so neighbouring rays leave no gaps
					for (Direction dir : Direction.Type.HORIZONTAL) {
						int nx = x + dir.getOffsetX();
						int nz = z + dir.getOffsetZ();
						if (inLand(core, radius, nx, nz) && world.getChunkManager().isChunkLoaded(nx >> 4, nz >> 4)
								&& !protectedColumn(nx, nz) && !isDone(world, nx, nz, phase, m)) {
							paintColumn(world, nx, nz, phase, wastes, d, random, m);
						}
					}
				}
				core.reach[ray] = d;
			}
			if (!blocked && d > rEff) {
				core.reach[ray] = (int) rEff;
			}
		}
	}

	// ---- crystals --------------------------------------------------------------------------------

	/** A crystal that can still grow: it will do so a while from now, when the engine gets to it. */
	public static void addGrowth(ServerWorld world, BlockPos pos, Block block, int factor) {
		if (!active(world)) {
			return;
		}
		Queues q = queues();
		if (q.growth.size() >= MAX_GROWTH) {
			return;
		}
		int speed = Math.max(1, world.getGameRules().getInt(ModGameRules.INFECTION_SPEED));
		int delay = (60 * factor + world.getRandom().nextInt(60 * factor)) * 100 / speed;
		q.growth.addLast(new Growth(pos.toImmutable(), block, world.getTime() + delay));
	}

	/** A crystal's random tick: it may rejoin the growth list (after a restart or when the list was full). */
	public static void offerGrowth(ServerWorld world, BlockPos pos, Block block, Random random) {
		if (active(world) && random.nextInt(16) == 0) {
			addGrowth(world, pos, block, 1);
		}
	}

	/** Takes the oldest crystal; if its time has come it grows one stage, if not it waits at the back. */
	private static boolean growOne(ServerWorld world, Queues q) {
		Growth g = q.growth.pollFirst();
		if (g == null) {
			return false;
		}
		if (world.getTime() < g.dueAt) {
			q.growth.addLast(g);
			return false;
		}
		if (!world.isChunkLoaded(g.pos)) {
			return false;
		}
		BlockState state = world.getBlockState(g.pos);
		if (!state.isOf(g.block) || !(g.block instanceof GrowingCrystalBlock crystal)) {
			return false;
		}
		crystal.grow(world, g.pos, state);
		statGrown++;
		return true;
	}

	// ---- every tick ------------------------------------------------------------------------------

	/**
	 * 1 while the server has time to spare, falling to 0 as the average tick approaches the 50 ms limit
	 * (full below 25 ms, a quarter at 35 ms, nothing from 45 ms): the infection gives way to the player.
	 */
	private static double loadFactor(ServerWorld world) {
		float ms = world.getServer().getAverageTickTime();
		if (ms <= 25) {
			return 1.0;
		}
		if (ms >= 45) {
			return 0.0;
		}
		return (45 - ms) / 20.0;
	}

	private static void tick(ServerWorld world) {
		if (!active(world)) {
			return;
		}
		Queues q = queues();
		long start = System.nanoTime();
		Random random = world.getRandom();
		if (world.getTime() % 200 == 0 || sites.length == 0) {
			refreshSites(world);
		}
		// 100 = the normal budget; the gamerule goes up to 1000 (ten times as much work per tick)
		double scale = Math.min(1000, world.getGameRules().getInt(ModGameRules.INFECTION_SPEED)) / 100.0;
		scale *= loadFactor(world);
		long paintBudget = (long) (PAINT_BUDGET_NANOS * scale);
		long marchBudget = (long) (MARCH_BUDGET_NANOS * scale);

		tickPlayers = playersOf(world);
		if (world.getTime() % 40 == 7) {
			scanAround(world, q);
		}
		if (!q.paint.isEmpty()) {
			paintSome(world, q, random, paintBudget);
		}

		BlockPos.Mutable m = new BlockPos.Mutable();
		List<CoreState.Core> cores = CoreManager.ACTIVE;
		if (!cores.isEmpty()) {
			// a few cores per tick, each with an equal slice of the time, taking turns
			int count = Math.min(cores.size(), MAX_CORES_PER_TICK);
			long slice = marchBudget / count;
			for (int i = 0; i < count; i++) {
				q.next = (q.next + 1) % cores.size();
				march(world, cores.get(q.next), System.nanoTime() + slice, random, m);
			}
		}

		q.growthOwed = Math.min(GROWTHS_PER_TICK * 2 * Math.max(1, scale), q.growthOwed + GROWTHS_PER_TICK * scale);
		int guard = 0;
		int limit = q.growth.size();
		while (q.growthOwed >= 1 && !q.growth.isEmpty() && guard++ < Math.min(limit, 12 * Math.max(1, (int) scale))) {
			if (growOne(world, q)) {
				q.growthOwed -= 1;
			}
			if (System.nanoTime() - start > marchBudget + paintBudget) {
				break;
			}
		}

		if (world.getTime() % 20 == 0) {
			flushBiomes(world, q);
		}
		long used = System.nanoTime() - start;
		statTicks++;
		statNanos += used;
		statMaxNanos = Math.max(statMaxNanos, used);
	}
}
