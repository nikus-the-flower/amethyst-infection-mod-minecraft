package com.amethystmod.entity;

import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Amitariel, apostle of the Amethyst. Greets those who climb the tower, offers them to become
 * followers (answer "yes" in chat and you die; "no", or silence, starts the fight) and then
 * fights in two phases. Phase 1: lunges, shard barrages, crystal pillars, guards. Phase 2
 * (below half health): he transforms and adds a shard storm and blink strikes.
 */
public class AmitarielEntity extends BossMob {
	private enum State {
		DORMANT, TALKING, ASKING, FIGHT, TRANSFORM
	}

	private static final TrackedData<Integer> PHASE = DataTracker.registerData(AmitarielEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final Identifier ENRAGE_ID = Identifier.of("amethystmod", "amitariel_enrage");
	private static final String[] SPEECH = {
			"message.amethystmod.amitariel.line1", "message.amethystmod.amitariel.line2",
			"message.amethystmod.amitariel.line3", "message.amethystmod.amitariel.line4",
			"message.amethystmod.amitariel.question"};
	private static final Set<String> YES = Set.of("да", "yes", "y", "д", "ага", "согласен", "принимаю", "конечно", "так", "згоден", "приймаю");
	private static final Set<String> NO = Set.of("нет", "no", "n", "н", "не", "никогда", "неа", "ні", "ніколи");

	private State state = State.DORMANT;
	private int stateTicks;
	private int line;
	private int askTicks;
	private int lungeTicks;
	private boolean lungeHit;
	private boolean summoned60;
	private boolean summoned30;
	private int dying;
	/** A move that plays out after its wind-up: the whirl or the leap. While it runs nothing else does. */
	private static final int ACT_NONE = 0;
	private static final int ACT_SPIN = 1;
	private static final int ACT_LEAP = 2;
	private int action;
	private int actionTicks;
	private Vec3d leapSpot = Vec3d.ZERO;
	private static final int DEATH_TICKS = 80;

	public AmitarielEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 450)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 16)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PHASE, 1);
	}

	/** 1 before the transformation, 2 after (the model switches texture). */
	public int getPhase() {
		return dataTracker.get(PHASE);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new Busy());
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new Blink(this));
		goalSelector.add(2, new Lunge(this));
		goalSelector.add(2, new Pillars(this));
		goalSelector.add(2, new Storm(this));
		goalSelector.add(2, new Whirl(this));
		goalSelector.add(2, new Leap(this));
		goalSelector.add(2, new Spears(this));
		goalSelector.add(3, new AbilityGoals.ShootShards(this, 5, 28, 26, 160, 5, 9.0f, 8.0f, 1.3f) {
			@Override
			protected boolean inRange(LivingEntity target, double distanceSq) {
				return !busy() && super.inRange(target, distanceSq);
			}
		});
		goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
		initIdle();
		initTargets();
	}

	// ---- no fighting before the conversation is over ---------------------------------

	@Override
	public void setTarget(LivingEntity target) {
		super.setTarget(state == State.FIGHT ? target : null);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (dying > 0 && !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) {
			return false;
		}
		if (state == State.TRANSFORM) {
			return false;
		}
		// the killing blow starts his death scene instead
		if (getWorld() instanceof ServerWorld world && dying == 0
				&& !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)
				&& amount / damageScale() >= getHealth()) {
			boolean hurt = super.damage(source, Math.max(0, (getHealth() - 1.0f) * damageScale()));
			beginDeath(world);
			return hurt;
		}
		if (state != State.FIGHT && source.getAttacker() instanceof ServerPlayerEntity) {
			startFight("message.amethystmod.amitariel.attacked");
		}
		return super.damage(source, amount);
	}

	private void say(ServerWorld world, String key) {
		Text message = Text.empty()
				.append(Text.translatable("entity.amethystmod.amitariel.short").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
				.append(Text.literal(": ").formatted(Formatting.GRAY))
				.append(Text.translatable(key).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
		for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(this) < 64 * 64)) {
			p.sendMessage(message, false);
		}
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.5f);
		Fx.particles(world, ModParticles.CRYSTAL_SPARK, getPos().add(0, 2.2, 0), 14, 0.5, 0.05);
	}

	private void startFight(String key) {
		if (getWorld() instanceof ServerWorld world) {
			say(world, key);
			Fx.sound(world, this, SoundEvents.ENTITY_WITHER_SPAWN, 1.5f, 1.3f);
		}
		state = State.FIGHT;
		stateTicks = 0;
		for (ServerPlayerEntity p : fighters(30)) {
			setTarget(p);
			break;
		}
	}

	/** Called for every chat message sent near the boss while he waits for an answer. */
	public void onAnswer(ServerPlayerEntity player, String raw) {
		if (state != State.ASKING || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		String text = raw.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
		if (YES.contains(text)) {
			say(world, "message.amethystmod.amitariel.yes");
			Fx.particles(world, ModParticles.PRISM_FLARE, player.getPos().add(0, 1, 0), 60, 0.4, 0.4);
			player.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
			if (fighters(30).isEmpty()) {
				state = State.DORMANT;
				stateTicks = 0;
			}
		} else if (NO.contains(text)) {
			startFight("message.amethystmod.amitariel.no");
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (age % 16 == 0) {
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, getX(), getY() + 1.5, getZ(), 1, 0.5, 1.0, 0.5, 0.01);
		}
		if (dying > 0) {
			deathTick(world);
			return;
		}

		switch (state) {
			case DORMANT -> {
				getNavigation().stop();
				if (age % 10 == 0) {
					if (fighters(12).stream().anyMatch(p -> Math.abs(p.getY() - getY()) < 3 && canSee(p))) {
						state = State.TALKING;
						stateTicks = 0;
						line = 0;
						play(ANIM_SPREAD, 100);
						Cutscenes.play(fighters(30), this, 100, Text.translatable("title.amethystmod.amitariel"),
								Text.translatable("title.amethystmod.amitariel.sub"));
						Fx.sound(world, this, SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0f, 0.6f);
					}
				}
			}
			case TALKING -> {
				getNavigation().stop();
				faceNearestPlayer();
				if (stateTicks % 75 == 0) {
					say(world, SPEECH[line]);
					if (++line >= SPEECH.length) {
						state = State.ASKING;
						askTicks = 20 * 40;
					}
				}
				stateTicks++;
			}
			case ASKING -> {
				getNavigation().stop();
				faceNearestPlayer();
				List<ServerPlayerEntity> near = fighters(40);
				if (near.isEmpty()) {
					state = State.DORMANT;
				} else if (--askTicks <= 0) {
					startFight("message.amethystmod.amitariel.timeout");
				}
			}
			case TRANSFORM -> transformTick(world);
			case FIGHT -> fightTick(world);
		}
	}

	private void faceNearestPlayer() {
		ServerPlayerEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayerEntity p : fighters(40)) {
			double d = p.squaredDistanceTo(this);
			if (d < best) {
				best = d;
				nearest = p;
			}
		}
		if (nearest != null) {
			getLookControl().lookAt(nearest, 30.0f, 30.0f);
		}
	}

	private void fightTick(ServerWorld world) {
		float health = getHealth() / getMaxHealth();
		if (getPhase() == 1 && health < 0.5f) {
			action = ACT_NONE;
			state = State.TRANSFORM;
			stateTicks = 0;
			setTarget(null);
			return;
		}
		if (!summoned60 && health < 0.8f) {
			summoned60 = true;
			summonGuards(world, ModEntities.WEAVER, 2);
		}
		if (!summoned30 && health < 0.3f) {
			summoned30 = true;
			summonGuards(world, ModEntities.WALKER, 1);
		}
		if (fighters(50).isEmpty() && age % 100 == 0) {
			heal(10.0f);
		}
		if (action == ACT_SPIN) {
			spinTick(world);
		} else if (action == ACT_LEAP) {
			leapTick(world);
		}

		if (lungeTicks > 0) {
			lungeTicks--;
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, getX(), getY() + 1, getZ(), 2, 0.3, 0.5, 0.3, 0.05);
			if (!lungeHit) {
				for (LivingEntity e : Fx.around(world, getPos().add(0, 1, 0), 2.4, BossFx.foesOf(this))) {
					lungeHit = true;
					e.damage(world.getDamageSources().mobAttack(this), 14.0f);
					Fx.push(e, getVelocity().multiply(0.8).add(0, 0.5, 0));
					AmethystMob.infect(e, 200);
				}
			}
		}
	}

	/** The transformation: he stops, shouts, and blasts everyone away. */
	private void transformTick(ServerWorld world) {
		getNavigation().stop();
		setVelocity(0, getVelocity().y, 0);
		if (stateTicks == 0) {
			say(world, "message.amethystmod.amitariel.phase2");
			Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_ROAR, 3.0f, 0.7f);
			play(ANIM_SPREAD, 72);
			Cutscenes.play(fighters(40), this, 70, Text.translatable("title.amethystmod.amitariel.phase2"),
					Text.translatable("message.amethystmod.amitariel.phase2"));
		}
		if (stateTicks % 10 == 0) {
			Cutscenes.shake(world, getPos(), 40, 0.3f + stateTicks / 70.0f, 8);
		}
		if (stateTicks % 4 == 0) {
			Fx.particles(world, ModParticles.PRISM_FLARE, getPos().add(0, 1.5, 0), 4, 1.2, 0.3);
		}
		if (stateTicks == 70) {
			dataTracker.set(PHASE, 2);
			bar.setColor(BossBar.Color.RED);
			EntityAttributeInstance speed = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			speed.addPersistentModifier(new EntityAttributeModifier(ENRAGE_ID, 0.15, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			EntityAttributeInstance damage = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			damage.addPersistentModifier(new EntityAttributeModifier(ENRAGE_ID, 0.35, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			BossFx.erupt(world, this, getPos(), 12, 10.0f);
			BossFx.spikeRing(world, getPos(), 7, 10, 1.4f);
			Cutscenes.shake(world, getPos(), 50, 3.0f, 16);
			for (LivingEntity e : Fx.around(world, getPos(), 14, BossFx.foesOf(this))) {
				Vec3d away = e.getPos().subtract(getPos()).multiply(1, 0, 1).normalize().multiply(2.0);
				Fx.push(e, away.add(0, 0.6, 0));
			}
			Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 3.0f, 0.6f);
			heal(getMaxHealth() * 0.05f);
			state = State.FIGHT;
			for (ServerPlayerEntity p : fighters(40)) {
				setTarget(p);
				break;
			}
		}
		stateTicks++;
	}

	/** He falls to his knees, speaks his last words, and the crystal in him cracks. */
	private void beginDeath(ServerWorld world) {
		dying = DEATH_TICKS;
		action = ACT_NONE;
		setTarget(null);
		getNavigation().stop();
		play(ANIM_KNEEL, DEATH_TICKS + 20);
		say(world, "message.amethystmod.amitariel.death");
		Cutscenes.play(fighters(40), this, DEATH_TICKS, Text.translatable("title.amethystmod.amitariel.death"),
				Text.translatable("message.amethystmod.amitariel.death"));
		Fx.sound(world, this, SoundEvents.ENTITY_WARDEN_DEATH, 2.0f, 1.2f);
	}

	private void deathTick(ServerWorld world) {
		dying--;
		setVelocity(0, getVelocity().y, 0);
		getNavigation().stop();
		if (dying % 8 == 0) {
			Fx.particles(world, ModParticles.PRISM_FLARE, getPos().add(0, 1.2, 0), 4, 0.6, 0.1);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.6f + (DEATH_TICKS - dying) / 100.0f);
		}
		if (dying == 0) {
			BossFx.spikeRing(world, getPos(), 3, 8, 1.2f);
			Cutscenes.shake(world, getPos(), 40, 2.0f, 12);
			super.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
			RiftManager.openTowerRift(world);
		}
	}

	/** A few servants answer his call: a pair of weavers at first, later one shard-walker. */
	private void summonGuards(ServerWorld world, EntityType<? extends AmethystMob> type, int count) {
		for (int i = 0; i < count; i++) {
			AmethystMob guard = type.create(world);
			if (guard == null) {
				continue;
			}
			double a = random.nextDouble() * Math.PI * 2;
			BlockPos pos = BlockPos.ofFloored(getX() + Math.cos(a) * 5, getY(), getZ() + Math.sin(a) * 5);
			guard.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
			guard.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null);
			world.spawnEntityAndPassengers(guard);
			Fx.particles(world, ModParticles.PRISM_FLARE, guard.getPos().add(0, 1, 0), 20, 0.5, 0.1);
		}
		Fx.sound(world, this, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
	}

	// ---- chat -------------------------------------------------------------------------

	@Override
	public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Fighting")) {
			state = State.FIGHT;
		}
		if (nbt.getInt("Phase") == 2) {
			dataTracker.set(PHASE, 2);
			bar.setColor(BossBar.Color.RED);
		}
		summoned60 = nbt.getBoolean("Summoned60");
		summoned30 = nbt.getBoolean("Summoned30");
	}

	@Override
	public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Fighting", state == State.FIGHT || state == State.TRANSFORM);
		nbt.putInt("Phase", getPhase());
		nbt.putBoolean("Summoned60", summoned60);
		nbt.putBoolean("Summoned30", summoned30);
	}

	// ---- self-test hooks -------------------------------------------------------------------

	/** Self-test only: skips the conversation and fights {@code target}. */
	public void debugFight(LivingEntity target) {
		state = State.FIGHT;
		super.setTarget(target);
	}

	/** Self-test only: plays one of the new moves right now (0 whirl, 1 leap, 2 spears). */
	public void debugMove(int move) {
		LivingEntity target = getTarget();
		if (!(getWorld() instanceof ServerWorld world) || target == null) {
			return;
		}
		switch (move) {
			case 0 -> new Whirl(this).release(world, target);
			case 1 -> {
				Leap leap = new Leap(this);
				leap.windupTick(world, target, 0);
				leap.release(world, target);
			}
			default -> new Spears(this).release(world, target);
		}
	}

	public String debugState() {
		return "action " + action + ", busy " + busy() + ", on ground " + isOnGround() + ", at " + getBlockPos();
	}

	// ---- the whirl and the leap play out after their wind-up -----------------------------

	boolean busy() {
		return action != ACT_NONE || lungeTicks > 0;
	}

	private float power() {
		return getPhase() == 2 ? 1.25f : 1.0f;
	}

	/** He spins with his spear held out, drifting slowly after his target and cutting all around. */
	private void spinTick(ServerWorld world) {
		getNavigation().stop();
		float yaw = getYaw() + 32.0f;
		setYaw(yaw);
		setBodyYaw(yaw);
		setHeadYaw(yaw);
		LivingEntity target = getTarget();
		if (target != null) {
			Vec3d to = target.getPos().subtract(getPos()).multiply(1, 0, 1);
			Vec3d step = to.lengthSquared() < 1 ? Vec3d.ZERO : to.normalize().multiply(0.11);
			setVelocity(step.x, getVelocity().y, step.z);
		}
		Vec3d c = getPos().add(0, 1.2, 0);
		if (actionTicks % 8 == 0) {
			for (LivingEntity e : Fx.around(world, c, 3.6, BossFx.foesOf(this))) {
				e.damage(world.getDamageSources().mobAttack(this), 6.0f * power());
				Vec3d away = e.getPos().subtract(getPos()).multiply(1, 0, 1);
				Fx.push(e, (away.lengthSquared() < 0.01 ? Vec3d.ZERO : away.normalize().multiply(0.9)).add(0, 0.3, 0));
				AmethystMob.infect(e, 100);
			}
			Fx.sound(world, this, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
		}
		play(ANIM_SPIN, 4);
		if (--actionTicks <= 0) {
			action = ACT_NONE;
			play(ANIM_RAISE, 0);
		}
	}

	/** In the air after the leap: he steers to the marked spot and smashes the floor when he lands. */
	private void leapTick(ServerWorld world) {
		actionTicks++;
		fallDistance = 0;
		getNavigation().stop();
		int remaining = Math.max(1, LEAP_TICKS - actionTicks);
		Vec3d to = leapSpot.subtract(getPos()).multiply(1, 0, 1).multiply(1.0 / remaining);
		if (to.length() > 1.2) {
			to = to.normalize().multiply(1.2);
		}
		setVelocity(to.x, getVelocity().y, to.z);
		velocityModified = true;
		if (actionTicks % 3 == 0) {
			world.spawnParticles(ModParticles.CRYSTAL_SPARK, getX(), getY() + 1, getZ(), 2, 0.3, 0.5, 0.3, 0.05);
		}
		if ((isOnGround() && actionTicks > 6) || actionTicks > 50) {
			action = ACT_NONE;
			play(ANIM_SLAM, 6);
			Vec3d at = getPos();
			BossFx.erupt(world, this, at, 4.2, 13.0f * power());
			BeamEffectEntity.burst(world, at, 0.6f, 0.45f, 0.8f, 10, BeamEffectEntity.AMETHYST);
			// crystals grow out of the floor where the spear struck, and stay a while
			for (int i = 0; i < 7; i++) {
				double a = i * Math.PI * 2 / 7 + random.nextDouble() * 0.5;
				double r = 2.2 + random.nextDouble() * 2.4;
				BossFx.spike(world, at.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1.3f + random.nextFloat() * 0.6f, 70);
			}
			Cutscenes.shake(world, at, 24, 2.2f, 10);
			Fx.sound(world, this, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.6f, 0.7f);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.5f);
		}
	}

	private static final int LEAP_TICKS = 22;

	/** While the whirl or the leap runs, his other goals (walking, abilities) stay quiet. */
	private class Busy extends net.minecraft.entity.ai.goal.Goal {
		Busy() {
			setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return action != ACT_NONE;
		}

		@Override
		public boolean shouldContinue() {
			return action != ACT_NONE;
		}
	}

	/** He raises his spear, a circle lights up around him, then he whirls. */
	private static class Whirl extends AbilityGoals.Windup {
		private final AmitarielEntity boss;

		Whirl(AmitarielEntity boss) {
			super(boss, 18, 200);
			this.boss = boss;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && distanceSq <= 7 * 7;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				BossFx.marker(world, boss.getPos(), 3.6, 20);
				Fx.sound(world, boss, SoundEvents.ITEM_TRIDENT_RIPTIDE_1.value(), 1.5f, 0.7f);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			boss.action = ACT_SPIN;
			boss.actionTicks = boss.getPhase() == 2 ? 64 : 48;
			BeamEffectEntity.whirl(world, boss, 2.6f, 0.56f, boss.actionTicks + 4, BeamEffectEntity.AMETHYST);
			Fx.sound(world, boss, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(), 1.5f, 0.8f);
		}
	}

	/** He crouches, leaps high toward where you stood and drives his spear into the floor there. */
	private static class Leap extends AbilityGoals.Windup {
		private final AmitarielEntity boss;
		private Vec3d spot = Vec3d.ZERO;

		Leap(AmitarielEntity boss) {
			super(boss, 16, 210);
			this.boss = boss;
		}

		@Override
		protected int anim() {
			return ANIM_CROUCH;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && boss.isOnGround() && distanceSq >= 25 && distanceSq <= 20 * 20;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				// the spot is marked from the start: there is time to get out of the circle
				spot = target.getPos();
				BossFx.marker(world, spot, 4.2, 16 + LEAP_TICKS + 6);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			boss.leapSpot = spot;
			boss.action = ACT_LEAP;
			boss.actionTicks = 0;
			Vec3d to = spot.subtract(boss.getPos()).multiply(1, 0, 1).multiply(1.0 / LEAP_TICKS);
			boss.setVelocity(to.x, 1.0, to.z);
			boss.velocityModified = true;
			boss.play(ANIM_SLAM, 40);
			Fx.sound(world, boss, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.6f);
		}
	}

	/** Spears of light appear in a ring over his head, take aim one by one and shoot. */
	private static class Spears extends AbilityGoals.Windup {
		private final AmitarielEntity boss;

		Spears(AmitarielEntity boss) {
			super(boss, 20, 240);
			this.boss = boss;
		}

		@Override
		protected int anim() {
			return ANIM_CAST;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && distanceSq <= 30 * 30;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				Fx.sound(world, boss, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 2.0f, 0.6f);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			List<ServerPlayerEntity> players = boss.fighters(32);
			int count = boss.getPhase() == 2 ? 6 : 4;
			for (int i = 0; i < count; i++) {
				LivingEntity aim = players.isEmpty() ? target : players.get(i % players.size());
				PhantomSpearEntity.summon(world, boss, aim, i, count, 30 + i * 10, 9.0f * boss.power());
			}
			boss.play(ANIM_SPREAD, 30);
			Fx.sound(world, boss, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 0.7f);
		}
	}

	// ---- abilities --------------------------------------------------------------------

	/** A dash across the arena that cuts through whoever it reaches. */
	private static class Lunge extends AbilityGoals.Windup {
		private final AmitarielEntity boss;

		Lunge(AmitarielEntity boss) {
			super(boss, 24, 170);
			this.boss = boss;
		}

		@Override
		protected int anim() {
			return ANIM_THRUST;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && distanceSq >= 36 && distanceSq <= 18 * 18;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed % 3 == 0) {
				Vec3d eye = boss.getPos().add(0, 1.5, 0);
				world.spawnParticles(ModParticles.PRISM_FLARE, eye.x, eye.y, eye.z, 1, 0.5, 0.8, 0.5, 0.05);
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			Vec3d dir = target.getPos().subtract(boss.getPos()).multiply(1, 0, 1).normalize();
			boss.setVelocity(dir.x * 1.5, 0.25, dir.z * 1.5);
			boss.velocityModified = true;
			boss.lungeTicks = 16;
			boss.lungeHit = false;
			Fx.sound(world, boss, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.7f);
		}
	}

	/** Crystal pillars burst out beneath every player in the fight. */
	private static class Pillars extends AbilityGoals.Windup {
		private final AmitarielEntity boss;
		private final List<Vec3d> spots = new ArrayList<>();

		Pillars(AmitarielEntity boss) {
			super(boss, 34, 190);
			this.boss = boss;
		}

		@Override
		protected int anim() {
			return ANIM_SLAM;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && distanceSq <= 30 * 30;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				spots.clear();
				for (ServerPlayerEntity p : boss.fighters(32)) {
					spots.add(p.getPos());
					BossFx.marker(world, p.getPos(), 2.6, 34);
				}
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			for (Vec3d spot : spots) {
				BossFx.erupt(world, boss, spot, 2.6, 15.0f);
			}
		}
	}

	/** Phase 2: a rain of crystal shards falls on and around every player. */
	private static class Storm extends AbilityGoals.Windup {
		private final AmitarielEntity boss;
		private final List<Vec3d> spots = new ArrayList<>();

		Storm(AmitarielEntity boss) {
			super(boss, 44, 190);
			this.boss = boss;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && boss.getPhase() == 2 && distanceSq <= 30 * 30;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			if (elapsed == 0) {
				spots.clear();
				for (ServerPlayerEntity p : boss.fighters(32)) {
					spots.add(p.getPos());
					for (int i = 0; i < 3; i++) {
						spots.add(p.getPos().add(boss.random.nextDouble() * 12 - 6, 0, boss.random.nextDouble() * 12 - 6));
					}
				}
				Fx.sound(world, boss, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 2.0f, 0.5f);
			}
			if (elapsed == 1) {
				for (Vec3d spot : spots) {
					BossFx.marker(world, spot, 1.6, 43);
				}
			}
			if (elapsed % 8 == 0) {
				for (Vec3d spot : spots) {
					world.spawnParticles(ModParticles.PRISM_FLARE, spot.x, spot.y + 12, spot.z, 1, 0.4, 0.4, 0.4, 0.0);
				}
			}
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			for (Vec3d spot : spots) {
				for (int i = 0; i < 3; i++) {
					CrystalShardEntity shard = new CrystalShardEntity(world, boss, 9.0f);
					shard.setPosition(spot.x + boss.random.nextDouble() - 0.5, spot.y + 12 + i * 1.5, spot.z + boss.random.nextDouble() - 0.5);
					shard.setVelocity(0, -1, 0, 1.6f, 0.5f);
					world.spawnEntity(shard);
				}
			}
			Fx.sound(world, boss, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 0.8f);
		}
	}

	/** Phase 2: vanishes in a flash and strikes from behind. */
	private static class Blink extends AbilityGoals.Windup {
		private final AmitarielEntity boss;

		Blink(AmitarielEntity boss) {
			super(boss, 20, 170);
			this.boss = boss;
		}

		@Override
		protected int anim() {
			return ANIM_CROUCH;
		}

		@Override
		protected boolean inRange(LivingEntity target, double distanceSq) {
			return !boss.busy() && boss.getPhase() == 2 && distanceSq >= 16 && distanceSq <= 22 * 22;
		}

		@Override
		protected void windupTick(ServerWorld world, LivingEntity target, int elapsed) {
			Fx.particles(world, ParticleTypes.REVERSE_PORTAL, boss.getPos().add(0, 1.5, 0), 10, 0.5, 0.4);
		}

		@Override
		protected void release(ServerWorld world, LivingEntity target) {
			Vec3d look = target.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
			Vec3d behind = target.getPos().subtract(look.multiply(2.2));
			Fx.particles(world, ModParticles.PRISM_FLARE, boss.getPos().add(0, 1.5, 0), 30, 0.6, 0.3);
			boss.requestTeleport(behind.x, behind.y, behind.z);
			boss.setYaw((float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0));
			Fx.particles(world, ModParticles.PRISM_FLARE, boss.getPos().add(0, 1.5, 0), 30, 0.6, 0.3);
			Fx.sound(world, boss, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.6f);
			if (boss.tryAttack(target)) {
				Fx.push(target, look.multiply(1.1).add(0, 0.5, 0));
			}
		}
	}
}
