package com.amethystmod.item;

import com.amethystmod.AmethystMod;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Data the mod keeps on item stacks. */
public final class ModComponents {
	/** Aegis of Purity: how much purified essence is in it (0-100). */
	public static final ComponentType<Integer> SHIELD_CHARGE = register("shield_charge");
	/** Aegis of Purity: health points of its shield right now. */
	public static final ComponentType<Integer> SHIELD_HEALTH = register("shield_health");
	/** A crystal geode: the phase of the core it plants (1-3). */
	public static final ComponentType<Integer> CORE_PHASE = register("core_phase");
	/** A page of the story: which text it holds. */
	public static final ComponentType<String> LORE_ID = Registry.register(Registries.DATA_COMPONENT_TYPE,
			Identifier.of(AmethystMod.MOD_ID, "lore_id"),
			ComponentType.<String>builder().codec(Codec.STRING).packetCodec(PacketCodecs.STRING).build());

	private ModComponents() {
	}

	private static ComponentType<Integer> register(String name) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(AmethystMod.MOD_ID, name),
				ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());
	}

	public static void register() {
	}
}
