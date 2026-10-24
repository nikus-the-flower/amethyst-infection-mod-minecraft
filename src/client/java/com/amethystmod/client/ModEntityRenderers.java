package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class ModEntityRenderers {
	public static void register() {
		geo(ModEntities.STALKER, "amethyst_stalker", 0.9f, 1.15f);
		geo(ModEntities.WEAVER, "crystal_weaver", 0.8f, 1.2f);
		geo(ModEntities.WALKER, "shard_walker", 0.9f, 1.0f);
		geo(ModEntities.LURKER, "void_lurker", 0.6f, 1.1f);
		geo(ModEntities.COLOSSUS, "geode_colossus", 1.8f, 1.0f);
		geo(ModEntities.DRIFTER, "celestial_drifter", 0.8f, 1.0f);
		{
			GeoDef def = GeoDef.load("amitariel");
			EntityModelLayer layer = new EntityModelLayer(Identifier.of(AmethystMod.MOD_ID, "amitariel"), "main");
			EntityModelLayerRegistry.registerModelLayer(layer, def::toModelData);
			EntityRendererRegistry.register(ModEntities.AMITARIEL, ctx -> new GeoRenderer<>(ctx, def, layer,
					e -> e.getPhase() == 2 ? "amitariel_p2" : "amitariel", 0.7f, 1.0f));
		}
		{
			GeoDef def = GeoDef.load("amethyst_heart");
			EntityModelLayer layer = new EntityModelLayer(Identifier.of(AmethystMod.MOD_ID, "amethyst_heart"), "main");
			EntityModelLayerRegistry.registerModelLayer(layer, def::toModelData);
			EntityRendererRegistry.register(ModEntities.HEART, ctx -> new GeoRenderer<>(ctx, def, layer,
					e -> e.getStage() <= 1 ? "amethyst_heart" : "amethyst_heart_p" + e.getStage(), 4.0f, 4.0f));
		}
		geo(ModEntities.HEART_PYLON, "heart_pylon", 1.0f, 1.0f);
		geo(ModEntities.RICARDO, "ricardo", 0.5f, 1.0f);
		geo(ModEntities.HEART_SENTINEL, "heart_sentinel", 0.6f, 1.0f);
		{
			GeoDef def = GeoDef.load("infection_core");
			EntityModelLayer layer = new EntityModelLayer(Identifier.of(AmethystMod.MOD_ID, "infection_core"), "main");
			EntityModelLayerRegistry.registerModelLayer(layer, def::toModelData);
			EntityRendererRegistry.register(ModEntities.INFECTION_CORE, ctx -> new GeoRenderer<>(ctx, def, layer,
					e -> e.isDying() ? "infection_core_dim" : "infection_core", 0.9f, 1.0f));
		}
		EntityRendererRegistry.register(ModEntities.HEART_NODE, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
		EntityModelLayer shardLayer = armorLayer("shard_bolt");
		EntityRendererRegistry.register(ModEntities.CRYSTAL_SHARD, ctx -> new ShardRenderer(ctx, shardLayer));
		EntityModelLayer spikeLayer = armorLayer("crystal_spike");
		EntityModelLayer runeLayer = armorLayer("rune_ring");
		EntityRendererRegistry.register(ModEntities.CRYSTAL_SPIKE, ctx -> new CrystalEffectRenderer(ctx, spikeLayer, "crystal_spike", false));
		EntityRendererRegistry.register(ModEntities.RUNE_CIRCLE, ctx -> new CrystalEffectRenderer(ctx, runeLayer, "rune_ring", true));
		EntityRendererRegistry.register(ModEntities.FALLING_CRYSTAL, ctx -> new CrystalEffectRenderer(ctx, spikeLayer, "crystal_spike", false, true));
		EntityRendererRegistry.register(ModEntities.BEAM_EFFECT, BeamEffectRenderer::new);
		EntityRendererRegistry.register(ModEntities.RIFT, RiftRenderer::new);
		EntityRendererRegistry.register(ModEntities.SLASH, SlashRenderer::new);
		EntityModelLayer spearLayer = armorLayer("phantom_spear");
		EntityRendererRegistry.register(ModEntities.PHANTOM_SPEAR, ctx -> new PhantomSpearRenderer(ctx, spearLayer));
		EntityRendererRegistry.register(ModEntities.CRYSTAL_ARROW, CrystalArrowRenderer::new);

		// 3D growths on the armor sets
		EntityModelLayer amethystLayer = armorLayer("armor_amethyst");
		EntityModelLayer purifiedLayer = armorLayer("armor_purified");
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
				helper.register(new ArmorExtras(playerRenderer, context.getPart(amethystLayer), context.getPart(purifiedLayer)));
			}
		});
	}

	private static EntityModelLayer armorLayer(String name) {
		GeoDef def = GeoDef.load(name);
		EntityModelLayer layer = new EntityModelLayer(Identifier.of(AmethystMod.MOD_ID, name), "main");
		EntityModelLayerRegistry.registerModelLayer(layer, def::toModelData);
		return layer;
	}

	private static <T extends MobEntity> void geo(EntityType<T> type, String name, float shadow, float scale) {
		GeoDef def = GeoDef.load(name);
		EntityModelLayer layer = new EntityModelLayer(Identifier.of(AmethystMod.MOD_ID, name), "main");
		EntityModelLayerRegistry.registerModelLayer(layer, def::toModelData);
		EntityRendererRegistry.register(type, ctx -> new GeoRenderer<>(ctx, def, layer, e -> name, shadow, scale));
	}
}
