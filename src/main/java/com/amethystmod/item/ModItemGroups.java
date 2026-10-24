package com.amethystmod.item;

import com.amethystmod.AmethystMod;
import com.amethystmod.block.CrystalColor;
import com.amethystmod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
	public static final ItemGroup AMETHYST = Registry.register(Registries.ITEM_GROUP,
			Identifier.of(AmethystMod.MOD_ID, "amethyst"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(ModBlocks.crystal(CrystalColor.VIOLET, 1)))
					.displayName(Text.translatable("itemGroup.amethystmod.amethyst"))
					.entries((context, entries) -> {
						ModItems.ALL.forEach(entries::add);
						entries.add(CrystalGeodeItem.stack(2, 1));
						entries.add(CrystalGeodeItem.stack(3, 1));
						ModBlocks.ALL.forEach(entries::add);
					})
					.build());

	public static void register() {
	}
}
