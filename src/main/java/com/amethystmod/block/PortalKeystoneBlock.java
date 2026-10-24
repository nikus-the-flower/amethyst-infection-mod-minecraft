package com.amethystmod.block;

import com.amethystmod.item.ModItems;
import com.amethystmod.world.AmethystWorld;
import com.amethystmod.world.PortalFrames;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The bottom-centre block of a portal frame: put the portal core in and the frame opens. The core stays in. */
public class PortalKeystoneBlock extends Block {
	public static final BooleanProperty LIT = Properties.LIT;

	public PortalKeystoneBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(LIT, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
											 PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (state.get(LIT) || !stack.isOf(ModItems.PORTAL_CORE)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (world.getRegistryKey() == AmethystWorld.WORLD) {
			if (!world.isClient) {
				player.sendMessage(Text.translatable("message.amethystmod.portal.sealed").formatted(Formatting.LIGHT_PURPLE), true);
			}
			return ItemActionResult.FAIL;
		}
		if (!world.isClient) {
			world.setBlockState(pos, state.with(LIT, true));
			PortalFrames.fill(world, pos);
			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
			}
			world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0f, 1.3f);
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 2.0f, 0.6f);
		}
		return ItemActionResult.success(world.isClient);
	}
}
