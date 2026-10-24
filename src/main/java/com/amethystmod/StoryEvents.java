package com.amethystmod;

import com.amethystmod.entity.AmitarielEntity;
import com.amethystmod.entity.ModEntities;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

/** Chat answers to Amitariel: "yes" ends your life, "no" starts the fight. */
public class StoryEvents {
	public static void register() {
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> onChat(sender, message.getSignedContent()));
	}

	private static void onChat(ServerPlayerEntity sender, String text) {
		ServerWorld world = sender.getServerWorld();
		Box box = sender.getBoundingBox().expand(48);
		for (AmitarielEntity apostle : world.getEntitiesByType(ModEntities.AMITARIEL, box, e -> true)) {
			apostle.onAnswer(sender, text);
		}
	}
}
