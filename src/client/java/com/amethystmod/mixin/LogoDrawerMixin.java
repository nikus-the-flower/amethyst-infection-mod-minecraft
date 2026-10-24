package com.amethystmod.mixin;

import com.amethystmod.AmethystMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the mod's own logo on the main menu instead of the Minecraft logo and the edition label. */
@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {
	/** tools/menu_src/logo_glass.png (glassy pink) or logo_gold.png (golden): which one the menu shows. */
	private static final Identifier LOGO = Identifier.of(AmethystMod.MOD_ID, "textures/gui/menu/logo_glass.png");
	private static final int TEXTURE_WIDTH = 1000;
	private static final int TEXTURE_HEIGHT = 302;
	private static final int WIDTH = 272;
	private static final int HEIGHT = WIDTH * TEXTURE_HEIGHT / TEXTURE_WIDTH;

	@Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("HEAD"), cancellable = true)
	private void amethystmod$drawLogo(DrawContext context, int screenWidth, float alpha, int y, CallbackInfo ci) {
		context.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		context.drawTexture(LOGO, screenWidth / 2 - WIDTH / 2, y - 6, WIDTH, HEIGHT, 0.0f, 0.0f, TEXTURE_WIDTH, TEXTURE_HEIGHT,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);
		context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		ci.cancel();
	}
}
