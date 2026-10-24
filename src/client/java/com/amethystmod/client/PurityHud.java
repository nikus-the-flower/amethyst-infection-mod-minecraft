package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.item.ModItems;
import com.amethystmod.item.PurityAegisItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Draws the white hearts of the Aegis of Purity in a row above the health (and armour) bar. */
public final class PurityHud {
	private static final Identifier FULL = Identifier.of(AmethystMod.MOD_ID, "textures/gui/purity_heart_full.png");
	private static final Identifier HALF = Identifier.of(AmethystMod.MOD_ID, "textures/gui/purity_heart_half.png");
	private static final Identifier EMPTY = Identifier.of(AmethystMod.MOD_ID, "textures/gui/purity_heart_container.png");

	private PurityHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> draw(context));
	}

	private static void draw(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.options.hudHidden || client.interactionManager == null
				|| !client.interactionManager.hasStatusBars()) {
			return;
		}
		ItemStack aegis = ItemStack.EMPTY;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(ModItems.PURITY_AEGIS)) {
				aegis = player.getInventory().getStack(i);
				break;
			}
		}
		if (aegis.isEmpty() || PurityAegisItem.charge(aegis) <= 0) {
			return;
		}
		int health = PurityAegisItem.health(aegis);
		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();
		// the same rows the vanilla health bar uses, so the white hearts sit just above it (and the armour)
		int rows = MathHelper.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 2.0f / 10.0f);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		int y = height - 39 - (rows - 1) * rowHeight - 10;
		if (player.getArmor() > 0) {
			y -= 10;
		}
		int x = width / 2 - 91;
		for (int i = 0; i < PurityAegisItem.MAX_HEALTH / 2; i++) {
			int hx = x + i * 8;
			context.drawTexture(EMPTY, hx, y, 0, 0, 9, 9, 9, 9);
			if (health >= i * 2 + 2) {
				context.drawTexture(FULL, hx, y, 0, 0, 9, 9, 9, 9);
			} else if (health == i * 2 + 1) {
				context.drawTexture(HALF, hx, y, 0, 0, 9, 9, 9, 9);
			}
		}
	}
}
