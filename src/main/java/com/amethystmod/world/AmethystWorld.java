package com.amethystmod.world;

import com.amethystmod.AmethystMod;
import com.amethystmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

/** The world of the Amethyst: where portals lead and where you land. */
public final class AmethystWorld {
	public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(AmethystMod.MOD_ID, "amethyst"));
	/** Travellers land this far (in blocks) from the temple at 0,0. */
	private static final int ARRIVAL_MIN = 150;
	private static final int ARRIVAL_MAX = 500;

	private AmethystWorld() {
	}

	public static TeleportTarget portalTarget(ServerWorld from, Entity entity) {
		MinecraftServer server = from.getServer();
		TeleportTarget.PostDimensionTransition after = TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET);
		if (from.getRegistryKey() == WORLD) {
			ServerWorld overworld = server.getOverworld();
			return new TeleportTarget(overworld, towerReturn(overworld), Vec3d.ZERO, 180.0f, 0.0f, after);
		}
		ServerWorld target = server.getWorld(WORLD);
		if (target == null) {
			return null;
		}
		Vec3d arrival = findArrival(target, entity.getRandom());
		return new TeleportTarget(target, arrival, Vec3d.ZERO, entity.getYaw(), 0.0f, after.then(e -> {
			if (e instanceof ServerPlayerEntity player) {
				player.sendMessage(Text.translatable("message.amethystmod.world.arrival").formatted(Formatting.LIGHT_PURPLE), false);
				target.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.AMBIENT, 2.0f, 0.5f);
			}
		}));
	}

	/** In Amitariel's hall, facing the rift, or the world spawn if the tower is missing. */
	public static Vec3d towerReturn(ServerWorld overworld) {
		TowerState state = TowerState.get(overworld);
		if (state.built) {
			TowerDesign design = new TowerDesign(state.baseY);
			return Vec3d.ofBottomCenter(design.arrivalPos());
		}
		BlockPos spawn = overworld.getSpawnPos();
		return Vec3d.ofBottomCenter(overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn));
	}

	/** A safe spot on solid ground somewhere in the ring around the temple. */
	public static Vec3d findArrival(ServerWorld world, Random random) {
		int x = 0;
		int z = 0;
		for (int attempt = 0; attempt < 24; attempt++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = ARRIVAL_MIN + random.nextDouble() * (ARRIVAL_MAX - ARRIVAL_MIN);
			x = (int) Math.round(Math.cos(a) * r);
			z = (int) Math.round(Math.sin(a) * r);
			world.getChunk(x >> 4, z >> 4);
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (y <= world.getBottomY() + 4 || y >= world.getTopY() - 4) {
				continue;
			}
			BlockPos ground = new BlockPos(x, y - 1, z);
			BlockState below = world.getBlockState(ground);
			if (below.isSolidBlock(world, ground) && below.getFluidState().isEmpty()
					&& world.getBlockState(ground.up()).isAir() && world.getBlockState(ground.up(2)).isAir()) {
				return new Vec3d(x + 0.5, y, z + 0.5);
			}
		}
		// nothing suitable: make a small landing platform in the air
		int y = 100;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				world.setBlockState(new BlockPos(x + dx, y - 1, z + dz), ModBlocks.AMETHYST_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
				for (int h = 0; h < 3; h++) {
					world.setBlockState(new BlockPos(x + dx, y + h, z + dz), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
		return new Vec3d(x + 0.5, y, z + 0.5);
	}
}
