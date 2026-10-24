package com.amethystmod.item;

import com.amethystmod.ModParticles;
import com.amethystmod.entity.SlashEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Crescent Blade. Hold right-click and let go: crescents of light fly out from the blade. The
 * longer it is held (up to three seconds), the more crescents (up to five), the bigger and the harder.
 */
public class CrescentBladeItem extends SwordItem {
	public static final int FULL = 60;

	public CrescentBladeItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this)) {
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
		if (world instanceof ServerWorld serverWorld && used <= FULL) {
			if (used % 15 == 0 && used > 0) {
				// each chime is one more crescent
				Fx.sound(serverWorld, user, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f + used / (float) FULL);
				Fx.particles(serverWorld, ModParticles.CRYSTAL_SPARK, user.getPos().add(0, 1.2, 0), 4 + used / 10, 0.5, 0.05);
			}
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof PlayerEntity player)) {
			return;
		}
		int used = Math.min(FULL, getMaxUseTime(stack, user) - remainingUseTicks);
		int count = 1 + used / 15;
		float power = used / (float) FULL;
		float damage = 6.0f + 6.0f * power;
		float size = 1.4f + 0.9f * power;
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d from = player.getEyePos().add(0, -0.35, 0).add(look.multiply(0.8));
		for (int i = 0; i < count; i++) {
			double spread = Math.toRadians((i - (count - 1) / 2.0) * 9.0);
			Vec3d dir = look.rotateY((float) spread);
			float roll = (float) Math.toRadians((i % 2 == 0 ? 1 : -1) * (10 + 12 * (i / 2)));
			SlashEntity.spawn(serverWorld, player, from, dir, size, roll, damage);
		}
		player.swingHand(player.getActiveHand(), true);
		Fx.sound(serverWorld, player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.1f - power * 0.4f);
		Fx.sound(serverWorld, player, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 1.4f - power * 0.5f);
		player.getItemCooldownManager().set(this, 20 + used / 2);
		stack.damage(1, player, player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
