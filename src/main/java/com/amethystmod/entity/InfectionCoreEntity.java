package com.amethystmod.entity;

import com.amethystmod.CoreManager;
import com.amethystmod.ModParticles;
import com.amethystmod.item.Fx;
import com.amethystmod.item.ModTags;
import com.amethystmod.world.CoreState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;

/**
 * The crystal at the heart of a patch of infection. It stands still and never wanders; its record in
 * {@link CoreState} (its age) decides its phase:
 * <ol>
 * <li>a plain crystal about a block tall, easy to break;</li>
 * <li>two blocks tall; calls weak amethyst creatures around itself;</li>
 * <li>a three-block monolith that shoots shards and raises crystals under players, calls dangerous
 * creatures, and hides behind a barrier that only purified amethyst can break.</li>
 * </ol>
 * Killing it stops its land from growing (the land itself stays infected).
 */
public class InfectionCoreEntity extends AmethystMob {
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(InfectionCoreEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> DYING = DataTracker.registerData(InfectionCoreEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static final float[] HEALTH = {0, 16, 70, 160};
	public static final float BARRIER_MAX = 120;
	/** Ticks without a purified hit before the barrier starts to grow back. */
	private static final int BARRIER_REGEN_DELAY = 300;

	private int coreId = -1;
	private float barrier;
	private int barrierDelay;
	private int summonTimer = 200;
	private int attackTimer = 80;
	private Vec3d eruptAt;
	private int eruptTimer;
	private int warnCooldown;
	private BeamEffectEntity dome;
	private ServerBossBar bar;

	public InfectionCoreEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		setPersistent();
		experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, HEALTH[1])
				.add(EntityAttributes.GENERIC_ARMOR, 2)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PHASE, 1);
		builder.add(DYING, false);
	}

	public int getPhase() {
		return dataTracker.get(PHASE);
	}

	public boolean isDying() {
		return dataTracker.get(DYING);
	}

	public int getCoreId() {
		return coreId;
	}

	public float getBarrier() {
		return barrier;
	}

	/** Ties a fresh entity to its record and gives it the record's phase. */
	public void bind(int id, int phase) {
		coreId = id;
		applyPhase(phase, true);
	}

	private void applyPhase(int phase, boolean heal) {
		dataTracker.set(PHASE, phase);
		var health = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH[phase]);
		}
		if (heal) {
			setHealth(getMaxHealth());
			if (phase == 3) {
				barrier = BARRIER_MAX;
			}
		} else if (getHealth() > getMaxHealth()) {
			setHealth(getMaxHealth());
		}
		if (phase < 3) {
			barrier = 0;
		}
		calculateDimensions();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (PHASE.equals(data)) {
			calculateDimensions();
		}
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return switch (getPhase()) {
			case 3 -> EntityDimensions.fixed(1.9f, 3.4f);
			case 2 -> EntityDimensions.fixed(1.3f, 2.3f);
			default -> EntityDimensions.fixed(0.9f, 1.3f);
		};
	}

	// ---- a crystal, not a creature ----------------------------------------------------------

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
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return false;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	// ---- damage and the barrier --------------------------------------------------------------

	/** Was this blow struck with purified amethyst (a purified weapon, or the light of the purified spear)? */
	public static boolean purified(DamageSource source) {
		if (source.getSource() instanceof PhantomSpearEntity spear && spear.isPure()) {
			return true;
		}
		ItemStack weapon = source.getWeaponStack();
		if (weapon != null && weapon.isIn(ModTags.PURIFIED_AMETHYST)) {
			return true;
		}
		return source.getAttacker() instanceof LivingEntity attacker && attacker.getMainHandStack().isIn(ModTags.PURIFIED_AMETHYST);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof CrystalKin) {
			return false;
		}
		if (getWorld() instanceof ServerWorld world && getPhase() == 3 && barrier > 0 && !source.isSourceCreativePlayer()
				&& !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) {
			Vec3d at = getPos().add(0, getHeight() * 0.5, 0);
			if (purified(source)) {
				barrier = Math.max(0, barrier - amount * 1.5f);
				barrierDelay = BARRIER_REGEN_DELAY;
				Fx.particles(world, ModParticles.WARD_MOTE, at, 18, 0.9, 0.15);
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.4f, 1.5f);
				if (barrier <= 0) {
					BeamEffectEntity.burst(world, at, 0.6f, 0.45f, 0.8f, 18, BeamEffectEntity.PURE);
					Fx.sound(world, this, SoundEvents.BLOCK_GLASS_BREAK, 2.0f, 0.6f);
					if (dome != null) {
						dome.discard();
						dome = null;
					}
				}
				updateBar();
			} else {
				Fx.particles(world, ModParticles.CRYSTAL_SPARK, at, 10, 0.7, 0.1);
				Fx.sound(world, this, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.4f);
				if (source.getAttacker() instanceof ServerPlayerEntity player && warnCooldown <= 0) {
					warnCooldown = 60;
					player.sendMessage(Text.translatable("message.amethystmod.core.barrier").formatted(Formatting.LIGHT_PURPLE), true);
				}
			}
			return false;
		}
		boolean hurt = super.damage(source, amount);
		if (hurt) {
			updateBar();
		}
		return hurt;
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld world) {
			// older cores give more on top of their loot table
			int phase = getPhase();
			if (phase >= 2 && source.getAttacker() instanceof PlayerEntity) {
				dropStack(new ItemStack(com.amethystmod.item.ModItems.AMETHYST_ESSENCE, 3 + random.nextInt(4) + (phase == 3 ? 8 : 0)));
				if (phase == 3 || random.nextFloat() < 0.35f) {
					dropStack(new ItemStack(com.amethystmod.item.ModItems.RESONANT_SHARD, phase == 3 ? 1 + random.nextInt(2) : 1));
				}
				if (phase == 3) {
					dropStack(new ItemStack(com.amethystmod.item.ModItems.PURIFIED_AMETHYST, 1));
				}
			}
			shatterEffects(world, 1.0f);
			CoreManager.onKilled(world, coreId);
		}
	}

	/** Breaks without loot: the record is gone (a command) or its time has come after the Heart's fall. */
	public void shatter() {
		if (getWorld() instanceof ServerWorld world) {
			shatterEffects(world, isDying() ? 0.5f : 1.0f);
		}
		discard();
	}

	private void shatterEffects(ServerWorld world, float strength) {
		Vec3d at = getPos().add(0, getHeight() * 0.5, 0);
		Fx.particles(world, ModParticles.PRISM_FLARE, at, (int) (50 * strength), 0.4 + getPhase() * 0.3, 0.3);
		Fx.particles(world, ParticleTypes.EXPLOSION, at, 1, 0.0, 0.0);
		BeamEffectEntity.burst(world, at, 0.5f, 0.4f, 0.8f, 20, BeamEffectEntity.AMETHYST);
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 2.5f, 0.5f);
	}

	// ---- life ----------------------------------------------------------------------------------

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		setVelocity(Vec3d.ZERO);
		if (coreId < 0 || !CoreManager.claim(this)) {
			discard();
			return;
		}
		if (warnCooldown > 0) {
			warnCooldown--;
		}
		if (age % 20 == 0 && !syncWithRecord(world)) {
			return;
		}
		int phase = getPhase();
		if (age % 10 == 0) {
			Vec3d top = getPos().add(0, getHeight() * 0.8, 0);
			if (isDying()) {
				world.spawnParticles(ParticleTypes.ASH, top.x, top.y, top.z, 6, 0.5, 0.6, 0.5, 0.01);
			} else {
				world.spawnParticles(ModParticles.CRYSTAL_SPARK, top.x, top.y, top.z, phase * 2, 0.4 * phase, 0.5 * phase, 0.4 * phase, 0.02);
			}
		}
		if (age % 10 == 0) {
			updateBar();
		}
		if (isDying() || world.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		if (phase >= 2) {
			summon(world, phase);
		}
		if (phase == 3) {
			fight(world);
			keepBarrier(world);
		}
	}

	/** Reads the record: new phase, the Heart's fall, or gone (removed by a command). False if discarded. */
	private boolean syncWithRecord(ServerWorld world) {
		CoreState.Core core = CoreManager.core(world.getServer(), coreId);
		if (core == null) {
			shatter();
			return false;
		}
		long now = world.getTime();
		if (core.dieAt > 0) {
			if (now >= core.dieAt) {
				CoreManager.remove(world.getServer(), core);
				shatter();
				return false;
			}
			if (!isDying() && now >= core.dieAt - 2400) {
				dataTracker.set(DYING, true);
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.4f);
			}
		}
		int phase = CoreManager.phase(core, world);
		if (phase != getPhase()) {
			boolean grew = phase > getPhase();
			applyPhase(phase, grew);
			if (grew) {
				Vec3d at = getPos().add(0, 1, 0);
				BeamEffectEntity.burst(world, at, 0.5f, 0.35f, 0.6f, 22, BeamEffectEntity.AMETHYST);
				Fx.particles(world, ModParticles.PRISM_FLARE, at, 40, 1.0, 0.2);
				Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 3.0f, 0.6f);
			}
		}
		if (core.y != getBlockY()) {
			core.y = getBlockY();
		}
		return true;
	}

	/** Calls creatures of the amethyst around itself now and then, up to a few at a time. */
	private void summon(ServerWorld world, int phase) {
		if (--summonTimer > 0) {
			return;
		}
		summonTimer = phase == 3 ? 400 + random.nextInt(200) : 600 + random.nextInt(300);
		if (BossFx.playersNear(world, getPos(), 40).isEmpty()) {
			return;
		}
		int cap = phase == 3 ? 5 : 3;
		List<AmethystMob> around = world.getEntitiesByClass(AmethystMob.class, getBoundingBox().expand(20),
				e -> e.isAlive() && !(e instanceof InfectionCoreEntity));
		if (around.size() >= cap) {
			return;
		}
		EntityType<? extends MobEntity> type;
		if (phase == 3 && random.nextBoolean()) {
			type = random.nextBoolean() ? ModEntities.WALKER : ModEntities.LURKER;
		} else {
			type = random.nextInt(3) == 0 ? ModEntities.WEAVER : ModEntities.STALKER;
		}
		double a = random.nextDouble() * Math.PI * 2;
		double r = 3 + random.nextDouble() * 5;
		int x = MathHelper.floor(getX() + Math.cos(a) * r);
		int z = MathHelper.floor(getZ() + Math.sin(a) * r);
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (Math.abs(y - getY()) > 6) {
			return;
		}
		BlockPos at = new BlockPos(x, y, z);
		MobEntity mob = type.create(world, null, at, SpawnReason.MOB_SUMMONED, false, false);
		if (mob == null || !world.isSpaceEmpty(mob)) {
			return;
		}
		world.spawnEntity(mob);
		BossFx.marker(world, Vec3d.ofBottomCenter(at), 1.4, 30);
		Fx.particles(world, ModParticles.PRISM_FLARE, Vec3d.ofBottomCenter(at).add(0, 1, 0), 24, 0.6, 0.1);
		Fx.sound(world, mob, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6f, 1.2f);
	}

	/** Phase 3: shard volleys and crystals erupting under players. */
	private void fight(ServerWorld world) {
		if (eruptAt != null && --eruptTimer <= 0) {
			BossFx.erupt(world, this, eruptAt, 2.2, 7.0f);
			eruptAt = null;
		} else if (eruptAt != null) {
			BossFx.telegraph(world, eruptAt, 2.2, 1.0f - eruptTimer / 24.0f);
		}
		if (--attackTimer > 0) {
			return;
		}
		attackTimer = 60 + random.nextInt(50);
		List<ServerPlayerEntity> near = BossFx.playersNear(world, getPos(), 24);
		near.removeIf(PlayerEntity::isCreative);
		if (near.isEmpty()) {
			return;
		}
		ServerPlayerEntity target = near.get(random.nextInt(near.size()));
		play(ANIM_CAST, 20);
		if (eruptAt == null && random.nextBoolean()) {
			eruptAt = target.getPos();
			eruptTimer = 24;
			BossFx.marker(world, eruptAt, 2.2, 26);
			Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.8f);
			return;
		}
		Vec3d from = getPos().add(0, getHeight() * 0.75, 0);
		for (int i = 0; i < 3; i++) {
			Vec3d aim = target.getEyePos().subtract(from);
			CrystalShardEntity shard = new CrystalShardEntity(world, this, 5.0f);
			shard.setPosition(from.x, from.y, from.z);
			shard.setVelocity(aim.x, aim.y, aim.z, 1.1f, 6.0f + i * 3.0f);
			world.spawnEntity(shard);
		}
		Fx.sound(world, this, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 1.4f);
	}

	/** Phase 3: the barrier grows back when nobody pierces it, and is shown as a dome while players are near. */
	private void keepBarrier(ServerWorld world) {
		if (barrierDelay > 0) {
			barrierDelay--;
		} else if (barrier < BARRIER_MAX && age % 10 == 0) {
			boolean wasDown = barrier <= 0;
			barrier = Math.min(BARRIER_MAX, barrier + 1.0f);
			if (wasDown) {
				Fx.sound(world, this, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.4f);
			}
		}
		if (barrier <= 0 || age % 20 != 0) {
			return;
		}
		if (BossFx.playersNear(world, getPos(), 48).isEmpty()) {
			return;
		}
		if (dome == null || dome.isRemoved()) {
			dome = BeamEffectEntity.dome(world, this, 2.6f, 45, BeamEffectEntity.AMETHYST);
		} else {
			dome.keepAlive(45);
		}
	}

	// ---- the bar over the screen -------------------------------------------------------------

	private void updateBar() {
		if (!(getWorld() instanceof ServerWorld world) || getPhase() < 2) {
			if (bar != null) {
				bar.clearPlayers();
				bar = null;
			}
			return;
		}
		if (bar == null) {
			bar = new ServerBossBar(Text.empty(), BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
		}
		boolean shield = getPhase() == 3 && barrier > 0;
		bar.setName(shield
				? Text.translatable("bossbar.amethystmod.core.barrier", getPhase(), Math.round(barrier * 100 / BARRIER_MAX))
				: Text.translatable("bossbar.amethystmod.core", getPhase()));
		bar.setColor(shield ? BossBar.Color.BLUE : BossBar.Color.PURPLE);
		bar.setPercent(shield ? barrier / BARRIER_MAX : getHealth() / getMaxHealth());
		for (ServerPlayerEntity p : List.copyOf(bar.getPlayers())) {
			if (p.isRemoved() || p.getWorld() != world || p.squaredDistanceTo(this) > 34 * 34) {
				bar.removePlayer(p);
			}
		}
		for (ServerPlayerEntity p : world.getPlayers(p -> p.squaredDistanceTo(this) < 28 * 28)) {
			bar.addPlayer(p);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (bar != null) {
			bar.clearPlayers();
			bar = null;
		}
		if (dome != null) {
			dome.discard();
			dome = null;
		}
		CoreManager.release(this);
	}

	// ---- saving ------------------------------------------------------------------------------

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("CoreId", coreId);
		nbt.putInt("CorePhase", getPhase());
		nbt.putFloat("Barrier", barrier);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		coreId = nbt.contains("CoreId") ? nbt.getInt("CoreId") : -1;
		dataTracker.set(PHASE, Math.max(1, Math.min(3, nbt.getInt("CorePhase"))));
		barrier = nbt.getFloat("Barrier");
		calculateDimensions();
	}
}
