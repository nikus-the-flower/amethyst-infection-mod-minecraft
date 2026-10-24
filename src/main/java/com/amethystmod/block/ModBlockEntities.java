package com.amethystmod.block;

import com.amethystmod.AmethystMod;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
	public static final BlockEntityType<AltarBlockEntity> ALTAR_PEDESTAL = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(AmethystMod.MOD_ID, "altar_pedestal"),
			BlockEntityType.Builder.create(AltarBlockEntity::pedestal, ModBlocks.ALTAR_PEDESTAL).build(null));
	public static final BlockEntityType<AltarBlockEntity> PURIFICATION_ALTAR = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(AmethystMod.MOD_ID, "purification_altar"),
			BlockEntityType.Builder.create(AltarBlockEntity::altar, ModBlocks.PURIFICATION_ALTAR).build(null));

	private ModBlockEntities() {
	}

	public static void register() {
	}
}
