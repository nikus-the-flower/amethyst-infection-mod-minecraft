package com.amethystmod.item;

import com.amethystmod.ModParticles;
import com.amethystmod.entity.PhantomSpearEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Purified Crystal Spear. Hold right-click: spears of white light appear over you one after
 * another and fly to where you were looking when each appeared (the ones already flying keep their
 * course). Keep holding for ten seconds and it bursts: a volley of spears fires like a machine gun at
 * whatever you look at. Then it needs to rest.
 */
public class PurifiedSpearItem extends Item {
	public static final int ULTRA_AT = 200;
	private static final int EVERY = 7;
	private static final int VOLLEY = 18;

	/** Players firing the volley: spears left to fire. */
	private static final Map<UUID, Integer> VOLLEYS = new HashMap<>();

	public PurifiedSpearItem(Settings settings) {
		super(settings);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(PurifiedSpearItem::tick);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this) || VOLLEYS.containsKey(user.getUuid())) {
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

	/** Where the player is looking: the block in front of them, or a point far away. */
	private static Vec3d aimPoint(ServerWorld world, LivingEntity user) {
		Vec3d eye = user.getEyePos();
		Vec3d end = eye.add(user.getRotationVec(1.0f).multiply(48));
		BlockHitResult hit = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
	}

	/** A spot over the player's head, to the side by {@code side} (-1..1). */
	private static Vec3d overHead(LivingEntity user, double side, double up) {
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d right = new Vec3d(-look.z, 0, look.x);
		right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
		return user.getEyePos().add(0, 1.0 + up, 0).add(right.multiply(side * 1.6));
	}

	private static void fire(ServerWorld world, PlayerEntity user, Vec3d from, float damage) {
		Vec3d to = aimPoint(world, user);
		PhantomSpearEntity.shoot(world, user, from, to.subtract(from), damage);
		Fx.particles(world, ModParticles.WARD_MOTE, from, 4, 0.2, 0.05);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof PlayerEntity player)) {
			return;
		}
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (used % EVERY == 3) {
			fire(serverWorld, player, overHead(user, user.getRandom().nextDouble() * 2 - 1, user.getRandom().nextDouble() * 0.6), 6.0f);
			Fx.sound(serverWorld, user, SoundEvents.ITEM_TRIDENT_THROW.value(), 0.6f, 1.4f + user.getRandom().nextFloat() * 0.3f);
			if (used % (EVERY * 6) == 3) {
				stack.damage(1, user, user.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			}
		}
		if (used % 40 == 0 && used > 0) {
			// the spear hums higher and higher as the volley gathers
			Fx.sound(serverWorld, user, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.6f + used / (float) ULTRA_AT);
		}
		if (used > ULTRA_AT - 60 && used % 3 == 0) {
			Fx.particles(serverWorld, ModParticles.WARD_MOTE, user.getPos().add(0, 2.4, 0), 3, 1.0, 0.05);
		}
		if (used >= ULTRA_AT) {
			VOLLEYS.put(player.getUuid(), VOLLEY);
			Fx.sound(serverWorld, user, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.2f);
			user.stopUsingItem();
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (user instanceof PlayerEntity player && !world.isClient) {
			player.getItemCooldownManager().set(this, VOLLEYS.containsKey(player.getUuid()) ? 300 : 20);
		}
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = VOLLEYS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> e = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			if (server.getTicks() % 2 != 0) {
				continue;
			}
			int left = e.getValue();
			// spears from a ring behind the head, one after another, at whatever the player looks at now
			double a = left * 0.9;
			fire(player.getServerWorld(), player, overHead(player, Math.cos(a), 0.4 + 0.4 * Math.sin(a)), 7.0f);
			Fx.sound(player.getServerWorld(), player, SoundEvents.ITEM_TRIDENT_THROW.value(), 0.7f, 1.6f);
			if (left <= 1) {
				it.remove();
			} else {
				e.setValue(left - 1);
			}
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.AQUA));
	}
}
