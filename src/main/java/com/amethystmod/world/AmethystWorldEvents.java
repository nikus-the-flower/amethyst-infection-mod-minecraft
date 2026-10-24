package com.amethystmod.world;

import com.amethystmod.ModParticles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Weather of the Amethyst world: when it would rain, crystal shards drift down instead. */
public final class AmethystWorldEvents {
	private AmethystWorldEvents() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getRegistryKey() != AmethystWorld.WORLD || world.getTime() % 2 != 0 || !world.getServer().getOverworld().isRaining()) {
				return;
			}
			crystalRain(world);
		});
	}

	private static void crystalRain(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			world.spawnParticles(player, ModParticles.CRYSTAL_RAIN, false, player.getX(), player.getY() + 12, player.getZ(),
					14, 14.0, 4.0, 14.0, 0.0);
			if (world.random.nextInt(60) == 0) {
				world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.WEATHER,
						0.6f, 0.6f + world.random.nextFloat() * 0.8f);
			}
		}
	}
}
