package com.amethystmod.entity;

import com.amethystmod.AmethystMod;
import com.amethystmod.block.InfectedBlock;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;

public class ModEntities {
	// Roam the infection in the normal world, and the world of the Amethyst
	public static final EntityType<StalkerEntity> STALKER = register("amethyst_stalker",
			EntityType.Builder.create(StalkerEntity::new, SpawnGroup.MONSTER).dimensions(1.1f, 1.3f).maxTrackingRange(8));
	public static final EntityType<WeaverEntity> WEAVER = register("crystal_weaver",
			EntityType.Builder.create(WeaverEntity::new, SpawnGroup.MONSTER).dimensions(1.5f, 0.9f).maxTrackingRange(8));

	// Guards of the tower and the world of the Amethyst
	public static final EntityType<WalkerEntity> WALKER = register("shard_walker",
			EntityType.Builder.create(WalkerEntity::new, SpawnGroup.MONSTER).dimensions(1.2f, 3.6f).maxTrackingRange(10));
	public static final EntityType<LurkerEntity> LURKER = register("void_lurker",
			EntityType.Builder.create(LurkerEntity::new, SpawnGroup.MONSTER).dimensions(1.0f, 1.5f).maxTrackingRange(10));
	public static final EntityType<ColossusEntity> COLOSSUS = register("geode_colossus",
			EntityType.Builder.create(ColossusEntity::new, SpawnGroup.MONSTER).dimensions(2.6f, 5.2f).maxTrackingRange(10));
	public static final EntityType<DrifterEntity> DRIFTER = register("celestial_drifter",
			EntityType.Builder.create(DrifterEntity::new, SpawnGroup.MONSTER).dimensions(1.8f, 3.6f).maxTrackingRange(12));

	// Bosses
	public static final EntityType<AmitarielEntity> AMITARIEL = register("amitariel",
			EntityType.Builder.create(AmitarielEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 3.0f).maxTrackingRange(16));
	public static final EntityType<HeartEntity> HEART = register("amethyst_heart",
			EntityType.Builder.create(HeartEntity::new, SpawnGroup.MONSTER).dimensions(7.0f, 10.0f).maxTrackingRange(20).makeFireImmune());

	/** The crystal at the heart of a patch of infection (its size follows its phase). */
	public static final EntityType<InfectionCoreEntity> INFECTION_CORE = register("infection_core",
			EntityType.Builder.create(InfectionCoreEntity::new, SpawnGroup.MISC).dimensions(0.9f, 1.3f).maxTrackingRange(10)
					.makeFireImmune().disableSummon());

	public static final EntityType<CrystalShardEntity> CRYSTAL_SHARD = register("crystal_shard",
			EntityType.Builder.<CrystalShardEntity>create(CrystalShardEntity::new, SpawnGroup.MISC)
					.dimensions(0.3f, 0.3f).maxTrackingRange(6).trackingTickInterval(5));

	public static final EntityType<HeartCrystalEntity> HEART_PYLON = register("heart_pylon",
			EntityType.Builder.create(HeartCrystalEntity::new, SpawnGroup.MONSTER).dimensions(1.6f, 3.6f).maxTrackingRange(16).makeFireImmune());
	public static final EntityType<HeartCrystalEntity> HEART_SENTINEL = register("heart_sentinel",
			EntityType.Builder.create(HeartCrystalEntity::new, SpawnGroup.MONSTER).dimensions(1.2f, 1.8f).maxTrackingRange(16).makeFireImmune());
	public static final EntityType<HeartNodeEntity> HEART_NODE = register("heart_node",
			EntityType.Builder.<HeartNodeEntity>create(HeartNodeEntity::new, SpawnGroup.MISC).dimensions(1.3f, 1.3f)
					.maxTrackingRange(16).makeFireImmune().disableSummon());

	public static final EntityType<CrystalEffectEntity> CRYSTAL_SPIKE = register("crystal_spike",
			EntityType.Builder.<CrystalEffectEntity>create(CrystalEffectEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(10).trackingTickInterval(20).makeFireImmune().disableSummon());
	public static final EntityType<CrystalEffectEntity> RUNE_CIRCLE = register("rune_circle",
			EntityType.Builder.<CrystalEffectEntity>create(CrystalEffectEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.1f)
					.maxTrackingRange(10).trackingTickInterval(20).makeFireImmune().disableSummon());

	public static final EntityType<CrystalEffectEntity> FALLING_CRYSTAL = register("falling_crystal",
			EntityType.Builder.<CrystalEffectEntity>create(CrystalEffectEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(10).trackingTickInterval(20).makeFireImmune().disableSummon());
	public static final EntityType<BeamEffectEntity> BEAM_EFFECT = register("crystal_beam",
			EntityType.Builder.<BeamEffectEntity>create(BeamEffectEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().disableSummon());

	public static final EntityType<RiftEntity> RIFT = register("dimensional_rift",
			EntityType.Builder.<RiftEntity>create(RiftEntity::new, SpawnGroup.MISC).dimensions(1.4f, 4.6f)
					.maxTrackingRange(10).trackingTickInterval(20).makeFireImmune().disableSummon());

	public static final EntityType<PhantomSpearEntity> PHANTOM_SPEAR = register("phantom_spear",
			EntityType.Builder.<PhantomSpearEntity>create(PhantomSpearEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(8).trackingTickInterval(1).makeFireImmune().disableSummon());

	public static final EntityType<SlashEntity> SLASH = register("crescent_slash",
			EntityType.Builder.<SlashEntity>create(SlashEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(8).trackingTickInterval(1).makeFireImmune().disableSummon());

	public static final EntityType<RicardoEntity> RICARDO = register("ricardo",
			EntityType.Builder.create(RicardoEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.95f).maxTrackingRange(10));

	public static final EntityType<CrystalArrowEntity> CRYSTAL_ARROW = register("crystal_arrow",
			EntityType.Builder.<CrystalArrowEntity>create(CrystalArrowEntity::new, SpawnGroup.MISC)
					.dimensions(0.5f, 0.5f).maxTrackingRange(4).trackingTickInterval(20));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		return Registry.register(Registries.ENTITY_TYPE, Identifier.of(AmethystMod.MOD_ID, name), builder.build(name));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.attributes());
		FabricDefaultAttributeRegistry.register(WEAVER, WeaverEntity.attributes());
		FabricDefaultAttributeRegistry.register(WALKER, WalkerEntity.attributes());
		FabricDefaultAttributeRegistry.register(LURKER, LurkerEntity.attributes());
		FabricDefaultAttributeRegistry.register(COLOSSUS, ColossusEntity.attributes());
		FabricDefaultAttributeRegistry.register(DRIFTER, DrifterEntity.attributes());
		FabricDefaultAttributeRegistry.register(AMITARIEL, AmitarielEntity.attributes());
		FabricDefaultAttributeRegistry.register(HEART, HeartEntity.attributes());
		FabricDefaultAttributeRegistry.register(RICARDO, RicardoEntity.attributes());
		FabricDefaultAttributeRegistry.register(HEART_PYLON, HeartCrystalEntity.pylonAttributes());
		FabricDefaultAttributeRegistry.register(HEART_SENTINEL, HeartCrystalEntity.sentinelAttributes());
		FabricDefaultAttributeRegistry.register(INFECTION_CORE, InfectionCoreEntity.attributes());

		// Natural spawns: only on infected ground, light does not matter (the crystals glow)
		SpawnRestriction.register(STALKER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		SpawnRestriction.register(WEAVER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		// the creatures of the tower spawn naturally only in the world of the Amethyst (its biomes list them)
		SpawnRestriction.register(WALKER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		SpawnRestriction.register(LURKER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		SpawnRestriction.register(COLOSSUS, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		SpawnRestriction.register(DRIFTER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnOnInfection);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, STALKER, 5, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, WEAVER, 3, 1, 1);
	}

	private static <T extends AmethystMob> boolean canSpawnOnInfection(EntityType<T> type, ServerWorldAccess world,
																		   SpawnReason reason, BlockPos pos, Random random) {
		boolean amethystWorld = world.toServerWorld().getRegistryKey() == com.amethystmod.world.AmethystWorld.WORLD;
		if (amethystWorld && pos.getX() * pos.getX() + pos.getZ() * pos.getZ() < 80 * 80) {
			return false;
		}
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		if (amethystWorld) {
			return random.nextInt(10) == 0;
		}
		// in the normal world the infected land is huge, so there must be a ceiling: no more than a handful of
		// creatures of the Amethyst around (the cores call their own), or they would fill the whole mob cap and the
		// server would slow down to a crawl
		int nearby = world.getEntitiesByClass(AmethystMob.class, new Box(pos).expand(44, 24, 44),
				e -> !(e instanceof InfectionCoreEntity)).size();
		if (nearby >= 4) {
			return false;
		}
		if (world.getBlockState(pos.down()).getBlock() instanceof InfectedBlock) {
			return random.nextInt(6) == 0;
		}
		return world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.isSkyVisible(pos) && random.nextInt(60) == 0;
	}
}
