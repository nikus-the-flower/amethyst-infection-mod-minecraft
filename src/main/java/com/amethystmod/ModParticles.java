package com.amethystmod;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
	/** A drifting violet glint. */
	public static final SimpleParticleType CRYSTAL_SPARK = register("crystal_spark");
	/** A cold white-blue mote of the purifying ward. */
	public static final SimpleParticleType WARD_MOTE = register("ward_mote");
	/** A dark magenta spore of the amethyst blight. */
	public static final SimpleParticleType BLIGHT_SPORE = register("blight_spore");
	/** A hot pink flash used by the bosses. */
	public static final SimpleParticleType PRISM_FLARE = register("prism_flare");
	/** A crystal flake drifting down in the crystal rain of the Amethyst world. */
	public static final SimpleParticleType CRYSTAL_RAIN = register("crystal_rain");

	private static SimpleParticleType register(String name) {
		return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(AmethystMod.MOD_ID, name),
				FabricParticleTypes.simple());
	}

	public static void register() {
	}
}
