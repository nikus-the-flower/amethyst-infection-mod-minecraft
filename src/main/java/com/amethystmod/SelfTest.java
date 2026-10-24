package com.amethystmod;

import com.amethystmod.block.ModBlocks;
import com.amethystmod.entity.HeartCrystalEntity;
import com.amethystmod.entity.HeartEntity;
import com.amethystmod.entity.ModEntities;
import com.amethystmod.world.AmethystWorld;
import com.amethystmod.world.TempleBuilder;
import com.amethystmod.world.TempleDesign;
import com.amethystmod.world.TowerBuilder;
import com.amethystmod.world.TowerDesign;
import com.amethystmod.world.TowerState;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development self-test, only active with -Damethystmod.selftest=true. Once the tower and the temple
 * are built it draws maps of the Amethyst world and cross-sections of both buildings into
 * run/selftest/, then fights the Heart with two invulnerable dummy players through every stage,
 * checks that the return portal opens, writes a report and stops the server.
 */
public final class SelfTest {
	/** Fake players used as boss targets during the test (empty otherwise). */
	public static final List<ServerPlayerEntity> DUMMIES = new ArrayList<>();
	private static final boolean ENABLED = Boolean.getBoolean("amethystmod.selftest");
	private static final int MAP_RADIUS = Integer.getInteger("amethystmod.selftest.radius", 192);
	private static final File OUT = new File("selftest");
	private static final List<String> REPORT = new ArrayList<>();

	private static int phase;
	/** The story places test: ticks left, the altar, the dummy talking to Ricardo. */
	private static int siteTicks;
	private static BlockPos altarPos;
	private static ServerPlayerEntity siteDummy;
	/** The infection core test: ticks since its start (0 = not running), the world, its dummy and its core. */
	private static int coreTicks;
	private static ServerWorld coreWorld;
	private static ServerPlayerEntity coreDummy;
	private static com.amethystmod.world.CoreState.Core testCore;
	private static final int CORE_X = 170;
	private static final int CORE_Z = 40;
	private static final int CORE_TEST_TICKS = 1000;
	/** The Amitariel test runs alongside the rest: ticks left, his dummy, the boss. */
	private static int amitTicks;
	private static ServerPlayerEntity amitDummy;
	private static com.amethystmod.entity.AmitarielEntity amitariel;
	private static int ticks;
	private static int mapRow;
	private static BufferedImage map;
	private static BufferedImage biomes;
	private static final Map<String, Integer> biomeColors = new HashMap<>();
	private static final Map<String, Integer> biomeCounts = new HashMap<>();
	private static HeartEntity heart;
	private static final int[] STAGE_END = {1300, 2400, 3500, 7000};
	private static HeartEntity.Move lastMove = HeartEntity.Move.NONE;

	private SelfTest() {
	}

	public static void register() {
		if (!ENABLED) {
			return;
		}
		ServerLifecycleEvents.SERVER_STARTED.register(server -> log("server started"));
		ServerTickEvents.END_SERVER_TICK.register(SelfTest::tick);
	}

	private static void log(String line) {
		AmethystMod.LOGGER.info("[selftest] {}", line);
		REPORT.add(line);
	}

	private static void tick(MinecraftServer server) {
		try {
			step(server);
		} catch (Throwable e) {
			log("FAILED: " + e);
			for (StackTraceElement el : e.getStackTrace()) {
				REPORT.add("    at " + el);
			}
			finish(server);
		}
	}

	private static void step(MinecraftServer server) throws IOException {
		ServerWorld amethyst = server.getWorld(AmethystWorld.WORLD);
		ticks++;
		if (amitTicks > 0) {
			amitarielTick(server.getOverworld());
		}
		if (coreTicks > 0) {
			coreTick();
		}
		if (siteTicks > 0) {
			sitesTick(server.getOverworld());
		}
		switch (phase) {
			case 0 -> {
				if (amethyst == null) {
					log("FAILED: the Amethyst world does not exist");
					finish(server);
					return;
				}
				if (!TowerBuilder.busy() && !TempleBuilder.busy() && ticks > 40) {
					log("tower and temple built after " + ticks + " ticks");
					OUT.mkdirs();
					sliceTower(server.getOverworld());
					sliceTemple(amethyst);
					int size = MAP_RADIUS * 2;
					map = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
					biomes = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
					phase = 1;
				}
			}
			case 1 -> {
				for (int n = 0; n < 4 && mapRow < MAP_RADIUS * 2; n++, mapRow++) {
					mapRow(amethyst, mapRow);
				}
				if (mapRow >= MAP_RADIUS * 2) {
					ImageIO.write(map, "png", new File(OUT, "amethyst_map.png"));
					slice(amethyst, 120, -MAP_RADIUS, MAP_RADIUS - 1, 0, 255, "terrain_slice_x120.png");
					Map<String, Integer> surface = new HashMap<>();
					for (int x = -MAP_RADIUS; x < MAP_RADIUS; x += 3) {
						for (int z = -MAP_RADIUS; z < MAP_RADIUS; z += 3) {
							BlockPos top = new BlockPos(x, amethyst.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1, z);
							surface.merge(amethyst.getBlockState(top).getBlock().getTranslationKey(), 1, Integer::sum);
						}
					}
					surface.forEach((k, v) -> log("surface block " + k + ": " + v));
					ImageIO.write(biomes, "png", new File(OUT, "amethyst_biomes.png"));
					int total = biomeCounts.values().stream().mapToInt(Integer::intValue).sum();
					biomeCounts.forEach((k, v) -> log(String.format("biome %s: %.1f%% (color #%06x)", k, 100.0 * v / total, biomeColors.get(k))));
					log("maps written");
					for (int cx = -3; cx <= 2; cx++) {
						for (int cz = -3; cz <= 2; cz++) {
							amethyst.setChunkForced(cx, cz, true);
						}
					}
					phase = 2;
					ticks = 0;
				}
			}
			case 2 -> {
				if (ticks == 60) {
					startFight(amethyst);
					phase = 3;
					ticks = 0;
				}
			}
			case 3 -> fight(server, amethyst);
			default -> {
			}
		}
	}

	// ---- pictures ------------------------------------------------------------------------

	private static int shade(int rgb, double f) {
		int r = (int) Math.min(255, ((rgb >> 16) & 255) * f);
		int g = (int) Math.min(255, ((rgb >> 8) & 255) * f);
		int b = (int) Math.min(255, (rgb & 255) * f);
		return r << 16 | g << 8 | b;
	}

	private static int colorOf(ServerWorld world, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return 0x100818;
		}
		if (state.isOf(ModBlocks.CLEAR_AMETHYST)) {
			return 0xC8A0FF;
		}
		if (state.isOf(ModBlocks.BRILLIANT_AMETHYST)) {
			return 0xFFD0FF;
		}
		return state.getMapColor(world, pos).getRenderColor(MapColor.Brightness.NORMAL) & 0xFFFFFF;
	}

	private static void mapRow(ServerWorld world, int row) {
		int z = row - MAP_RADIUS;
		for (int col = 0; col < MAP_RADIUS * 2; col++) {
			int x = col - MAP_RADIUS;
			world.getChunk(x >> 4, z >> 4);
			int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			int rgb;
			if (top <= world.getBottomY()) {
				rgb = 0x05030A;
			} else {
				BlockPos pos = new BlockPos(x, top - 1, z);
				rgb = shade(colorOf(world, pos, world.getBlockState(pos)), 0.45 + 0.75 * (top - 30) / 150.0);
			}
			map.setRGB(col, row, rgb);
			String biome = world.getBiome(new BlockPos(x, Math.max(top, 80), z)).getKey().map(k -> k.getValue().getPath()).orElse("?");
			int color = biomeColors.computeIfAbsent(biome, b -> 0x404040 + (b.hashCode() & 0xBFBFBF));
			biomeCounts.merge(biome, 1, Integer::sum);
			biomes.setRGB(col, row, top <= world.getBottomY() ? 0 : color);
		}
	}

	private static void slice(ServerWorld world, int minZ, int maxZ, int minY, int maxY, String name) throws IOException {
		slice(world, 0, minZ, maxZ, minY, maxY, name);
	}

	private static void slice(ServerWorld world, int x, int minZ, int maxZ, int minY, int maxY, String name) throws IOException {
		BufferedImage img = new BufferedImage(maxZ - minZ + 1, maxY - minY + 1, BufferedImage.TYPE_INT_RGB);
		for (int z = minZ; z <= maxZ; z++) {
			for (int y = minY; y <= maxY; y++) {
				BlockPos pos = new BlockPos(x, y, z);
				img.setRGB(z - minZ, maxY - y, colorOf(world, pos, world.getBlockState(pos)));
			}
		}
		ImageIO.write(img, "png", new File(OUT, name));
	}

	private static void sliceTower(ServerWorld overworld) throws IOException {
		TowerState state = TowerState.get(overworld);
		TowerDesign design = new TowerDesign(state.baseY);
		overworld.getChunk(0, 0);
		slice(overworld, -40, 40, state.baseY - 8, design.topY() + 2, "tower_slice.png");
		front(overworld, state.baseY - 8, design.topY() + 2, "tower_front.png");
		log("tower base y=" + state.baseY + ", top y=" + design.topY() + ", hall floor " + design.floorY(TowerDesign.BOSS_FLOOR)
				+ ", amitariel " + overworld.getEntitiesByType(ModEntities.AMITARIEL, new Box(-20, state.baseY, -20, 20, design.topY(), 20), e -> true).size());
		int blocked = 0;
		for (int z = 13; z <= TowerDesign.REACH; z++) {
			for (int x = -1; x <= 1; x++) {
				for (int y = state.baseY + 1; y <= state.baseY + 2; y++) {
					if (!overworld.getBlockState(new BlockPos(x, y, z)).getCollisionShape(overworld, new BlockPos(x, y, z)).isEmpty()) {
						blocked++;
					}
				}
			}
		}
		log("path to the door: " + blocked + " solid blocks in the way");
		startAmitarielTest(overworld, design);
		startCoreTest(overworld);
		startSitesTest(overworld);
		// Amitariel's fall opens the rift in his hall
		com.amethystmod.entity.RiftManager.openTowerRift(overworld);
		log("tower rift after his fall: " + overworld.getEntitiesByType(ModEntities.RIFT, new Box(design.riftPos()).expand(3), e -> true).size()
				+ " at " + design.riftPos() + ", arrival block " + overworld.getBlockState(design.arrivalPos()).getBlock()
				+ " / " + overworld.getBlockState(design.arrivalPos().up()).getBlock());
	}

	/** The tower seen from the south: the first block met along each line of sight. */
	private static void front(ServerWorld world, int minY, int maxY, String name) throws IOException {
		int r = TowerDesign.REACH + 4;
		BufferedImage img = new BufferedImage(r * 2 + 1, maxY - minY + 1, BufferedImage.TYPE_INT_RGB);
		for (int x = -r; x <= r; x++) {
			for (int y = minY; y <= maxY; y++) {
				int color = 0x9AB8E0;
				for (int z = r; z >= -r; z--) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState s = world.getBlockState(pos);
					if (!s.isAir()) {
						color = shade(colorOf(world, pos, s), 0.6 + 0.4 * (z + r) / (2.0 * r));
						break;
					}
				}
				img.setRGB(x + r, maxY - y, color);
			}
		}
		ImageIO.write(img, "png", new File(OUT, name));
	}

	private static void sliceTemple(ServerWorld world) throws IOException {
		slice(world, -50, 50, TempleDesign.bottomY(), TempleDesign.topY() + 2, "temple_slice.png");
		BufferedImage img = new BufferedImage(97, 97, BufferedImage.TYPE_INT_RGB);
		int y = TempleDesign.FLOOR_Y + 1;
		for (int x = -48; x <= 48; x++) {
			for (int z = -48; z <= 48; z++) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockState s = world.getBlockState(pos);
				img.setRGB(x + 48, z + 48, s.isAir() ? colorOf(world, pos.down(), world.getBlockState(pos.down())) : shade(colorOf(world, pos, s), 1.4));
			}
		}
		ImageIO.write(img, "png", new File(OUT, "temple_floor_plan.png"));
	}

	// ---- the fight -----------------------------------------------------------------------

	private static void startFight(ServerWorld world) {
		List<HeartEntity> hearts = world.getEntitiesByType(ModEntities.HEART, new Box(-40, 60, -40, 40, 200, 40), Entity::isAlive);
		if (hearts.isEmpty()) {
			log("FAILED: no Heart in the temple");
			finish(world.getServer());
			return;
		}
		heart = hearts.get(0);
		log("Heart found at " + heart.getBlockPos() + " health " + heart.getHealth());
		double[][] spots = {{9, 0}, {-7, 7}};
		for (int i = 0; i < spots.length; i++) {
			FakePlayer dummy = FakePlayer.get(world, new GameProfile(UUID.nameUUIDFromBytes(("amethyst-dummy-" + i).getBytes(StandardCharsets.UTF_8)), "Dummy" + i));
			dummy.refreshPositionAndAngles(heart.getX() + spots[i][0], heart.getY(), heart.getZ() + spots[i][1], 0, 0);
			dummy.setInvulnerable(true);
			DUMMIES.add(dummy);
		}
	}

	private static String lastStatus = "";

	private static void fight(MinecraftServer server, ServerWorld world) {
		if (heart == null) {
			return;
		}
		if (ticks % 20 == 0) {
			String status = "stage " + heart.getStage() + " health " + Math.round(heart.getHealth()) + "/" + Math.round(heart.getMaxHealth())
					+ " pylons " + world.getEntitiesByType(ModEntities.HEART_PYLON, heart.getBoundingBox().expand(40), Entity::isAlive).size()
					+ " sentinels " + world.getEntitiesByType(ModEntities.HEART_SENTINEL, heart.getBoundingBox().expand(40), Entity::isAlive).size()
					+ " nodes " + world.getEntitiesByType(ModEntities.HEART_NODE, heart.getBoundingBox().expand(10), Entity::isAlive).size();
			if (!status.equals(lastStatus)) {
				log("t=" + ticks + " " + status);
				lastStatus = status;
			}
		}
		if (ticks == 160) {
			for (HeartCrystalEntity pylon : world.getEntitiesByType(ModEntities.HEART_PYLON, heart.getBoundingBox().expand(40), Entity::isAlive)) {
				pylon.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
			}
			log("pylons destroyed");
		}
		HeartEntity.Move move = heart.currentMove();
		if (move != lastMove) {
			if (move != HeartEntity.Move.NONE) {
				log("t=" + ticks + " stage " + heart.getStage() + " attack " + move);
			}
			lastMove = move;
		}
		// each phase runs for a while, then its health bar is emptied in one blow
		for (int i = 0; i < 3; i++) {
			if (ticks == STAGE_END[i]) {
				heart.damage(world.getDamageSources().magic(), 1_000_000f);
				log("t=" + ticks + " phase bar emptied: stage now " + heart.getStage() + " health " + heart.getHealth() + "/" + heart.getMaxHealth());
			}
			if (ticks == STAGE_END[i] - 300) {
				probeWeakPoints(world);
			}
		}
		if (ticks == STAGE_END[2] + 10) {
			// the last phase plays every brick-built attack once, so each one runs at least once
			for (HeartEntity.Move m : HeartEntity.Move.values()) {
				if (m.scripted()) {
					HeartEntity.FORCED.add(m);
				}
			}
		}
		int end = STAGE_END[3];
		if (ticks == end - 300) {
			probeWeakPoints(world);
			heart.setHealth(heart.getMaxHealth() * 0.06f);
			log("Heart health set to 6% of the last bar");
		}
		if (ticks == end) {
			heart.damage(world.getDamageSources().magic(), 1_000_000f);
			log("Heart dealt a killing blow: dying=" + heart.isAlive() + " health " + heart.getHealth());
		}
		if (ticks == end + 160) {
			log("after death scene: heart alive=" + heart.isAlive());
			log("temple rift after the Heart: " + world.getEntitiesByType(ModEntities.RIFT,
					new Box(BlockPos.ofFloored(com.amethystmod.entity.RiftManager.templeRift())).expand(3), e -> true).size()
					+ ", old frame " + world.getBlockState(TempleDesign.portalKeystone()).getBlock());
			log("SUCCESS");
			finish(server);
		}
	}

	/** Infected columns (top block infected) and land columns (top block not water) in a square around a point. */
	private static int[] countLand(ServerWorld world, int cx, int cz, int r) {
		int infected = 0;
		int land = 0;
		// an unloaded chunk would read as empty ground: load them all first
		for (int x = (cx - r) >> 4; x <= (cx + r) >> 4; x++) {
			for (int z = (cz - r) >> 4; z <= (cz + r) >> 4; z++) {
				world.getChunk(x, z);
			}
		}
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > r * r) {
					continue;
				}
				BlockState top = world.getBlockState(pos.set(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z));
				if (!top.getFluidState().isEmpty()) {
					continue;
				}
				land++;
				if (InfectionSpread.isInfected(top)) {
					infected++;
				}
			}
		}
		return new int[]{infected, land};
	}

	private static int countWastes(ServerWorld world, int cx, int cz, int r) {
		int n = 0;
		for (int x = cx - r; x <= cx + r; x += 4) {
			for (int z = cz - r; z <= cz + r; z += 4) {
				int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
				if (world.getBiome(new BlockPos(x, y, z)).matchesKey(InfectionManager.CRYSTAL_WASTES)) {
					n++;
				}
			}
		}
		return n;
	}

	private static com.amethystmod.entity.InfectionCoreEntity coreEntity(ServerWorld world) {
		List<com.amethystmod.entity.InfectionCoreEntity> list = world.getEntitiesByType(ModEntities.INFECTION_CORE,
				new Box(CORE_X - 4, world.getBottomY(), CORE_Z - 4, CORE_X + 5, world.getTopY(), CORE_Z + 5), e -> e.isAlive());
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * The infection core test, away from the tower: plants a phase-1 core with a dummy beside it and lets it
	 * paint its land; then raises it to phase 3 and checks the barrier (a plain blow and a purified one), its
	 * creatures, the crystal wastes, a geode, and that its land stays when it dies. Also checks the tower's land.
	 */
	private static void startCoreTest(ServerWorld world) {
		for (int cx = (CORE_X >> 4) - 6; cx <= (CORE_X >> 4) + 6; cx++) {
			for (int cz = (CORE_Z >> 4) - 6; cz <= (CORE_Z >> 4) + 6; cz++) {
				world.setChunkForced(cx, cz, true);
				world.getChunk(cx, cz);
			}
		}
		coreWorld = world;
		int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, CORE_X, CORE_Z);
		FakePlayer dummy = FakePlayer.get(world, new GameProfile(UUID.nameUUIDFromBytes("amethyst-dummy-core".getBytes(StandardCharsets.UTF_8)), "DummyC"));
		dummy.refreshPositionAndAngles(CORE_X + 4, ground, CORE_Z, 0, 0);
		dummy.setInvulnerable(true);
		coreDummy = dummy;
		DUMMIES.add(dummy);
		int[] before = countLand(world, CORE_X, CORE_Z, 40);
		InfectionManager.resetStats();
		testCore = CoreManager.plant(world, new BlockPos(CORE_X, ground, CORE_Z), 1);
		// most of the way through phase 1, so that its land is already some 20 blocks wide
		testCore.born -= (long) (CoreManager.phase2Ticks(world) * 0.6);
		coreTicks = 1;
		log("Core test: server tick before the core: avg " + String.format("%.1f", world.getServer().getAverageTickTime()) + " ms");
		int[] center = countLand(world, 0, 0, 46);
		log("Core test: core #" + testCore.id + " planted at " + CORE_X + " " + ground + " " + CORE_Z + ", land radius "
				+ Math.round(CoreManager.radius(testCore, world)) + ", infected columns around before: " + before[0] + "/" + before[1]
				+ "; the tower's land (r 46): " + center[0] + "/" + center[1] + " columns infected");
	}

	private static void coreTick() {
		ServerWorld world = coreWorld;
		int t = coreTicks++;
		com.amethystmod.entity.InfectionCoreEntity core = coreEntity(world);
		if (t == 400) {
			int[] land = countLand(world, CORE_X, CORE_Z, 40);
			log("Core test, phase 1 after 400 ticks: entity " + (core != null) + (core != null ? " phase " + core.getPhase() + " height " + core.getHeight() : "")
					+ ", infected columns " + land[0] + "/" + land[1] + "; " + InfectionManager.stats());
			try {
				topView(world, new BlockPos(CORE_X, world.getTopY(Heightmap.Type.MOTION_BLOCKING, CORE_X, CORE_Z), CORE_Z), 60, "core_phase1_top.png");
			} catch (IOException e) {
				log("Core test: " + e);
			}
			InfectionManager.resetStats();
			CoreManager.setPhase(world, testCore, 3);
		}
		if (t == 460 && core != null) {
			coreDummy.refreshPositionAndAngles(core.getX() + 2.5, core.getY(), core.getZ(), 90, 0);
			log("Core test, phase 3: entity phase " + core.getPhase() + ", height " + core.getHeight() + ", health " + core.getHealth()
					+ ", barrier " + core.getBarrier());
			coreDummy.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_SWORD));
			float h = core.getHealth();
			float b = core.getBarrier();
			core.damage(world.getDamageSources().playerAttack(coreDummy), 10.0f);
			log("Core test: a diamond sword blow: health " + h + " -> " + core.getHealth() + ", barrier " + b + " -> " + core.getBarrier());
			coreDummy.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new net.minecraft.item.ItemStack(com.amethystmod.item.ModItems.PURIFIER_BLADE));
			h = core.getHealth();
			b = core.getBarrier();
			core.damage(world.getDamageSources().playerAttack(coreDummy), 10.0f);
			log("Core test: a purifier's blade blow: health " + h + " -> " + core.getHealth() + ", barrier " + b + " -> " + core.getBarrier()
					+ " (purified: " + com.amethystmod.entity.InfectionCoreEntity.purified(world.getDamageSources().playerAttack(coreDummy)) + ")");
			coreDummy.refreshPositionAndAngles(CORE_X + 4, core.getY(), CORE_Z, 0, 0);
		}
		if (t >= 420 && t < CORE_TEST_TICKS && t % 60 == 0) {
			int[] land = countLand(world, CORE_X, CORE_Z, 92);
			log("Core test, front: tick " + t + ", land radius " + Math.round(CoreManager.radius(testCore, world)) + ", infected columns " + land[0]
					+ "/" + land[1] + ", whole server tick avg " + String.format("%.1f", world.getServer().getAverageTickTime()) + " ms");
		}
		if (t == CORE_TEST_TICKS) {
			int[] land = countLand(world, CORE_X, CORE_Z, 60);
			int mobs = world.getEntitiesByClass(com.amethystmod.entity.AmethystMob.class, new Box(CORE_X - 24, world.getBottomY(), CORE_Z - 24,
					CORE_X + 24, world.getTopY(), CORE_Z + 24), e -> !(e instanceof com.amethystmod.entity.InfectionCoreEntity)).size();
			log("Core test, phase 3 after " + (CORE_TEST_TICKS - 400) + " ticks: land radius " + Math.round(CoreManager.radius(testCore, world))
					+ ", infected columns " + land[0] + "/" + land[1] + ", crystal wastes cells " + countWastes(world, CORE_X, CORE_Z, 60)
					+ ", creatures around " + mobs + ", barrier " + (core != null ? core.getBarrier() : -1) + "; " + InfectionManager.stats());
			try {
				topView(world, new BlockPos(CORE_X, world.getTopY(Heightmap.Type.MOTION_BLOCKING, CORE_X, CORE_Z), CORE_Z), 70, "core_phase3_top.png");
				slice(world, CORE_X, CORE_Z - 70, CORE_Z + 70, world.getTopY(Heightmap.Type.MOTION_BLOCKING, CORE_X, CORE_Z) - 20,
						world.getTopY(Heightmap.Type.MOTION_BLOCKING, CORE_X, CORE_Z) + 25, "core_phase3_slice.png");
			} catch (IOException e) {
				log("Core test: " + e);
			}
			com.amethystmod.world.CoreState.Core child = CoreManager.throwGeode(world, testCore);
			log("Core test: geode " + (child == null ? "found no place" : "landed at " + child.x + " " + child.z + " (core #" + child.id + ", phase "
					+ CoreManager.phase(child, world) + ")") + "; cores in the world " + com.amethystmod.world.CoreState.get(world.getServer()).cores.size());
			if (core != null) {
				core.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
			}
			int[] after = countLand(world, CORE_X, CORE_Z, 60);
			log("Core test: core destroyed (alive " + (core != null && core.isAlive()) + ", record left "
					+ (CoreManager.core(world.getServer(), testCore.id) != null) + "), infected columns now " + after[0] + "/" + after[1]);
			int[] center = countLand(world, 0, 0, 46);
			log("Core test: the tower's land (r 46): " + center[0] + "/" + center[1] + " columns infected");
			try {
				topView(world, new BlockPos(0, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 30, 30), 0), 64, "tower_land_top.png");
			} catch (IOException e) {
				log("Core test: " + e);
			}
			DUMMIES.remove(coreDummy);
			coreDummy.discard();
			for (int cx = (CORE_X >> 4) - 6; cx <= (CORE_X >> 4) + 6; cx++) {
				for (int cz = (CORE_Z >> 4) - 6; cz <= (CORE_Z >> 4) + 6; cz++) {
					world.setChunkForced(cx, cz, false);
				}
			}
			coreTicks = 0;
		}
	}

	/** Top-down picture of a place: the colour of the highest block of each column. */
	private static void topView(ServerWorld world, BlockPos c, int r, String name) throws IOException {
		BufferedImage img = new BufferedImage(r * 2 + 1, r * 2 + 1, BufferedImage.TYPE_INT_RGB);
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, c.getX() + x, c.getZ() + z) - 1;
				BlockPos pos = new BlockPos(c.getX() + x, top, c.getZ() + z);
				int color = colorOf(world, pos, world.getBlockState(pos));
				img.setRGB(x + r, z + r, shade(color, 0.55 + 0.45 * Math.max(0, Math.min(1, (top - c.getY() + 12) / 24.0))));
			}
		}
		ImageIO.write(img, "png", new File(OUT, name));
	}

	/** Builds every story place now, takes pictures, then tests the altar and Ricardo's gift. */
	private static void startSitesTest(ServerWorld world) {
		try {
			for (com.amethystmod.world.WorldSites.Kind kind : com.amethystmod.world.WorldSites.Kind.values()) {
				com.amethystmod.world.WorldSites.Site site = com.amethystmod.world.WorldSites.site(world.getServer(), kind);
				if (!site.built) {
					com.amethystmod.world.WorldSites.buildNow(world, site);
				}
				BlockPos o = site.origin();
				world.setChunkForced(o.getX() >> 4, o.getZ() >> 4, true);
				int chests = 0;
				for (BlockPos p : BlockPos.iterate(o.add(-30, -16, -30), o.add(30, 14, 30))) {
					if (world.getBlockState(p).isOf(net.minecraft.block.Blocks.CHEST)) {
						chests++;
					}
				}
				log("Site " + kind.id() + " at " + o + " (distance " + Math.round(Math.sqrt((double) o.getX() * o.getX() + (double) o.getZ() * o.getZ()))
						+ "), chests " + chests);
				topView(world, o, 34, "site_" + kind.id() + "_top.png");
				slice(world, o.getX(), o.getZ() - 34, o.getZ() + 34, o.getY() - 16, o.getY() + 18, "site_" + kind.id() + "_slice.png");
			}
			log("spawn point " + world.getSpawnPos());
			com.amethystmod.world.WorldSites.Site crater = com.amethystmod.world.WorldSites.site(world.getServer(), com.amethystmod.world.WorldSites.Kind.CRATER);
			altarPos = crater.origin().add(0, -com.amethystmod.world.SiteDesigns.CRATER_DEPTH + 1, 0);
			List<com.amethystmod.block.AltarBlockEntity> pedestals = com.amethystmod.block.AltarBlockEntity.pedestals(world, altarPos);
			log("altar block " + world.getBlockState(altarPos).getBlock() + ", pedestals " + pedestals.size()
					+ ", ricardo " + world.getEntitiesByType(ModEntities.RICARDO, new Box(altarPos).expand(30), e -> true).size());
			net.minecraft.item.Item[] relics = {com.amethystmod.item.ModItems.PURITY_SHARD, com.amethystmod.item.ModItems.EMPEROR_CROWN,
					com.amethystmod.item.ModItems.CHAOS_FLASK};
			for (int i = 0; i < pedestals.size() && i < 3; i++) {
				pedestals.get(i).setStack(new net.minecraft.item.ItemStack(relics[i]));
			}
			log("altar awake with the relics: " + com.amethystmod.block.AltarBlockEntity.awake(world, altarPos));
			if (world.getBlockEntity(altarPos) instanceof com.amethystmod.block.AltarBlockEntity altar) {
				altar.beginRitual(new net.minecraft.item.ItemStack(com.amethystmod.item.ModItems.AMETHYST_ESSENCE, 5));
			}
			siteTicks = 120;
		} catch (IOException e) {
			log("Sites test: " + e);
		}
	}

	private static void sitesTick(ServerWorld world) {
		siteTicks--;
		if (siteTicks > 0) {
			return;
		}
		if (world.getBlockEntity(altarPos) instanceof com.amethystmod.block.AltarBlockEntity altar) {
			log("after the ritual the altar holds " + altar.getStack() + ", story altar awakened "
					+ com.amethystmod.world.StoryState.get(world.getServer()).altarAwakened);
		}
		List<com.amethystmod.entity.RicardoEntity> ricardo = world.getEntitiesByType(ModEntities.RICARDO, new Box(altarPos).expand(30), e -> true);
		if (!ricardo.isEmpty()) {
			FakePlayer dummy = FakePlayer.get(world, new GameProfile(UUID.nameUUIDFromBytes("amethyst-dummy-ricardo".getBytes(StandardCharsets.UTF_8)), "DummyR"));
			dummy.refreshPositionAndAngles(ricardo.get(0).getX() + 1, ricardo.get(0).getY(), ricardo.get(0).getZ(), 0, 0);
			ricardo.get(0).act(dummy, "gift");
			log("Ricardo's gift: amulet " + dummy.getInventory().contains(new net.minecraft.item.ItemStack(com.amethystmod.item.ModItems.PURITY_AMULET))
					+ ", offers " + ricardo.get(0).getOffers().size());
		}
	}

	/** Fights Amitariel in his hall with a dummy: each new move is played once, then he is left be. */
	private static void startAmitarielTest(ServerWorld world, TowerDesign design) {
		world.setChunkForced(0, 0, true);
		List<com.amethystmod.entity.AmitarielEntity> found = world.getEntitiesByType(ModEntities.AMITARIEL,
				new Box(-20, design.baseY, -20, 20, design.topY(), 20), e -> true);
		if (found.isEmpty()) {
			log("Amitariel test: no Amitariel");
			return;
		}
		amitariel = found.get(0);
		FakePlayer dummy = FakePlayer.get(world, new GameProfile(UUID.nameUUIDFromBytes("amethyst-dummy-amitariel".getBytes(StandardCharsets.UTF_8)), "DummyA"));
		dummy.refreshPositionAndAngles(amitariel.getX() + 7, amitariel.getY(), amitariel.getZ() + 1, 0, 0);
		dummy.setInvulnerable(true);
		amitDummy = dummy;
		DUMMIES.add(dummy);
		amitariel.debugFight(dummy);
		amitTicks = 360;
		log("Amitariel test: fight started, " + amitariel.debugState());
	}

	private static void amitarielTick(ServerWorld world) {
		amitTicks--;
		int t = 360 - amitTicks;
		if (t == 20 || t == 120 || t == 220) {
			amitariel.debugMove((t - 20) / 100);
			log("Amitariel test: move " + new String[]{"whirl", "leap", "spears"}[(t - 20) / 100] + " started, " + amitariel.debugState());
		}
		if (t == 60 || t == 160 || t == 260) {
			log("Amitariel test: +40 ticks, " + amitariel.debugState() + ", phantom spears "
					+ world.getEntitiesByType(ModEntities.PHANTOM_SPEAR, amitariel.getBoundingBox().expand(40), e -> true).size());
		}
		if (amitTicks == 0) {
			log("Amitariel test: done, " + amitariel.debugState() + ", alive " + amitariel.isAlive() + ", phantom spears left "
					+ world.getEntitiesByType(ModEntities.PHANTOM_SPEAR, amitariel.getBoundingBox().expand(40), e -> true).size());
			DUMMIES.remove(amitDummy);
			amitDummy.discard();
			world.setChunkForced(0, 0, false);
		}
	}

	/** A dummy strikes the body, then aims at a weak point and strikes it, and reports both. */
	private static void probeWeakPoints(ServerWorld world) {
		ServerPlayerEntity dummy = DUMMIES.stream().filter(d -> d.getWorld() == world).findFirst().orElse(DUMMIES.get(0));
		List<net.minecraft.util.math.Vec3d> points = heart.weakPoints();
		net.minecraft.util.math.Vec3d target = points.isEmpty() ? heart.getPos().add(0, 3, 0) : points.get(0);
		net.minecraft.util.math.Vec3d stand = new net.minecraft.util.math.Vec3d(target.x, heart.getY() - 1, target.z)
				.add(target.subtract(heart.getPos()).multiply(1, 0, 1).normalize().multiply(2.5));
		dummy.refreshPositionAndAngles(stand.x, stand.y, stand.z, 0, 0);
		dummy.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, target);
		float before = heart.getHealth();
		heart.timeUntilRegen = 0;
		heart.damage(world.getDamageSources().playerAttack(dummy), 10.0f);
		float aimed = before - heart.getHealth();
		// a blow on the body away from every weak point: from the side, aimed high
		dummy.refreshPositionAndAngles(heart.getX() + 6, heart.getY() - 1, heart.getZ(), 0, 0);
		dummy.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, heart.getPos().add(0, 9.5, 0));
		before = heart.getHealth();
		heart.timeUntilRegen = 0;
		heart.damage(world.getDamageSources().playerAttack(dummy), 10.0f);
		float body = before - heart.getHealth();
		log("probe stage " + heart.getStage() + " open=" + heart.isOpen() + " weak points " + points.size()
				+ ": aimed hit took " + aimed + ", body hit took " + body);
	}

	private static void finish(MinecraftServer server) {
		phase = 99;
		DUMMIES.clear();
		try {
			OUT.mkdirs();
			try (PrintWriter out = new PrintWriter(new File(OUT, "report.txt"), StandardCharsets.UTF_8)) {
				REPORT.forEach(out::println);
			}
		} catch (IOException e) {
			AmethystMod.LOGGER.error("cannot write selftest report", e);
		}
		server.stop(false);
	}
}
