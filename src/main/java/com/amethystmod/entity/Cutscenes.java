package com.amethystmod.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Short staged scenes played by the server, without any client code: the players are held in place,
 * their view turns smoothly toward the boss, a big title appears and the camera shakes on impacts.
 * Also camera shake on its own, for heavy blows.
 */
public final class Cutscenes {
	private static final class State {
		Vec3d anchor;
		Entity focus;
		int ticks;
		float shake;
		int shakeTicks;
		float offsetYaw;
		float offsetPitch;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private Cutscenes() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Cutscenes::tick);
	}

	private static State state(ServerPlayerEntity player) {
		return STATES.computeIfAbsent(player.getUuid(), id -> new State());
	}

	/** Holds the players for {@code ticks}, turning their view toward {@code focus}, with a title. */
	public static void play(List<ServerPlayerEntity> players, Entity focus, int ticks, Text title, Text subtitle) {
		for (ServerPlayerEntity player : players) {
			if (player.networkHandler == null || player.getClass() != ServerPlayerEntity.class) {
				continue;
			}
			State s = state(player);
			s.anchor = player.getPos();
			s.focus = focus;
			s.ticks = ticks;
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks + 10, 4, false, false, false));
			if (title != null) {
				player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, Math.max(20, ticks - 30), 20));
				player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle == null ? Text.empty()
						: subtitle.copy().formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC)));
				player.networkHandler.sendPacket(new TitleS2CPacket(title.copy().formatted(Formatting.DARK_PURPLE, Formatting.BOLD)));
			}
		}
	}

	/** Shakes the view of every player within {@code radius} of {@code center}. */
	public static void shake(ServerWorld world, Vec3d center, double radius, float strength, int ticks) {
		for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(center) <= radius * radius)) {
			State s = state(player);
			s.shake = Math.max(s.shake, strength);
			s.shakeTicks = Math.max(s.shakeTicks, ticks);
		}
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, State> entry = it.next();
			State s = entry.getValue();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || (s.ticks <= 0 && s.shakeTicks <= 0)) {
				it.remove();
				continue;
			}
			// undo last tick's shake before applying a new one, so the view never drifts
			float jitterYaw = 0;
			float jitterPitch = 0;
			if (s.shakeTicks > 0) {
				s.shakeTicks--;
				float amount = s.shake * Math.min(1.0f, s.shakeTicks / 6.0f);
				jitterYaw = (player.getRandom().nextFloat() - 0.5f) * 2 * amount;
				jitterPitch = (player.getRandom().nextFloat() - 0.5f) * 2 * amount;
			}
			float dyaw = jitterYaw - s.offsetYaw;
			float dpitch = jitterPitch - s.offsetPitch;
			s.offsetYaw = jitterYaw;
			s.offsetPitch = jitterPitch;

			if (s.ticks > 0 && s.focus != null && s.focus.isAlive()) {
				s.ticks--;
				Vec3d eye = player.getEyePos();
				Vec3d look = s.focus.getPos().add(0, s.focus.getHeight() * 0.6, 0).subtract(eye);
				float wantYaw = (float) (MathHelper.atan2(look.z, look.x) * 57.2957795) - 90.0f;
				float wantPitch = (float) -(MathHelper.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 57.2957795);
				float yaw = player.getYaw() + MathHelper.wrapDegrees(wantYaw - player.getYaw()) * 0.18f + dyaw;
				float pitch = player.getPitch() + (wantPitch - player.getPitch()) * 0.18f + dpitch;
				player.networkHandler.requestTeleport(s.anchor.x, s.anchor.y, s.anchor.z, yaw, pitch);
				player.setVelocity(Vec3d.ZERO);
			} else if (dyaw != 0 || dpitch != 0) {
				s.ticks = 0;
				// requestTeleport takes absolute values even with relative flags (flags only shape the packet)
				player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(),
						player.getYaw() + dyaw, player.getPitch() + dpitch, EnumSet.allOf(PositionFlag.class));
			}
		}
	}
}
