package com.amethystmod.item;

import com.amethystmod.ModParticles;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Amitariel's spear, dropped when he falls. Tap right-click: a whirling spin for three seconds that
 * cuts everything around you. Hold it a moment and let go: a spinning dash forward that ends in a
 * sweep throwing enemies back.
 */
public class AmitarielSpearItem extends Item {
	/** Hold at least this long for the dash; a shorter press is the whirl. */
	public static final int HOLD = 8;
	private static final int WHIRL_TICKS = 60;
	private static final int DASH_TICKS = 14;

	/** Players whirling: ticks left. */
	private static final Map<UUID, Integer> WHIRLING = new HashMap<>();
	/** Players dashing: ticks left until the closing sweep. */
	private static final Map<UUID, Integer> DASHING = new HashMap<>();

	public AmitarielSpearItem(Settings settings) {
		super(settings);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(AmitarielSpearItem::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.isOf(DamageTypes.FALL) && DASHING.containsKey(entity.getUuid())));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this) || WHIRLING.containsKey(user.getUuid())) {
			return TypedActionResult.fail(stack);
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (used == HOLD && world instanceof ServerWorld serverWorld) {
			// ready to dash
			Fx.sound(serverWorld, user, SoundEvents.ITEM_TRIDENT_RIPTIDE_1.value(), 0.6f, 1.4f);
			Fx.particles(serverWorld, ModParticles.CRYSTAL_SPARK, user.getPos().add(0, 1.2, 0), 8, 0.4, 0.05);
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) {
			return;
		}
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		EquipmentSlot slot = player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
		if (used < HOLD) {
			if (world instanceof ServerWorld serverWorld) {
				WHIRLING.put(player.getUuid(), WHIRL_TICKS);
				com.amethystmod.entity.BeamEffectEntity.whirl(serverWorld, player, 2.0f, 0.5f, WHIRL_TICKS + 2, com.amethystmod.entity.BeamEffectEntity.AMETHYST);
				player.getItemCooldownManager().set(this, WHIRL_TICKS + 60);
				stack.damage(1, player, slot);
				Fx.sound(serverWorld, player, SoundEvents.ITEM_TRIDENT_RIPTIDE_2.value(), 1.0f, 1.2f);
			}
			return;
		}
		// the dash: runs on both sides, the client moves its own player
		Vec3d look = player.getRotationVector();
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : flat.normalize();
		player.setVelocity(flat.x * 2.0, 0.2, flat.z * 2.0);
		player.useRiptide(DASH_TICKS, 8.0f, stack);
		if (world instanceof ServerWorld serverWorld) {
			DASHING.put(player.getUuid(), DASH_TICKS);
			com.amethystmod.entity.BeamEffectEntity.whirl(serverWorld, player, 1.3f, 0.9f, DASH_TICKS + 2, com.amethystmod.entity.BeamEffectEntity.AMETHYST);
			player.getItemCooldownManager().set(this, 70);
			stack.damage(1, player, slot);
			Fx.sound(serverWorld, player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(), 1.0f, 1.0f);
		}
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = WHIRLING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> e = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
			int left = e.getValue() - 1;
			if (player == null || !player.isAlive() || left <= 0) {
				it.remove();
				continue;
			}
			e.setValue(left);
			whirl(player.getServerWorld(), player, WHIRL_TICKS - left);
		}
		it = DASHING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> e = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
			int left = e.getValue() - 1;
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			player.fallDistance = 0;
			e.setValue(left);
			if (left <= 0) {
				it.remove();
				sweep(player.getServerWorld(), player);
			}
		}
	}

	/** One tick of the whirl: slashes circle the player, and every half second they cut. */
	private static void whirl(ServerWorld world, ServerPlayerEntity player, int t) {
		Vec3d c = player.getPos().add(0, 1.0, 0);
		if (t % 4 == 0) {
			player.swingHand(t % 8 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
		}
		if (t % 10 == 0) {
			for (LivingEntity e : Fx.around(world, c, 3.4, Fx.hostileTo(player))) {
				e.damage(world.getDamageSources().playerAttack(player), 7.0f);
				Vec3d away = e.getPos().subtract(player.getPos()).multiply(1, 0, 1);
				Fx.push(e, (away.lengthSquared() < 0.01 ? Vec3d.ZERO : away.normalize().multiply(0.6)).add(0, 0.25, 0));
			}
			Fx.sound(world, player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f + (t % 20) * 0.02f);
		}
	}

	/** The end of the dash: a short spin that throws everything around back. */
	private static void sweep(ServerWorld world, ServerPlayerEntity player) {
		Vec3d c = player.getPos().add(0, 1.0, 0);
		com.amethystmod.entity.BeamEffectEntity.whirl(world, player, 2.3f, 1.0f, 9, com.amethystmod.entity.BeamEffectEntity.AMETHYST);
		com.amethystmod.entity.BeamEffectEntity.burst(world, player.getPos(), 0.6f, 0.4f, 0.7f, 9, com.amethystmod.entity.BeamEffectEntity.AMETHYST);
		Fx.particles(world, ModParticles.PRISM_FLARE, c, 8, 1.5, 0.15);
		for (LivingEntity e : Fx.around(world, c, 3.6, Fx.hostileTo(player))) {
			e.damage(world.getDamageSources().playerAttack(player), 8.0f);
			Vec3d away = e.getPos().subtract(player.getPos()).multiply(1, 0, 1);
			Fx.push(e, (away.lengthSquared() < 0.01 ? Vec3d.ZERO : away.normalize().multiply(1.4)).add(0, 0.45, 0));
		}
		player.swingHand(Hand.MAIN_HAND, true);
		Fx.sound(world, player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
		Fx.sound(world, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.7f);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
