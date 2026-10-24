package com.amethystmod;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class ModGameRules {
	/**
	 * Percent of the infection's speed: its per-tick budget, how fast cores age and how often they throw geodes.
	 * 0 stops the infection, 100 is normal, up to 1000 (ten times as fast, and ten times the work per tick).
	 */
	public static final GameRules.Key<GameRules.IntRule> INFECTION_SPEED = GameRuleRegistry.register(
			"amethystInfectionSpeed", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(100, 0, 1000));
	/** In-game days after which a core of the infection reaches its second phase. */
	public static final GameRules.Key<GameRules.IntRule> CORE_PHASE2_DAYS = GameRuleRegistry.register(
			"amethystCorePhase2Days", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(6, 1, 1000));
	/** In-game days after which a core reaches its third phase (counted from its birth, not from phase 2). */
	public static final GameRules.Key<GameRules.IntRule> CORE_PHASE3_DAYS = GameRuleRegistry.register(
			"amethystCorePhase3Days", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(15, 2, 2000));

	public static void register() {
	}
}
