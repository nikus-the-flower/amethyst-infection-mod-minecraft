package com.amethystmod.entity;

import com.amethystmod.world.AmethystWorld;
import com.amethystmod.world.StoryState;
import com.amethystmod.world.TempleDesign;
import com.amethystmod.world.TowerDesign;
import com.amethystmod.world.TowerState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

/**
 * Keeps the rifts where the story opened them: in Amitariel's hall once he has fallen (leading to the
 * world of the Amethyst), and in the temple once the Heart is destroyed (leading home). Rifts are not
 * saved; whenever the place is loaded and someone is near, the rift is there.
 */
public final class RiftManager {
	private RiftManager() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 == 0) {
				tick(server);
			}
		});
	}

	private static void tick(MinecraftServer server) {
		StoryState story = StoryState.get(server);
		ServerWorld overworld = server.getOverworld();
		TowerState tower = TowerState.get(overworld);
		if (story.amitarielDefeated && tower.built) {
			ensure(overworld, towerRift(tower), false);
		}
		ServerWorld amethyst = server.getWorld(AmethystWorld.WORLD);
		if (amethyst != null) {
			// temples built before the rifts still have the old portal frame
			BlockPos key = TempleDesign.portalKeystone();
			if (amethyst.isChunkLoaded(ChunkPos.toLong(key)) && amethyst.getBlockState(key).isOf(com.amethystmod.block.ModBlocks.PORTAL_KEYSTONE)) {
				com.amethystmod.world.PortalFrames.remove(amethyst, key);
			}
			if (story.heartDefeated) {
				ensure(amethyst, templeRift(), false);
			}
		}
	}

	public static Vec3d towerRift(TowerState tower) {
		return Vec3d.ofBottomCenter(new TowerDesign(tower.baseY).riftPos());
	}

	public static Vec3d templeRift() {
		return Vec3d.ofBottomCenter(TempleDesign.heartHome());
	}

	/** Makes sure a rift stands at {@code pos}; {@code tear} plays the opening scene for a new one. */
	public static void ensure(ServerWorld world, Vec3d pos, boolean tear) {
		BlockPos block = BlockPos.ofFloored(pos);
		if (!world.isChunkLoaded(ChunkPos.toLong(block)) || (!tear && BossFx.playersNear(world, pos, 64).isEmpty())) {
			return;
		}
		if (!world.getEntitiesByType(ModEntities.RIFT, new Box(block).expand(3), e -> true).isEmpty()) {
			return;
		}
		RiftEntity rift = ModEntities.RIFT.create(world);
		if (rift == null) {
			return;
		}
		rift.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
		if (tear) {
			rift.tearOpen();
			world.playSound(null, block, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.8f);
			world.playSound(null, block, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 1.5f, 0.5f);
			Cutscenes.shake(world, pos, 40, 1.5f, 30);
		}
		world.spawnEntity(rift);
	}

	/** Amitariel has fallen: a rift tears open in the middle of his hall. */
	public static void openTowerRift(ServerWorld world) {
		StoryState.markAmitarielDefeated(world.getServer());
		TowerState tower = TowerState.get(world.getServer().getOverworld());
		if (tower.built && world == world.getServer().getOverworld()) {
			ensure(world, towerRift(tower), true);
			announce(world, towerRift(tower), "message.amethystmod.rift.tower");
		}
	}

	/** The Heart is destroyed: a rift home tears open in the temple. */
	public static void openTempleRift(ServerWorld world) {
		ensure(world, templeRift(), true);
		announce(world, templeRift(), "message.amethystmod.rift.temple");
	}

	private static void announce(ServerWorld world, Vec3d pos, String key) {
		Text text = Text.translatable(key).formatted(Formatting.LIGHT_PURPLE);
		for (var player : world.getPlayers(p -> p.squaredDistanceTo(pos) < 80 * 80)) {
			player.sendMessage(text, false);
		}
	}
}
