package com.amethystmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The old altar of the sunken ruins: a pedestal for a relic, or (center = true) the altar itself, on
 * which purified things are made. Right-click with an item to lay it down, with an empty hand to take it.
 */
public class AltarBlock extends BlockWithEntity {
	private static final VoxelShape PEDESTAL = VoxelShapes.union(
			createCuboidShape(2, 0, 2, 14, 3, 14), createCuboidShape(4, 3, 4, 12, 11, 12), createCuboidShape(3, 11, 3, 13, 14, 13));
	private static final VoxelShape ALTAR = VoxelShapes.union(
			createCuboidShape(0, 0, 0, 16, 4, 16), createCuboidShape(2, 4, 2, 14, 10, 14), createCuboidShape(1, 10, 1, 15, 14, 15));

	private final boolean center;

	public AltarBlock(AbstractBlock.Settings settings, boolean center) {
		super(settings);
		this.center = center;
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return createCodec(s -> new AltarBlock(s, center));
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return center ? ALTAR : PEDESTAL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return center ? AltarBlockEntity.altar(pos, state) : AltarBlockEntity.pedestal(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (!center || world.isClient) {
			return null;
		}
		return validateTicker(type, ModBlockEntities.PURIFICATION_ALTAR, AltarBlockEntity::serverTick);
	}

	private static void say(PlayerEntity player, String key) {
		player.sendMessage(Text.translatable(key).formatted(Formatting.LIGHT_PURPLE), true);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player,
											 Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof AltarBlockEntity be) || !be.getStack().isEmpty()) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!center) {
			if (!AltarBlockEntity.isRelic(stack)) {
				if (!world.isClient) {
					say(player, "message.amethystmod.altar.pedestal");
				}
				return ItemActionResult.FAIL;
			}
			if (!world.isClient) {
				be.setStack(stack.copyWithCount(1));
				stack.decrementUnlessCreative(1, player);
				world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
				// the last relic in place: the altar nearby wakes up
				for (BlockPos p : BlockPos.iterate(pos.add(-AltarBlockEntity.REACH, -1, -AltarBlockEntity.REACH),
						pos.add(AltarBlockEntity.REACH, 1, AltarBlockEntity.REACH))) {
					if (world.getBlockEntity(p) instanceof AltarBlockEntity a && a.getType() == ModBlockEntities.PURIFICATION_ALTAR
							&& AltarBlockEntity.awake(world, p)) {
						world.playSound(null, p, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.5f, 0.8f);
						world.playSound(null, p, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.0f);
						say(player, "message.amethystmod.altar.awake");
					}
				}
			}
			return ItemActionResult.success(world.isClient);
		}
		if (AltarBlockEntity.purified(stack.getItem()) == null) {
			if (!world.isClient) {
				say(player, "message.amethystmod.altar.cannot");
			}
			return ItemActionResult.FAIL;
		}
		if (!AltarBlockEntity.awake(world, pos)) {
			if (!world.isClient) {
				say(player, "message.amethystmod.altar.asleep");
			}
			return ItemActionResult.FAIL;
		}
		if (!world.isClient) {
			ItemStack laid = player.getAbilities().creativeMode ? stack.copy() : stack.split(stack.getCount());
			be.beginRitual(laid);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof AltarBlockEntity be) || be.getStack().isEmpty() || be.inRitual()) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			ItemStack taken = be.getStack();
			be.setStack(ItemStack.EMPTY);
			if (!player.giveItemStack(taken)) {
				player.dropItem(taken, false);
			}
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.0f);
		}
		return ActionResult.success(world.isClient);
	}
}
