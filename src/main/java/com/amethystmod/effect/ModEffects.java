package com.amethystmod.effect;

import com.amethystmod.AmethystMod;
import com.amethystmod.InfectionSpread;
import com.amethystmod.ModParticles;
import com.amethystmod.block.InfectedBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class ModEffects {
	/** Worn by the full amethyst set: the crystals resonate with the wearer. */
	public static final RegistryEntry<StatusEffect> RESONANCE = register("crystal_resonance", new Resonance());
	/** Worn by the full purifier's set: heals on infected ground, burns the infection away, blocks the blight. */
	public static final RegistryEntry<StatusEffect> WARD = register("anti_amethyst_ward", new Ward());
	/** Inflicted by amethyst creatures: the body slowly turns to crystal. */
	public static final RegistryEntry<StatusEffect> BLIGHT = register("amethyst_blight", new Blight());

	private static RegistryEntry<StatusEffect> register(String name, StatusEffect effect) {
		return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(AmethystMod.MOD_ID, name), effect);
	}

	public static void register() {
	}

	private static Identifier id(String path) {
		return Identifier.of(AmethystMod.MOD_ID, "effect." + path);
	}

	private static class Resonance extends StatusEffect {
		Resonance() {
			super(StatusEffectCategory.BENEFICIAL, 0xB57BFF);
			addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, id("resonance_damage"), 2.0,
					EntityAttributeModifier.Operation.ADD_VALUE);
			addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, id("resonance_speed"), 0.15,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, id("resonance_move"), 0.08,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}

		@Override
		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			return duration % 10 == 0;
		}

		@Override
		public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
			if (entity.getWorld() instanceof ServerWorld world) {
				Vec3d p = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
				world.spawnParticles(ModParticles.CRYSTAL_SPARK, p.x, p.y, p.z, 1, 0.5, 0.7, 0.5, 0.01);
			}
			return true;
		}
	}

	private static class Ward extends StatusEffect {
		Ward() {
			super(StatusEffectCategory.BENEFICIAL, 0xC9F2FF);
			addAttributeModifier(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, id("ward_knockback"), 0.4,
					EntityAttributeModifier.Operation.ADD_VALUE);
			addAttributeModifier(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, id("ward_toughness"), 2.0,
					EntityAttributeModifier.Operation.ADD_VALUE);
		}

		@Override
		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			return duration % 20 == 0;
		}

		@Override
		public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
			if (!(entity.getWorld() instanceof ServerWorld world)) {
				return true;
			}
			entity.removeStatusEffect(BLIGHT);
			if (world.getBlockState(entity.getBlockPos().down()).getBlock() instanceof InfectedBlock) {
				entity.heal(1.5f);
			}
			InfectionSpread.purgeAround(world, entity.getBlockPos(), 5, entity.getRandom(), 6);
			Vec3d p = entity.getPos().add(0, 0.1, 0);
			world.spawnParticles(ModParticles.WARD_MOTE, p.x, p.y, p.z, 8, 2.5, 0.2, 2.5, 0.02);
			return true;
		}
	}

	private static class Blight extends StatusEffect {
		Blight() {
			super(StatusEffectCategory.HARMFUL, 0x7A2BC8);
			addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, id("blight_slow"), -0.15,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, id("blight_weak"), -0.1,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}

		@Override
		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			return duration % 40 == 0;
		}

		@Override
		public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
			if (entity.getWorld() instanceof ServerWorld world) {
				if (entity.hasStatusEffect(WARD)) {
					entity.removeStatusEffect(BLIGHT);
					return true;
				}
				DamageSource source = world.getDamageSources().magic();
				if (entity.getHealth() > 6.0f) {
					entity.damage(source, 1.0f + amplifier);
				}
				Vec3d p = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
				world.spawnParticles(ModParticles.BLIGHT_SPORE, p.x, p.y, p.z, 4, 0.3, 0.5, 0.3, 0.02);
			}
			return true;
		}
	}
}
