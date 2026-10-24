package com.amethystmod.block;

import com.amethystmod.ModParticles;
import com.amethystmod.world.AmethystWorld;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Portal;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/** The pulsing violet veil inside an activated amethyst portal frame. */
public class AmethystPortalBlock extends Block implements Portal {
	public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
	private static final VoxelShape X_SHAPE = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
	private static final VoxelShape Z_SHAPE = Block.createCuboidShape(6, 0, 0, 10, 16, 16);

	public AmethystPortalBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(AXIS, Direction.Axis.X));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
	}

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity.canUsePortals(false)) {
			entity.tryUsePortal(this, pos);
		}
	}

	@Override
	public int getPortalDelay(ServerWorld world, Entity entity) {
		if (entity instanceof PlayerEntity player) {
			return player.getAbilities().invulnerable ? 1 : 50;
		}
		return 0;
	}

	@Override
	public TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos) {
		return AmethystWorld.portalTarget(world, entity);
	}

	@Override
	public Portal.Effect getPortalEffect() {
		return Portal.Effect.CONFUSION;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		for (int i = 0; i < 2; i++) {
			world.addParticle(ModParticles.CRYSTAL_SPARK, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
		}
	}

	@Override
	public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}
}
