package com.amethystmod;

import com.amethystmod.block.ModBlocks;
import com.amethystmod.effect.ModEffects;
import com.amethystmod.entity.ModEntities;
import com.amethystmod.item.ModArmorMaterials;
import com.amethystmod.item.ModItemGroups;
import com.amethystmod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmethystMod implements ModInitializer {
	public static final String MOD_ID = "amethystmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		com.amethystmod.item.ModComponents.register();
		ModBlocks.register();
		com.amethystmod.block.ModBlockEntities.register();
		ModParticles.register();
		ModEffects.register();
		ModEntities.register();
		ModArmorMaterials.register();
		ModItems.register();
		ModItemGroups.register();
		ArtifactEvents.register();
		StoryEvents.register();
		com.amethystmod.world.TowerBuilder.register();
		com.amethystmod.world.StoryState.register();
		com.amethystmod.world.TempleBuilder.register();
		com.amethystmod.world.AmethystWorldEvents.register();
		com.amethystmod.world.feature.AmethystFeatures.register();
		SelfTest.register();
		com.amethystmod.entity.Cutscenes.register();
		com.amethystmod.item.CrystalHammerItem.register();
		com.amethystmod.item.AmitarielSpearItem.register();
		InfectionManager.register();
		CoreManager.register();
		ModNetworking.register();
		com.amethystmod.world.WorldSites.register();
		com.amethystmod.item.PurifiedSpearItem.register();
		com.amethystmod.item.PurityAegisItem.register();
		com.amethystmod.entity.RiftManager.register();
		ModGameRules.register();

		// the old random patches are no longer placed in the world (the infection now grows from cores);
		// the feature stays registered so that worlds and data packs that name it still load
		Registry.register(Registries.FEATURE, Identifier.of(MOD_ID, "infection_patch"),
				new InfectionPatchFeature(DefaultFeatureConfig.CODEC));

		LOGGER.info("Amethyst Infection loaded");
	}
}
