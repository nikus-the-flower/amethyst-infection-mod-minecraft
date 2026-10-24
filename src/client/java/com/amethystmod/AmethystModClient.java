package com.amethystmod;

import com.amethystmod.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ArmorItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AmethystModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.CRYSTALS.toArray(new Block[0]));
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INFECTED_LEAVES, RenderLayer.getCutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(),
				ModBlocks.CLEAR_AMETHYST, ModBlocks.CLEAR_AMETHYST_PANE, ModBlocks.AMETHYST_PORTAL);

		com.amethystmod.client.ModEntityRenderers.register();
		com.amethystmod.client.PurityHud.register();
		com.amethystmod.item.LoreItem.opener = com.amethystmod.client.LoreScreen::open;
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(com.amethystmod.block.ModBlockEntities.ALTAR_PEDESTAL,
				com.amethystmod.client.AltarRenderer::new);
		net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(com.amethystmod.block.ModBlockEntities.PURIFICATION_ALTAR,
				com.amethystmod.client.AltarRenderer::new);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ModNetworking.RicardoDialog.ID,
				(payload, context) -> context.client().setScreen(
						new com.amethystmod.client.RicardoScreen(payload.entityId(), payload.altarAwake(), payload.gifted())));

		// /amethystpanorama: takes the six pictures of a panorama (for the main menu background) from where you stand,
		// into <game folder>/screenshots/panorama/. Hide the HUD first (F1) and stand still.
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("amethystpanorama")
						.executes(ctx -> {
							net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
							client.send(() -> {
								Text result = client.takePanorama(client.runDirectory, 1024, 1024);
								if (client.player != null) {
									client.player.sendMessage(result, false);
								}
							});
							return 1;
						})));
		com.amethystmod.item.GuideBookItem.opener = com.amethystmod.client.GuideScreen::open;

		ModelPredicateProviderRegistry.register(com.amethystmod.item.ModItems.AMETHYST_SHIELD, Identifier.ofVanilla("blocking"),
				(stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0f : 0.0f);
		ModelPredicateProviderRegistry.register(com.amethystmod.item.ModItems.CRYSTAL_BOW, Identifier.ofVanilla("pull"),
				(stack, world, entity, seed) -> entity == null || entity.getActiveItem() != stack ? 0.0f
						: (stack.getMaxUseTime(entity) - entity.getItemUseTimeLeft()) / 20.0f);
		ModelPredicateProviderRegistry.register(com.amethystmod.item.ModItems.CRYSTAL_BOW, Identifier.ofVanilla("pulling"),
				(stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0f : 0.0f);

		ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
		particles.register(ModParticles.CRYSTAL_SPARK, EndRodParticle.Factory::new);
		particles.register(ModParticles.WARD_MOTE, EndRodParticle.Factory::new);
		particles.register(ModParticles.BLIGHT_SPORE, EndRodParticle.Factory::new);
		particles.register(ModParticles.PRISM_FLARE, EndRodParticle.Factory::new);
		particles.register(ModParticles.CRYSTAL_RAIN, net.minecraft.client.particle.SnowflakeParticle.Factory::new);

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (stack.getItem() instanceof ArmorItem armor) {
				String set = armor.getMaterial().getKey().map(k -> k.getValue()).filter(id -> id.getNamespace().equals(AmethystMod.MOD_ID))
						.map(id -> id.getPath()).orElse(null);
				if (set != null) {
					lines.add(Text.translatable("tooltip.amethystmod.set." + set).formatted(Formatting.LIGHT_PURPLE));
				}
			}
		});
	}
}
