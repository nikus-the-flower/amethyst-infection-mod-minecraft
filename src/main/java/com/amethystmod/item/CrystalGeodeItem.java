package com.amethystmod.item;

import com.amethystmod.CoreManager;
import com.amethystmod.ModParticles;
import com.amethystmod.world.StoryState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * A crystal geode: the seed of an infection core. The cores throw them by themselves; this item is for
 * creative play and testing. Used on the ground in the normal world it plants a core of its level.
 */
public class CrystalGeodeItem extends Item {
	public CrystalGeodeItem(Settings settings) {
		super(settings);
	}

	public static ItemStack stack(int phase, int count) {
		ItemStack stack = new ItemStack(ModItems.CRYSTAL_GEODE, count);
		stack.set(ModComponents.CORE_PHASE, phase);
		return stack;
	}

	public static int phase(ItemStack stack) {
		return Math.max(1, Math.min(3, stack.getOrDefault(ModComponents.CORE_PHASE, 1)));
	}

	@Override
	public Text getName(ItemStack stack) {
		return Text.translatable("item.amethystmod.crystal_geode.level", Text.translatable(getTranslationKey()), phase(stack));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		if (!(world instanceof ServerWorld server)) {
			return ActionResult.SUCCESS;
		}
		if (world.getRegistryKey() != World.OVERWORLD) {
			if (player != null) {
				player.sendMessage(Text.translatable("message.amethystmod.geode.overworld").formatted(Formatting.GRAY), true);
			}
			return ActionResult.FAIL;
		}
		if (StoryState.infectionStopped) {
			if (player != null) {
				player.sendMessage(Text.translatable("message.amethystmod.geode.dead").formatted(Formatting.GRAY), true);
			}
			return ActionResult.FAIL;
		}
		BlockPos pos = context.getSide() == Direction.UP ? context.getBlockPos().up() : context.getBlockPos().offset(context.getSide());
		ItemStack stack = context.getStack();
		CoreManager.plant(server, pos, phase(stack));
		server.spawnParticles(ModParticles.PRISM_FLARE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.BLOCKS, 1.5f, 0.6f);
		if (player == null || !player.isCreative()) {
			stack.decrement(1);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.amethystmod.crystal_geode.tooltip").formatted(Formatting.GRAY));
	}
}
