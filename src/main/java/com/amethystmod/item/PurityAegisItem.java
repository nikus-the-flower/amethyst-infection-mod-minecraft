package com.amethystmod.item;

import com.amethystmod.ArtifactEvents;
import com.amethystmod.ModParticles;
import com.amethystmod.entity.BeamEffectEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Aegis of Purity. Charge it in the inventory: pick up purified amethyst and right-click the aegis
 * with it (one piece per click, ten for a full charge). While it is charged and anywhere in your
 * inventory, a see-through dome of light surrounds you and you get a second row of white hearts: they
 * take damage before your own health and regrow on their own when you are not hit for a while. Every
 * point of health they take or regrow uses up charge; when the charge runs out, the dome and the white
 * hearts are gone.
 */
public class PurityAegisItem extends Item {
	public static final int MAX_CHARGE = 100;
	public static final int PER_ESSENCE = 10;
	/** The white hearts: 16 points, eight hearts. */
	public static final int MAX_HEALTH = 16;
	private static final int REGROW_AFTER = 60;
	private static final int REGROW_EVERY = 12;

	private static final Map<UUID, BeamEffectEntity> DOMES = new HashMap<>();
	private static final Map<UUID, Integer> LAST_HIT = new HashMap<>();
	private static boolean absorbing;

	public PurityAegisItem(Settings settings) {
		super(settings);
	}

	public static int charge(ItemStack stack) {
		return stack.getOrDefault(ModComponents.SHIELD_CHARGE, 0);
	}

	public static int health(ItemStack stack) {
		return stack.getOrDefault(ModComponents.SHIELD_HEALTH, 0);
	}

	/** Pick up purified amethyst and right-click the aegis with it. */
	@Override
	public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursor) {
		if (clickType != ClickType.RIGHT || !otherStack.isOf(ModItems.PURIFIED_AMETHYST) || !slot.canTakePartial(player)) {
			return false;
		}
		int charge = charge(stack);
		if (charge >= MAX_CHARGE) {
			return true;
		}
		stack.set(ModComponents.SHIELD_CHARGE, Math.min(MAX_CHARGE, charge + PER_ESSENCE));
		otherStack.decrement(1);
		player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f + charge / 100.0f);
		return true;
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(13.0f * charge(stack) / MAX_CHARGE);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return 0x8FE6FF;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.AQUA));
		tooltip.add(Text.translatable(getTranslationKey() + ".charge", charge(stack), health(stack) / 2.0f)
				.formatted(Formatting.GRAY));
	}

	// ---- the shield ----------------------------------------------------------------------

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(PurityAegisItem::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(PurityAegisItem::absorb);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ItemStack aegis = ArtifactEvents.findArtifact(player, ModItems.PURITY_AEGIS);
			int charge = aegis.isEmpty() ? 0 : charge(aegis);
			int health = aegis.isEmpty() ? 0 : health(aegis);
			if (!aegis.isEmpty() && charge <= 0 && health > 0) {
				aegis.set(ModComponents.SHIELD_HEALTH, 0);
				health = 0;
			}
			// the white hearts regrow while you are left alone, each point costing charge
			if (charge > 0 && health < MAX_HEALTH && player.age - LAST_HIT.getOrDefault(player.getUuid(), -1000) > REGROW_AFTER
					&& player.age % REGROW_EVERY == 0) {
				health++;
				aegis.set(ModComponents.SHIELD_HEALTH, health);
				aegis.set(ModComponents.SHIELD_CHARGE, charge - 1);
			}
			dome(player, health > 0);
		}
	}

	/** Keeps the dome around a player while the white hearts last. */
	private static void dome(ServerPlayerEntity player, boolean up) {
		BeamEffectEntity dome = DOMES.get(player.getUuid());
		if (up) {
			if (dome == null || dome.isRemoved() || dome.getWorld() != player.getWorld()) {
				dome = BeamEffectEntity.dome(player.getServerWorld(), player, 1.5f, 10, BeamEffectEntity.PURE);
				DOMES.put(player.getUuid(), dome);
			}
			if (dome != null) {
				dome.keepAlive(10);
			}
		} else if (dome != null) {
			dome.discard();
			DOMES.remove(player.getUuid());
		}
	}

	/** The white hearts take the blow first; whatever is left over goes on to the player. */
	private static boolean absorb(LivingEntity entity, DamageSource source, float amount) {
		if (absorbing || !(entity instanceof ServerPlayerEntity player) || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| player.timeUntilRegen > 10 || amount <= 0) {
			return true;
		}
		ItemStack aegis = ArtifactEvents.findArtifact(player, ModItems.PURITY_AEGIS);
		int health = aegis.isEmpty() ? 0 : health(aegis);
		if (health <= 0) {
			return true;
		}
		float taken = Math.min(amount, health);
		int cost = MathHelper.ceil(taken);
		aegis.set(ModComponents.SHIELD_HEALTH, Math.max(0, health - cost));
		aegis.set(ModComponents.SHIELD_CHARGE, Math.max(0, charge(aegis) - cost));
		LAST_HIT.put(player.getUuid(), player.age);
		player.timeUntilRegen = 20;
		ServerWorld world = player.getServerWorld();
		world.spawnParticles(ModParticles.WARD_MOTE, player.getX(), player.getY() + 1, player.getZ(), 10, 0.6, 0.6, 0.6, 0.05);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.2f,
				health - cost <= 0 ? 0.6f : 1.4f);
		if (health - cost <= 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.3f);
		}
		float rest = amount - taken;
		if (rest > 0.01f) {
			absorbing = true;
			try {
				player.timeUntilRegen = 0;
				player.damage(source, rest);
			} finally {
				absorbing = false;
			}
		}
		return false;
	}
}
