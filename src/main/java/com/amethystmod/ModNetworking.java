package com.amethystmod;

import com.amethystmod.entity.RicardoEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** The two messages of a talk with Ricardo: the server opens the dialogue, the client sends what was chosen. */
public final class ModNetworking {
	/** Server to client: open the dialogue with Ricardo (entity id), and where the story stands. */
	public record RicardoDialog(int entityId, boolean altarAwake, boolean gifted) implements CustomPayload {
		public static final Id<RicardoDialog> ID = new Id<>(Identifier.of(AmethystMod.MOD_ID, "ricardo_dialog"));
		public static final PacketCodec<RegistryByteBuf, RicardoDialog> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, RicardoDialog::entityId, PacketCodecs.BOOL, RicardoDialog::altarAwake,
				PacketCodecs.BOOL, RicardoDialog::gifted, RicardoDialog::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Client to server: an action chosen in the dialogue ("trade", "gift"). */
	public record RicardoAction(int entityId, String action) implements CustomPayload {
		public static final Id<RicardoAction> ID = new Id<>(Identifier.of(AmethystMod.MOD_ID, "ricardo_action"));
		public static final PacketCodec<RegistryByteBuf, RicardoAction> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, RicardoAction::entityId, PacketCodecs.STRING, RicardoAction::action, RicardoAction::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(RicardoDialog.ID, RicardoDialog.CODEC);
		PayloadTypeRegistry.playC2S().register(RicardoAction.ID, RicardoAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RicardoAction.ID, (payload, context) -> {
			if (context.player().getWorld().getEntityById(payload.entityId()) instanceof RicardoEntity ricardo) {
				ricardo.act(context.player(), payload.action());
			}
		});
	}
}
