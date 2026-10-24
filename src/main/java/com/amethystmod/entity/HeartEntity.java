package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import com.amethystmod.item.Fx;
import com.amethystmod.world.AmethystWorld;
import com.amethystmod.world.StoryState;
import com.amethystmod.world.TempleBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Amethyst Heart, final boss, in the temple at the centre of the Amethyst world.
 *
 * <p>It wakes with a short scene. The fight has five stages: five pylon crystals shield it, then
 * four phases, each with its own health bar (growing from phase to phase). Each phase exposes new
 * weak points and the old ones close: the nodes at its roots, then wounds where armour plates fell
 * off, then crystal veins under its belly, and in the last phase its whole body. Weak points are
 * sealed except for a short window after each attack. Attacks are chosen so the same one rarely
 * comes twice and none is forgotten for long; later phases drop the weak attacks and add
 * combinations built from {@link HeartBricks}. Its death is a long scene of the Heart cracking apart.
 */
public class HeartEntity extends BossMob {
	public enum Move {
		NONE, ERUPTION, VOLLEY, SHOCKWAVE, RING_BEAMS, WALLS, SENTINELS, SECTORS, PULL_NOVA, FLASH,
		// built from bricks, see HeartBricks
		PENTAGON, CHASE, SPIRAL, BIG_SPIRAL, DOUBLE_SPIRAL, GROW, RAIN, DEATH_BEAM, LASERS,
		// combinations: several bricks played as one attack
		SPIRAL_CHASE, RAIN_GROW, SPIRAL_PENTAGON, CHASE_PENTAGON, CATACLYSM, LASER_STORM, LASER_WAVE;

		public boolean scripted() {
			return this == SHOCKWAVE || ordinal() >= PENTAGON.ordinal();
		}

		public boolean combo() {
			return ordinal() >= SPIRAL_CHASE.ordinal();
		}
	}

	// animation ids shown by the model (besides the attack moves, which use 100 + ordinal)
	public static final int AN_IDLE = 0;
	public static final int AN_AWAKEN = 50;
	public static final int AN_RAGE = 51;
	public static final int AN_DYING = 52;
	public static final int AN_MOVE = 100;

	private static final TrackedData<Integer> STAGE = DataTracker.registerData(HeartEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** World time when the shells last opened (positive) or closed (negative): lets the client ease the motion. */
	private static final TrackedData<Integer> OPEN_TIME = DataTracker.registerData(HeartEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int[] OPEN_WINDOW = {0, 80, 70, 60, 40};
	/** Health of each phase: when one bar is emptied, the next phase starts with a full one. */
	private static final float[] STAGE_HEALTH = {200, 200, 250, 300, 400};
	/**
	 * Weak points of phases 2 and 3, as offsets from the Heart's feet (phase 1 uses the node entities,
	 * phase 4 the whole body). Phase 2: wounds where the lower armour plates fell off, front and back.
	 * Phase 3: crystal veins hanging under its belly.
	 */
	private static final double[][][] WEAK = {{}, {},
			{{1.6, 4.6, -2.6}, {-1.6, 4.6, -2.6}, {1.6, 4.6, 2.6}, {-1.6, 4.6, 2.6}},
			{{2.4, 2.6, 0}, {-2.4, 2.6, 0}, {0, 2.6, 2.0}, {0, 2.6, -2.0}},
			{}};
	private static final BossBar.Color[] COLORS = {BossBar.Color.BLUE, BossBar.Color.PURPLE, BossBar.Color.PINK, BossBar.Color.RED, BossBar.Color.RED};
	private static final double[][] NODES = {{4.6, 0.9, 0}, {-4.6, 0.9, 0}, {0, 0.9, 4.6}, {0, 0.9, -4.6}};
	static final double ARENA = 30.0;
	private static final int INTRO_TICKS = 140;
	private static final int DEATH_TICKS = 120;

	private BlockPos home;
	private Move move = Move.NONE;
	private int t;
	private int cooldown = 60;
	private int transition;
	private int idle;
	private int hintCooldown;
	private int finalTicks = -1;
	private int intro;
	private int dying;
	private boolean introDone;
	private boolean pylonsSpawned;
	private boolean fromNode;
	private final UUID[] nodeIds = new UUID[NODES.length];
	private final List<Vec3d> spots = new ArrayList<>();
	private final Map<UUID, Integer> lastHit = new HashMap<>();
	private final Set<UUID> waveHit = new HashSet<>();
	private final List<Move> recent = new ArrayList<>();
	private final Map<Move, Integer> sinceUsed = new EnumMap<>(Move.class);
	private double[] lines = new double[0];
	private boolean[] dangerous = new boolean[6];
	private double baseAngle;
	private int spin;
	private final List<BeamEffectEntity> beamFx = new ArrayList<>();
	/** The running combination: when each brick starts, and the bricks running now. */
	private List<Cue> script = List.of();
	private final List<HeartBricks.Brick> running = new ArrayList<>();

	private record Cue(int at, java.util.function.Supplier<HeartBricks.Brick> brick) {
	}

	public HeartEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		experiencePoints = 3000;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 200)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(STAGE, 0);
		builder.add(OPEN_TIME, -1);
	}

	/** 0 while shielded by the pylons, then phases 1-4 (the model glows hotter with each). */
	public int getStage() {
		return dataTracker.get(STAGE);
	}

	private void setStage(int stage) {
		dataTracker.set(STAGE, stage);
		bar.setColor(COLORS[stage]);
		updateBarName();
	}

	/** "The Amethyst Heart: stage N/5" */
	private void updateBarName() {
		bar.setName(Text.translatable("bar.amethystmod.heart.stage", getDisplayName(), getStage() + 1));
	}

	/** Gives the Heart the full health bar of {@code stage}. */
	private void fillHealth(int stage) {
		float health = STAGE_HEALTH[Math.max(1, stage)];
		getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(health);
		setHealth(health);
	}

	/** The weak points of the current phase, in the world (empty in phases 1 and 4). */
	public List<Vec3d> weakPoints() {
		List<Vec3d> list = new ArrayList<>();
		for (double[] w : WEAK[getStage()]) {
			list.add(getPos().add(w[0], w[1], w[2]));
		}
		return list;
	}

	public Move currentMove() {
		return move;
	}

	/** World time the shells opened (> 0) or closed (< 0). */
	public int getOpenTime() {
		return dataTracker.get(OPEN_TIME);
	}

	public boolean isOpen() {
		return getOpenTime() > 0;
	}

	private void setOpen(boolean open) {
		if (open != isOpen()) {
			int now = (int) Math.max(1, getWorld().getTime() % 1_000_000_000L);
			dataTracker.set(OPEN_TIME, open ? now : -now);
		}
	}

	public boolean isVulnerable() {
		return getStage() >= 1 && transition == 0 && dying == 0;
	}

	public void setHome(BlockPos home) {
		this.home = home.toImmutable();
	}

	@Override
	protected void initGoals() {
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return false;
	}

	Vec3d center() {
		return new Vec3d(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
	}

	Vec3d core() {
		return getPos().add(0, 6.5, 0);
	}

	// ---- damage --------------------------------------------------------------------------

	/** Called by a weak-point node: double damage, but only while the shells are open. */
	public boolean hurtFromNode(DamageSource source, float amount) {
		if (getStage() > 1) {
			return false;
		}
		if (!isOpen()) {
			closedHint(source);
			return false;
		}
		fromNode = true;
		try {
			return damage(source, amount * 2.0f);
		} finally {
			fromNode = false;
		}
	}

	private void closedHint(DamageSource source) {
		if (source.getAttacker() instanceof ServerPlayerEntity player && hintCooldown <= 0) {
			hintCooldown = 60;
			player.sendMessage(Text.translatable("message.amethystmod.heart.closed").formatted(Formatting.LIGHT_PURPLE), true);
		}
		if (getWorld() instanceof ServerWorld world) {
			world.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, getSoundCategory(), 1.5f, 1.6f);
		}
	}

	/**
	 * Whether a hit on the body struck a weak point of this phase: a projectile that landed near one,
	 * or a blow whose line of sight passes through one (the body's hitbox is a plain box, so the
	 * aim decides which part of it was struck).
	 */
	private boolean hitsWeakPoint(DamageSource source) {
		Entity direct = source.getSource();
		Entity attacker = source.getAttacker();
		for (Vec3d point : weakPoints()) {
			if (direct != null && direct != attacker) {
				if (direct.getPos().squaredDistanceTo(point) < 2.2 * 2.2) {
					return true;
				}
			} else if (attacker instanceof LivingEntity living) {
				Vec3d eye = living.getEyePos();
				Vec3d look = living.getRotationVec(1.0f);
				Vec3d rel = point.subtract(eye);
				double along = rel.dotProduct(look);
				if (along > 0 && along < 12 && rel.subtract(look.multiply(along)).lengthSquared() < 1.5 * 1.5) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
			return super.damage(source, amount);
		}
		if (source.getAttacker() instanceof CrystalKin) {
			return false;
		}
		if (!isVulnerable()) {
			if (source.getAttacker() instanceof ServerPlayerEntity player && hintCooldown <= 0 && dying == 0) {
				hintCooldown = 60;
				player.sendMessage(Text.translatable(getStage() == 0 ? "message.amethystmod.heart.shielded"
						: "message.amethystmod.heart.transforming").formatted(Formatting.LIGHT_PURPLE), true);
			}
			return false;
		}
		int stage = getStage();
		if (!fromNode && stage < 4) {
			if (stage >= 2 && hitsWeakPoint(source)) {
				if (!isOpen()) {
					closedHint(source);
					return false;
				}
				amount *= 2.0f;
			} else {
				amount *= isOpen() ? 0.4f : 0.1f;
				if (source.getAttacker() instanceof ServerPlayerEntity player && hintCooldown <= 0) {
					hintCooldown = 200;
					String key = stage == 1 ? "nodes" : stage == 2 ? "wounds" : "veins";
					player.sendMessage(Text.translatable(isOpen() ? "message.amethystmod.heart." + key : "message.amethystmod.heart.closed")
							.formatted(Formatting.LIGHT_PURPLE), true);
				}
			}
		}
		// emptying a health bar does not kill: it starts the next phase, or the death scene after the last
		if (getWorld() instanceof ServerWorld world && amount / damageScale() >= getHealth()) {
			boolean hurt = super.damage(source, Math.max(0, getHealth() - 1.0f) * damageScale());
			if (stage < 4) {
				beginTransition(world, stage + 1, fighters(ARENA + 8));
			} else {
				beginDeath(world);
			}
			return hurt;
		}
		return super.damage(source, amount);
	}

	// ---- main loop -----------------------------------------------------------------------

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (home == null) {
			home = getBlockPos();
		}
		hintCooldown--;
		setPosition(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		setVelocity(Vec3d.ZERO);
		setYaw(0);
		bodyYaw = 0;
		headYaw = 0;
		if (age % 20 == 1) {
			if (getStage() <= 1) {
				ensureNodes(world);
			} else {
				removeNodes(world);
			}
		}
		ambience(world);

		if (dying > 0) {
			deathTick(world);
			return;
		}

		List<ServerPlayerEntity> players = fighters(ARENA + 8);
		if (players.isEmpty()) {
			if (++idle > 200 && needsReset()) {
				resetFight(world);
			}
			return;
		}
		idle = 0;

		if (getStage() == 0) {
			if (!introDone) {
				introTick(world, players);
				return;
			}
			if (!pylonsSpawned) {
				spawnPylons(world, players.size());
			}
			List<HeartCrystalEntity> pylons = pylons(world);
			if (age % 12 == 0) {
				for (HeartCrystalEntity pylon : pylons) {
					BossFx.beam(world, this, pylon.getPos().add(0, 2.4, 0), core(), true);
				}
			}
			if (pylons.isEmpty() && transition == 0) {
				beginTransition(world, 1, players);
			}
		}
		if (transition > 0) {
			transitionTick(world);
			return;
		}
		finalCountdown(world, players);

		if (move == Move.NONE) {
			if (--cooldown <= 0) {
				setOpen(false);
				choose(world, players);
			}
			return;
		}
		t++;
		switch (move) {
			case ERUPTION -> eruption(world);
			case VOLLEY -> volley(world, players);
			case RING_BEAMS -> ringBeams(world, players);
			case WALLS -> walls(world, players);
			case SENTINELS -> sentinels(world);
			case SECTORS -> sectors(world, players);
			case PULL_NOVA -> pullNova(world, players);
			case FLASH -> flash(world, players);
			default -> runScript(world, players);
		}
	}

	private void ambience(ServerWorld world) {
		Vec3d core = core();
		int stage = getStage();
		if (age % 8 == 0) {
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, core.x, core.y, core.z, 1 + stage / 2, 4.0, 3.5, 4.0, 0.02);
		}
		if (isOpen() && age % 5 == 0) {
			if (stage <= 1) {
				for (double[] node : NODES) {
					world.spawnParticles(ModParticles.PRISM_FLARE, getX() + node[0], getY() + node[1] + 0.6, getZ() + node[2], 1, 0.3, 0.3, 0.3, 0.01);
				}
			}
			for (Vec3d point : weakPoints()) {
				world.spawnParticles(ModParticles.PRISM_FLARE, point.x, point.y, point.z, 1, 0.3, 0.3, 0.3, 0.01);
			}
		}
		int beat = Math.max(12, 44 - stage * 8);
		if (age % beat == 0) {
			world.playSound(null, core.x, core.y, core.z, SoundEvents.ENTITY_WARDEN_HEARTBEAT, getSoundCategory(), 4.0f, 0.5f + stage * 0.08f);
		}
	}

	private void removeNodes(ServerWorld world) {
		for (int i = 0; i < NODES.length; i++) {
			Entity existing = nodeIds[i] == null ? null : world.getEntity(nodeIds[i]);
			if (existing != null) {
				existing.discard();
			}
			nodeIds[i] = null;
		}
	}

	private void ensureNodes(ServerWorld world) {
		for (int i = 0; i < NODES.length; i++) {
			Entity existing = nodeIds[i] == null ? null : world.getEntity(nodeIds[i]);
			if (existing == null || !existing.isAlive()) {
				HeartNodeEntity node = ModEntities.HEART_NODE.create(world);
				if (node != null) {
					node.attach(this, NODES[i][0], NODES[i][1], NODES[i][2]);
					world.spawnEntity(node);
					nodeIds[i] = node.getUuid();
				}
			}
		}
	}

	// ---- scenes ----------------------------------------------------------------------------

	/** The awakening: the Heart rises out of its slumber, then the pylons light up. */
	private void introTick(ServerWorld world, List<ServerPlayerEntity> players) {
		if (intro == 0) {
			play(AN_AWAKEN, INTRO_TICKS);
			Cutscenes.play(players, this, INTRO_TICKS, Text.translatable("title.amethystmod.heart"),
					Text.translatable("title.amethystmod.heart.sub"));
			Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_EMERGE, 5.0f, 0.5f);
		}
		intro++;
		if (intro % 20 == 0) {
			Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 5.0f, 0.4f + intro / 300.0f);
			Cutscenes.shake(world, center(), 50, 0.4f + intro / 200.0f, 6);
		}
		if (intro == INTRO_TICKS - 20) {
			Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_ROAR, 6.0f, 0.5f);
			Cutscenes.shake(world, center(), 50, 3.0f, 20);
			BossFx.spikeRing(world, center(), 9, 8, 1.6f);
		}
		if (intro >= INTRO_TICKS) {
			introDone = true;
			spawnPylons(world, players.size());
		}
	}

	private void beginDeath(ServerWorld world) {
		if (dying > 0) {
			return;
		}
		dying = DEATH_TICKS;
		move = Move.NONE;
		stopAttack();
		setOpen(true);
		play(AN_DYING, DEATH_TICKS);
		Box arena = new Box(center(), center()).expand(ARENA + 15);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, arena, e -> e instanceof CrystalKin && e != this)) {
			e.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
		}
		Cutscenes.play(fighters(ARENA + 20), this, DEATH_TICKS, Text.translatable("title.amethystmod.heart.death"),
				Text.translatable("title.amethystmod.heart.death.sub"));
		Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_DEATH, 6.0f, 0.4f);
	}

	private void deathTick(ServerWorld world) {
		dying--;
		int elapsed = DEATH_TICKS - dying;
		if (elapsed % 10 == 0) {
			Cutscenes.shake(world, center(), 60, 0.5f + elapsed / 40.0f, 10);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 5.0f, 0.4f + elapsed / 200.0f);
			double a = random.nextDouble() * Math.PI * 2;
			BossFx.spike(world, center().add(Math.cos(a) * (6 + random.nextDouble() * 18), 0, Math.sin(a) * (6 + random.nextDouble() * 18)),
					1.0f + random.nextFloat(), 30);
		}
		if (elapsed % 4 == 0) {
			Fx.particles(world, ModParticles.PRISM_FLARE, core(), 4 + elapsed / 15, 3.0, 0.3);
		}
		if (dying == 0) {
			Fx.particles(world, ModParticles.WARD_MOTE, core(), 200, 6.0, 0.6);
			Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, core(), 3, 3.0, 0.0);
			Cutscenes.shake(world, center(), 70, 5.0f, 30);
			super.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
		}
	}

	// ---- shield, phases, reset -----------------------------------------------------------

	private List<HeartCrystalEntity> pylons(ServerWorld world) {
		return world.getEntitiesByType(ModEntities.HEART_PYLON, new Box(center(), center()).expand(ARENA + 6), Entity::isAlive);
	}

	private void spawnPylons(ServerWorld world, int playerCount) {
		pylonsSpawned = true;
		Vec3d c = center();
		for (int i = 0; i < 5; i++) {
			double a = Math.toRadians(270 + 72 * i);
			HeartCrystalEntity pylon = ModEntities.HEART_PYLON.create(world);
			if (pylon == null) {
				continue;
			}
			pylon.refreshPositionAndAngles(c.x + Math.cos(a) * 21, home.getY() + 1, c.z + Math.sin(a) * 21, 0, 0);
			double health = 70 * (1.0 + 0.6 * (playerCount - 1));
			pylon.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(health);
			pylon.setHealth((float) health);
			pylon.setPersistent();
			world.spawnEntity(pylon);
			BossFx.spike(world, pylon.getPos(), 1.2f, 24);
		}
		broadcast(world, "message.amethystmod.heart.awake");
	}

	private void beginTransition(ServerWorld world, int stage, List<ServerPlayerEntity> players) {
		setStage(stage);
		fillHealth(stage);
		finalTicks = -1;
		transition = 70;
		move = Move.NONE;
		stopAttack();
		setOpen(false);
		if (stage >= 2) {
			removeNodes(world);
			// armour plates break away from its body
			for (int i = 0; i < 6; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				BossFx.spike(world, center().add(Math.cos(a) * (4 + random.nextDouble() * 4), 0, Math.sin(a) * (4 + random.nextDouble() * 4)),
						0.8f + random.nextFloat() * 0.5f, 40);
			}
			Fx.particles(world, new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.crystal(CrystalColor.VIOLET, 2).getDefaultState()),
					core(), 40, 3.5, 0.3);
			Fx.sound(world, this, SoundEvents.BLOCK_GLASS_BREAK, 5.0f, 0.5f);
		}
		play(AN_RAGE, 70);
		Cutscenes.play(players, this, 60, Text.translatable("title.amethystmod.heart.phase" + stage),
				Text.translatable("message.amethystmod.heart.phase" + stage));
		Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_ROAR, 5.0f, 0.5f);
		if (stage == 1) {
			Fx.particles(world, ModParticles.WARD_MOTE, core(), 80, 4.0, 0.4);
			Fx.sound(world, this, SoundEvents.BLOCK_GLASS_BREAK, 5.0f, 0.4f);
		}
	}

	private void transitionTick(ServerWorld world) {
		transition--;
		if (transition % 6 == 0) {
			Fx.particles(world, ModParticles.PRISM_FLARE, core(), 8, 4.0, 0.4);
		}
		if (transition > 0) {
			return;
		}
		Vec3d c = center();
		BossFx.erupt(world, this, c, 9, 8.0f);
		for (LivingEntity e : Fx.around(world, c, 24, BossFx.foesOf(this))) {
			Vec3d away = e.getPos().subtract(c).multiply(1, 0, 1).normalize().multiply(2.0);
			Fx.push(e, away.add(0, 0.7, 0));
		}
		Cutscenes.shake(world, c, 50, 3.0f, 16);
		Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 5.0f, 0.5f);
		switch (getStage()) {
			case 2 -> summon(world, ModEntities.STALKER, 3);
			case 3 -> {
				summon(world, ModEntities.COLOSSUS, 1);
				summon(world, ModEntities.LURKER, 2);
				spawnSentinels(world, 1, false);
			}
			case 4 -> {
				spawnSentinels(world, 2, true);
				summon(world, ModEntities.WALKER, 2);
			}
			default -> {
			}
		}
		play(AN_IDLE, 0);
		cooldown = 30;
	}

	private boolean needsReset() {
		return getStage() > 0 || pylonsSpawned || getHealth() < getMaxHealth();
	}

	/** Everyone in the fight died or fled: the Heart heals, the pylons come back. */
	private void resetFight(ServerWorld world) {
		Box arena = new Box(center(), center()).expand(ARENA + 15);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, arena, e -> e instanceof CrystalKin && e != this)) {
			e.discard();
		}
		removeNodes(world);
		setStage(0);
		fillHealth(1);
		setOpen(false);
		transition = 0;
		move = Move.NONE;
		stopAttack();
		cooldown = 60;
		finalTicks = -1;
		pylonsSpawned = false;
		updateBarName();
		idle = 0;
	}

	private void broadcast(ServerWorld world, String key) {
		Text text = Text.translatable(key).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
		for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(text, false);
		}
	}

	private void actionBar(List<ServerPlayerEntity> players, String key) {
		for (ServerPlayerEntity p : players) {
			p.sendMessage(Text.translatable(key).formatted(Formatting.GOLD, Formatting.BOLD), true);
		}
	}

	// ---- summons -------------------------------------------------------------------------

	private void summon(ServerWorld world, EntityType<? extends AmethystMob> type, int count) {
		Vec3d c = center();
		for (int i = 0; i < count; i++) {
			AmethystMob mob = type.create(world);
			if (mob == null) {
				continue;
			}
			double a = random.nextDouble() * Math.PI * 2;
			double r = 12 + random.nextDouble() * 10;
			BlockPos pos = BlockPos.ofFloored(c.x + Math.cos(a) * r, home.getY(), c.z + Math.sin(a) * r);
			mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
			mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null);
			world.spawnEntityAndPassengers(mob);
			BossFx.spike(world, mob.getPos(), 1.0f, 20);
		}
		Fx.sound(world, this, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 3.0f, 0.5f);
	}

	private void spawnSentinels(ServerWorld world, int count, boolean orbiting) {
		Vec3d c = center();
		int existing = world.getEntitiesByType(ModEntities.HEART_SENTINEL, new Box(c, c).expand(ARENA + 6), Entity::isAlive).size();
		for (int i = 0; i < count && existing + i < 4; i++) {
			HeartCrystalEntity sentinel = ModEntities.HEART_SENTINEL.create(world);
			if (sentinel == null) {
				continue;
			}
			double a = random.nextDouble() * Math.PI * 2;
			double r = 12 + random.nextDouble() * 10;
			sentinel.refreshPositionAndAngles(c.x + Math.cos(a) * r, home.getY() + 3 + random.nextDouble() * 3, c.z + Math.sin(a) * r, 0, 0);
			if (orbiting) {
				sentinel.orbit(this, 180f * i, 11.0f, 5.0f);
			}
			sentinel.setPersistent();
			world.spawnEntity(sentinel);
			Fx.particles(world, ModParticles.PRISM_FLARE, sentinel.getPos(), 10, 0.5, 0.15);
		}
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, 4.0f, 0.5f);
	}

	// ---- choosing moves ------------------------------------------------------------------

	/**
	 * The attacks of each phase. Later phases drop the weaker attacks; the last one has only the
	 * strongest attacks and every combination.
	 */
	private List<Move> pool(int stage) {
		return switch (stage) {
			case 0, 1 -> List.of(Move.ERUPTION, Move.VOLLEY, Move.SHOCKWAVE, Move.PENTAGON, Move.SPIRAL, Move.DEATH_BEAM);
			case 2 -> List.of(Move.ERUPTION, Move.SHOCKWAVE, Move.PENTAGON, Move.RING_BEAMS, Move.WALLS, Move.SENTINELS,
					Move.CHASE, Move.SPIRAL, Move.GROW, Move.RAIN, Move.DEATH_BEAM, Move.SPIRAL_CHASE, Move.CHASE_PENTAGON);
			case 3 -> List.of(Move.SHOCKWAVE, Move.RING_BEAMS, Move.WALLS, Move.SECTORS, Move.PULL_NOVA, Move.SENTINELS,
					Move.CHASE, Move.GROW, Move.RAIN, Move.BIG_SPIRAL, Move.DOUBLE_SPIRAL, Move.DEATH_BEAM,
					Move.SPIRAL_CHASE, Move.RAIN_GROW, Move.SPIRAL_PENTAGON);
			default -> List.of(Move.SECTORS, Move.PULL_NOVA, Move.FLASH, Move.RING_BEAMS, Move.SHOCKWAVE, Move.BIG_SPIRAL,
					Move.DOUBLE_SPIRAL, Move.DEATH_BEAM, Move.LASERS, Move.SPIRAL_CHASE, Move.RAIN_GROW, Move.SPIRAL_PENTAGON,
					Move.CHASE_PENTAGON, Move.CATACLYSM, Move.LASER_STORM, Move.LASER_WAVE);
		};
	}

	/**
	 * Weighted choice: the last two moves are (almost) excluded, and a move's weight grows with every
	 * turn it has not been used, so nothing repeats but nothing runs in a fixed circle either.
	 */
	private Move pick(int stage) {
		List<Move> pool = pool(stage);
		double total = 0;
		double[] weights = new double[pool.size()];
		for (int i = 0; i < pool.size(); i++) {
			Move m = pool.get(i);
			double w = 1.0 + 0.5 * sinceUsed.getOrDefault(m, 3);
			if (!recent.isEmpty() && recent.get(recent.size() - 1) == m) {
				w = 0;
			} else if (recent.size() > 1 && recent.get(recent.size() - 2) == m) {
				w *= 0.15;
			}
			if (m == Move.SENTINELS && spotsOfSentinels() >= 3) {
				w = 0;
			}
			if (m == Move.DEATH_BEAM) {
				w *= 0.85;
			}
			if (m.combo() && stage >= 4) {
				w *= 1.6;
			}
			weights[i] = w;
			total += w;
		}
		double roll = random.nextDouble() * total;
		Move chosen = pool.get(0);
		for (int i = 0; i < pool.size(); i++) {
			roll -= weights[i];
			if (roll <= 0) {
				chosen = pool.get(i);
				break;
			}
		}
		for (Move m : pool) {
			sinceUsed.merge(m, 1, Integer::sum);
		}
		sinceUsed.put(chosen, 0);
		recent.add(chosen);
		if (recent.size() > 3) {
			recent.remove(0);
		}
		return chosen;
	}

	private int spotsOfSentinels() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return 0;
		}
		return world.getEntitiesByType(ModEntities.HEART_SENTINEL, getBoundingBox().expand(ARENA + 6), Entity::isAlive).size();
	}

	private static final Map<Move, Integer> DURATION = new EnumMap<>(Map.ofEntries(
			Map.entry(Move.ERUPTION, 44), Map.entry(Move.VOLLEY, 56), Map.entry(Move.SHOCKWAVE, 96),
			Map.entry(Move.RING_BEAMS, 124), Map.entry(Move.WALLS, 40), Map.entry(Move.SENTINELS, 24),
			Map.entry(Move.SECTORS, 52), Map.entry(Move.PULL_NOVA, 48), Map.entry(Move.FLASH, 56),
			Map.entry(Move.PENTAGON, 74), Map.entry(Move.CHASE, 140), Map.entry(Move.SPIRAL, 110),
			Map.entry(Move.BIG_SPIRAL, 100), Map.entry(Move.DOUBLE_SPIRAL, 110), Map.entry(Move.GROW, 160),
			Map.entry(Move.RAIN, 160), Map.entry(Move.DEATH_BEAM, 72), Map.entry(Move.SPIRAL_CHASE, 130),
			Map.entry(Move.RAIN_GROW, 165), Map.entry(Move.SPIRAL_PENTAGON, 120), Map.entry(Move.CHASE_PENTAGON, 110),
			Map.entry(Move.CATACLYSM, 140), Map.entry(Move.LASERS, 118), Map.entry(Move.LASER_STORM, 128),
			Map.entry(Move.LASER_WAVE, 134)));

	/** Self-test only: attacks to play next, in order, instead of choosing. */
	public static final java.util.ArrayDeque<Move> FORCED = new java.util.ArrayDeque<>();

	private void choose(ServerWorld world, List<ServerPlayerEntity> players) {
		int stage = getStage();
		Move chosen = FORCED.isEmpty() ? pick(stage) : FORCED.poll();
		move = chosen;
		t = 0;
		spots.clear();
		waveHit.clear();
		lastHit.clear();
		baseAngle = random.nextDouble() * Math.PI * 2;
		spin = random.nextBoolean() ? 1 : -1;
		play(AN_MOVE + chosen.ordinal(), DURATION.getOrDefault(chosen, 50));
		if (chosen.scripted()) {
			script = script(chosen, players);
			running.clear();
			if (chosen == Move.CHASE || chosen == Move.SPIRAL_CHASE || chosen == Move.CHASE_PENTAGON) {
				actionBar(players, "message.amethystmod.heart.hint.run");
			} else if (chosen == Move.SHOCKWAVE || chosen == Move.LASER_WAVE) {
				actionBar(players, "message.amethystmod.heart.hint.wave");
			}
			return;
		}
		switch (chosen) {
			case ERUPTION -> {
				int extra = stage <= 2 ? 2 : stage == 3 ? 3 : 4;
				for (ServerPlayerEntity p : players) {
					spots.add(p.getPos());
					for (int i = 0; i < extra; i++) {
						spots.add(p.getPos().add(random.nextDouble() * 10 - 5, 0, random.nextDouble() * 10 - 5));
					}
				}
				for (Vec3d spot : spots) {
					BossFx.marker(world, spot, 2.6, 36);
				}
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 4.0f, 0.4f);
			}
			case VOLLEY -> Fx.sound(world, this, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 4.0f, 0.6f);
			case RING_BEAMS -> {
				actionBar(players, "message.amethystmod.heart.hint.jump");
				Fx.sound(world, this, SoundEvents.BLOCK_BEACON_ACTIVATE, 4.0f, 0.5f);
			}
			case WALLS -> {
				int n = stage == 2 ? 3 : stage == 3 ? 4 : 6;
				lines = new double[n];
				for (int i = 0; i < n; i++) {
					lines[i] = baseAngle + i * (Math.PI * 2 / n) + random.nextDouble() * 0.3;
				}
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 4.0f, 0.5f);
			}
			case SECTORS -> {
				int count = stage >= 4 ? 4 : 3;
				dangerous = new boolean[6];
				int start = random.nextInt(6);
				for (int i = 0; i < count; i++) {
					dangerous[(start + i * 2 + (i >= 3 ? 1 : 0)) % 6] = true;
				}
				actionBar(players, "message.amethystmod.heart.hint.sectors");
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 4.0f, 0.4f);
			}
			case PULL_NOVA -> {
				actionBar(players, "message.amethystmod.heart.hint.pull");
				Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 4.0f, 0.5f);
			}
			case FLASH -> {
				actionBar(players, "message.amethystmod.heart.hint.flash");
				Fx.sound(world, this, SoundEvents.BLOCK_BEACON_ACTIVATE, 4.0f, 1.6f);
			}
			default -> {
			}
		}
	}

	/** After an attack the Heart opens up: the nodes can be hit until the next attack. */
	private void finish() {
		move = Move.NONE;
		int stage = getStage();
		if (stage >= 1) {
			cooldown = OPEN_WINDOW[stage];
			setOpen(true);
			play(AN_IDLE, 0);
			if (getWorld() instanceof ServerWorld world) {
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 3.0f, 1.4f);
			}
		} else {
			cooldown = 90;
			play(AN_IDLE, 0);
		}
	}

	private boolean canHit(ServerPlayerEntity p, int every) {
		Integer at = lastHit.get(p.getUuid());
		if (at != null && age - at < every) {
			return false;
		}
		lastHit.put(p.getUuid(), age);
		return true;
	}

	void hurt(ServerWorld world, LivingEntity target, float damage) {
		target.damage(world.getDamageSources().indirectMagic(this, this), damage);
		AmethystMob.infect(target, 160);
	}

	// ---- moves ---------------------------------------------------------------------------

	private static Cue cue(int at, java.util.function.Supplier<HeartBricks.Brick> brick) {
		return new Cue(at, brick);
	}

	/** A spiral out from the Heart and back in between its own arms. */
	private HeartBricks.Brick spiral(int arms, boolean big, double angle) {
		return new HeartBricks.Spiral(this, arms, big, List.of(new HeartBricks.Pass(true, spin, angle),
				new HeartBricks.Pass(false, spin, angle + Math.PI / arms)));
	}

	/** Two spirals at once, one going out and one coming in; then each goes back along another path. */
	private List<Cue> doubleSpiral(int arms, double angle, int at) {
		double half = Math.PI / arms;
		return List.of(
				cue(at, () -> new HeartBricks.Spiral(this, arms, false, List.of(new HeartBricks.Pass(true, spin, angle),
						new HeartBricks.Pass(false, -spin, angle + half)))),
				cue(at, () -> new HeartBricks.Spiral(this, arms, false, List.of(new HeartBricks.Pass(false, -spin, angle + half / 2),
						new HeartBricks.Pass(true, spin, angle + half * 1.5)))));
	}

	/** What a brick-built attack does and when; a combination starts several bricks as one attack. */
	private List<Cue> script(Move m, List<ServerPlayerEntity> players) {
		int s = getStage();
		int arms = s >= 4 ? 6 : s == 3 ? 5 : 4;
		int chaseEvery = s >= 4 ? 5 : 6;
		double b = baseAngle;
		// crystals per tick for the growing and falling zones: about 32-48 a second
		double density = s >= 4 ? 2.4 : s == 3 ? 2.0 : 1.6;
		List<ServerPlayerEntity> targets = List.copyOf(players);
		return switch (m) {
			case SHOCKWAVE -> {
				// one to three waves in a row, more likely more in later phases
				int waves = Math.min(3, 1 + random.nextInt(2) + (s >= 3 ? 1 : 0) + (s == 2 && random.nextInt(3) == 0 ? 1 : 0));
				yield List.of(cue(0, () -> new HeartBricks.Waves(this, waves)));
			}
			case PENTAGON -> List.of(cue(0, () -> new HeartBricks.Pentagon(this, s >= 3)));
			case CHASE -> List.of(cue(0, () -> new HeartBricks.Chase(this, 120, chaseEvery)));
			case SPIRAL -> List.of(cue(0, () -> spiral(arms, false, b)));
			case BIG_SPIRAL -> List.of(cue(0, () -> spiral(s >= 4 ? 4 : 3, true, b)));
			case DOUBLE_SPIRAL -> doubleSpiral(arms - 1, b, 0);
			case GROW -> List.of(cue(0, () -> new HeartBricks.Grow(this, 140, density)));
			case RAIN -> List.of(cue(0, () -> new HeartBricks.Rain(this, 140, density)));
			case DEATH_BEAM -> List.of(cue(0, () -> new HeartBricks.DeathBeam(this, targets, s >= 4 ? 2 : 1, 0.5)));
			case LASERS -> List.of(cue(0, () -> new HeartBricks.Lasers(this, 5, 90)));
			case LASER_STORM -> List.of(cue(0, () -> new HeartBricks.Lasers(this, 4, 100)),
					cue(14, () -> new HeartBricks.DeathBeam(this, targets, 1, 0.5)));
			case LASER_WAVE -> List.of(cue(0, () -> new HeartBricks.Lasers(this, 4, 110)),
					cue(10, () -> new HeartBricks.Waves(this, 2 + random.nextInt(2))));
			case SPIRAL_CHASE -> List.of(cue(0, () -> spiral(arms - 1, false, b)),
					cue(24, () -> new HeartBricks.Chase(this, 90, chaseEvery + 1)));
			case RAIN_GROW -> List.of(cue(0, () -> new HeartBricks.Rain(this, 140, density * 0.55)),
					cue(8, () -> new HeartBricks.Grow(this, 130, density * 0.55)));
			case SPIRAL_PENTAGON -> List.of(cue(0, () -> new HeartBricks.Pentagon(this, false)),
					cue(10, () -> spiral(arms - 1, false, b)));
			case CHASE_PENTAGON -> List.of(cue(0, () -> new HeartBricks.Pentagon(this, s >= 4)),
					cue(6, () -> new HeartBricks.Chase(this, 90, chaseEvery + 1)));
			case CATACLYSM -> {
				List<Cue> list = new ArrayList<>(doubleSpiral(arms - 2, b, 0));
				list.add(cue(14, () -> new HeartBricks.DeathBeam(this, targets, 1, 0.5)));
				list.add(cue(30, () -> new HeartBricks.Grow(this, 100, density * 0.35)));
				yield list;
			}
			default -> List.of();
		};
	}

	/** Starts each brick at its time and runs the bricks; the attack ends when all are done. */
	private void runScript(ServerWorld world, List<ServerPlayerEntity> players) {
		int last = 0;
		for (Cue c : script) {
			last = Math.max(last, c.at());
			if (c.at() == t - 1) {
				running.add(c.brick().get());
			}
		}
		running.removeIf(brick -> {
			boolean done = brick.tick(world, players);
			if (done) {
				brick.stop();
			}
			return done;
		});
		if (running.isEmpty() && t - 1 >= last) {
			script = List.of();
			finish();
		}
	}

	/** Cuts the current attack short: its beams and bricks disappear. */
	private void stopAttack() {
		running.forEach(HeartBricks.Brick::stop);
		running.clear();
		script = List.of();
		beamFx.forEach(Entity::discard);
		beamFx.clear();
	}

	private void eruption(ServerWorld world) {
		if (t == 36) {
			for (Vec3d spot : spots) {
				BossFx.erupt(world, this, spot, 2.6, 12.0f + getStage() * 2);
			}
			Cutscenes.shake(world, center(), 50, 1.5f, 8);
		}
		if (t >= 44) {
			finish();
		}
	}

	private void volley(ServerWorld world, List<ServerPlayerEntity> players) {
		int stage = getStage();
		int interval = stage >= 4 ? 2 : stage == 3 ? 3 : 4;
		Vec3d core = core();
		if (t % interval == 0 && t > 8 && t < 48) {
			for (ServerPlayerEntity p : players) {
				fire(world, core, p.getEyePos().subtract(core), 7.0f, 7.0f + stage);
			}
			if (stage >= 3) {
				for (int i = 0; i < (stage == 4 ? 5 : 3); i++) {
					fire(world, core, new Vec3d(random.nextDouble() - 0.5, random.nextDouble() * 0.3 - 0.45, random.nextDouble() - 0.5), 14.0f, 8.0f);
				}
			}
			Fx.sound(world, this, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 1.4f);
		}
		if (t >= 56) {
			finish();
		}
	}

	private void fire(ServerWorld world, Vec3d origin, Vec3d aim, float spread, float damage) {
		CrystalShardEntity shard = new CrystalShardEntity(world, this, damage);
		shard.setPosition(origin.x, origin.y, origin.z);
		shard.setVelocity(aim.x, aim.y, aim.z, 1.1f, spread);
		world.spawnEntity(shard);
	}

	/** Beams that sweep around the floor like the hands of a clock: jump over them. */
	private void ringBeams(ServerWorld world, List<ServerPlayerEntity> players) {
		int stage = getStage();
		int n = stage <= 2 ? 2 : stage == 3 ? 3 : 4;
		double speed = stage >= 4 ? 0.05 : 0.035;
		Vec3d c = center();
		if (t == 1) {
			for (int b = 0; b < n; b++) {
				BeamEffectEntity sweep = BeamEffectEntity.sweep(world, c, 3.5f, (float) ARENA, SWEEP_HALF_WIDTH,
						(float) (baseAngle + b * Math.PI * 2 / n), (float) (speed * spin), 20, 124);
				if (sweep != null) {
					beamFx.add(sweep);
				}
			}
		}
		boolean active = t > 20;
		if (active) {
			for (ServerPlayerEntity p : players) {
				double dx = p.getX() - c.x;
				double dz = p.getZ() - c.z;
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r < 3 || r > ARENA + 0.5 || p.getY() > c.y + SWEEP_HIT_HEIGHT) {
					continue;
				}
				for (BeamEffectEntity sweep : beamFx) {
					double a = sweep.sweepAngle(0);
					double along = dx * Math.cos(a) + dz * Math.sin(a);
					double perp = Math.abs(dx * Math.sin(a) - dz * Math.cos(a));
					if (along > 0 && perp < SWEEP_HALF_WIDTH + 0.3 && canHit(p, 12)) {
						hurt(world, p, 9.0f + stage);
						Fx.push(p, new Vec3d(-Math.sin(a) * spin, 0.4, Math.cos(a) * spin).multiply(0.8));
					}
				}
			}
			if (t % 20 == 0) {
				Fx.sound(world, this, SoundEvents.BLOCK_BEACON_AMBIENT, 3.0f, 1.2f);
			}
		}
		if (t >= 124) {
			beamFx.clear();
			finish();
		}
	}

	private static final float SWEEP_HALF_WIDTH = 0.5f;
	private static final float SWEEP_HIT_HEIGHT = 0.9f;

	/** Crystal walls burst out of the floor along straight lines from the Heart. */
	private void walls(ServerWorld world, List<ServerPlayerEntity> players) {
		Vec3d c = center();
		if (t == 1) {
			for (double a : lines) {
				for (double r = 6; r <= ARENA; r += 6) {
					BossFx.marker(world, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1.6, 30);
				}
			}
		}
		if (t == 30) {
			for (double a : lines) {
				for (double r = 4; r <= ARENA; r += 2.2) {
					BossFx.spike(world, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1.1f + random.nextFloat() * 0.6f, 26);
				}
			}
			for (ServerPlayerEntity p : players) {
				double dx = p.getX() - c.x;
				double dz = p.getZ() - c.z;
				for (double a : lines) {
					double along = dx * Math.cos(a) + dz * Math.sin(a);
					double perp = Math.abs(dx * Math.sin(a) - dz * Math.cos(a));
					if (along > 2.5 && perp < 1.6 && canHit(p, 10)) {
						hurt(world, p, 12.0f + 2 * getStage());
						Fx.push(p, new Vec3d(0, 0.85, 0));
					}
				}
			}
			Cutscenes.shake(world, c, 50, 1.8f, 8);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 5.0f, 0.5f);
		}
		if (t >= 40) {
			finish();
		}
	}

	private void sentinels(ServerWorld world) {
		if (t == 10) {
			spawnSentinels(world, 2, false);
		}
		if (t >= 24) {
			finish();
		}
	}

	/** The arena is cut into six slices; the marked ones crystallize. Stand in a pale one. */
	private void sectors(ServerWorld world, List<ServerPlayerEntity> players) {
		Vec3d c = center();
		if (t == 1) {
			for (int s = 0; s < 6; s++) {
				if (!dangerous[s]) {
					continue;
				}
				double mid = baseAngle + (s + 0.5) * Math.PI / 3;
				for (double r = 8; r < ARENA; r += 8) {
					BossFx.marker(world, c.add(Math.cos(mid) * r, 0, Math.sin(mid) * r), 2.2 + r / 10, 44);
				}
			}
		}
		if (t < 44 && t % 6 == 0) {
			for (int s = 0; s < 6; s++) {
				for (int i = 0; i < 6; i++) {
					double a = baseAngle + (s + random.nextDouble()) * Math.PI / 3;
					double r = 4 + random.nextDouble() * (ARENA - 4);
					world.spawnParticles(dangerous[s] ? ModParticles.PRISM_FLARE : ModParticles.WARD_MOTE,
							c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
				}
			}
		}
		if (t == 44) {
			for (int s = 0; s < 6; s++) {
				if (!dangerous[s]) {
					continue;
				}
				for (int i = 0; i < 9; i++) {
					double a = baseAngle + (s + random.nextDouble()) * Math.PI / 3;
					double r = 5 + random.nextDouble() * (ARENA - 6);
					BossFx.spike(world, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1.0f + random.nextFloat(), 28);
				}
			}
			for (ServerPlayerEntity p : players) {
				double dx = p.getX() - c.x;
				double dz = p.getZ() - c.z;
				double r = Math.sqrt(dx * dx + dz * dz);
				double a = Math.atan2(dz, dx) - baseAngle;
				int s = (int) Math.floor(((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2) / (Math.PI / 3)) % 6;
				if (dangerous[s] && r > 3.5 && r < ARENA + 0.5) {
					hurt(world, p, 15.0f + getStage());
					Fx.push(p, new Vec3d(0, 0.9, 0));
				}
			}
			Cutscenes.shake(world, c, 50, 2.0f, 10);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 5.0f, 0.4f);
		}
		if (t >= 52) {
			finish();
		}
	}

	/** Everyone is dragged toward the Heart, then it bursts: run away from it. */
	private void pullNova(ServerWorld world, List<ServerPlayerEntity> players) {
		Vec3d c = center();
		if (t == 1) {
			BossFx.marker(world, c, 8.5, 40);
		}
		if (t < 40) {
			for (ServerPlayerEntity p : players) {
				Vec3d pull = c.subtract(p.getPos()).multiply(1, 0, 1);
				if (pull.lengthSquared() > 1) {
					Vec3d add = pull.normalize().multiply(0.11);
					p.addVelocity(add.x, 0, add.z);
					p.velocityModified = true;
				}
			}
			if (t % 2 == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				Vec3d from = c.add(Math.cos(a) * 20, 1 + random.nextDouble() * 3, Math.sin(a) * 20);
				Fx.shoot(world, ModParticles.PRISM_FLARE, from, c.add(0, 2, 0).subtract(from).normalize(), 0.9);
			}
		}
		if (t == 40) {
			BossFx.spikeRing(world, c, 7, 10, 1.5f);
			for (ServerPlayerEntity p : players) {
				Vec3d away = p.getPos().subtract(c).multiply(1, 0, 1);
				if (away.length() <= 8.5) {
					hurt(world, p, 18.0f + getStage());
					Fx.push(p, (away.lengthSquared() < 0.01 ? new Vec3d(1, 0, 0) : away.normalize()).multiply(1.7).add(0, 0.5, 0));
				}
			}
			Cutscenes.shake(world, c, 50, 3.0f, 12);
			Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 5.0f, 0.6f);
		}
		if (t >= 48) {
			finish();
		}
	}

	/** A blinding flash: whoever looks at the Heart is blinded, and the floor erupts under them. */
	private void flash(ServerWorld world, List<ServerPlayerEntity> players) {
		Vec3d core = core();
		if (t < 30 && t % 3 == 0) {
			Vec3d from = core.add(random.nextGaussian() * 6, random.nextGaussian() * 6, random.nextGaussian() * 6);
			Fx.shoot(world, ModParticles.WARD_MOTE, from, core.subtract(from).normalize(), 0.5);
		}
		if (t == 30) {
			Fx.particles(world, ModParticles.WARD_MOTE, core, 80, 8.0, 0.6);
			Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 3.0f, 1.8f);
			for (ServerPlayerEntity p : players) {
				Vec3d toHeart = core.subtract(p.getEyePos()).normalize();
				if (p.getRotationVec(1.0f).dotProduct(toHeart) > 0.25) {
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
					spots.add(p.getPos());
					BossFx.marker(world, p.getPos(), 2.4, 20);
				}
			}
		}
		if (t == 50) {
			for (Vec3d spot : spots) {
				BossFx.erupt(world, this, spot, 2.4, 16.0f);
			}
		}
		if (t >= 56) {
			finish();
		}
	}

	/** Below 8% in the last phase: 30 seconds to finish it, or the whole arena is blasted. */
	private void finalCountdown(ServerWorld world, List<ServerPlayerEntity> players) {
		if (getStage() < 4) {
			return;
		}
		if (finalTicks < 0) {
			if (getHealth() / getMaxHealth() < 0.08f) {
				finalTicks = 600;
				broadcast(world, "message.amethystmod.heart.final");
			}
			return;
		}
		finalTicks--;
		if (finalTicks % 20 == 0) {
			bar.setName(Text.translatable("bar.amethystmod.heart.countdown", finalTicks / 20).formatted(Formatting.RED));
			Fx.sound(world, this, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 3.0f, 0.5f + (600 - finalTicks) / 600.0f);
		}
		if (finalTicks % 4 == 0) {
			Fx.particles(world, ModParticles.PRISM_FLARE, core(), 1 + (600 - finalTicks) / 80, 4.0, 0.3);
		}
		if (finalTicks == 0) {
			for (ServerPlayerEntity p : players) {
				hurt(world, p, 45.0f);
				Vec3d away = p.getPos().subtract(center()).multiply(1, 0, 1);
				Fx.push(p, (away.lengthSquared() < 0.01 ? new Vec3d(1, 0, 0) : away.normalize()).multiply(2.0).add(0, 0.8, 0));
			}
			Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, core(), 3, 6.0, 0.0);
			Cutscenes.shake(world, center(), 60, 4.0f, 25);
			Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 6.0f, 0.4f);
			heal(getMaxHealth() * 0.12f);
			updateBarName();
			finalTicks = -1;
		}
	}

	// ---- death ---------------------------------------------------------------------------

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d core = core();
		Fx.particles(world, ModParticles.PRISM_FLARE, core, 120, 5.0, 0.6);
		Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 6.0f, 0.5f);
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 6.0f, 0.3f);
		BossFx.spikeRing(world, center(), 12, 14, 2.0f);
		if (world.getRegistryKey() == AmethystWorld.WORLD) {
			RiftManager.openTempleRift(world);
		}
		StoryState.markHeartDefeated(world.getServer());
		Text text = Text.translatable("message.amethystmod.heart.defeated").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
		for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
			p.sendMessage(text, false);
		}
	}

	// ---- saving --------------------------------------------------------------------------

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (home != null) {
			nbt.putInt("HomeX", home.getX());
			nbt.putInt("HomeY", home.getY());
			nbt.putInt("HomeZ", home.getZ());
		}
		nbt.putInt("Stage", getStage());
		nbt.putBoolean("PylonsSpawned", pylonsSpawned);
		nbt.putBoolean("IntroDone", introDone);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("HomeX")) {
			home = new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ"));
		}
		setStage(MathHelper.clamp(nbt.getInt("Stage"), 0, 4));
		if (getMaxHealth() > STAGE_HEALTH[4] + 1) {
			// a Heart from before the phases had their own health bars
			fillHealth(Math.max(1, getStage()));
		}
		pylonsSpawned = nbt.getBoolean("PylonsSpawned");
		introDone = nbt.getBoolean("IntroDone");
	}
}
