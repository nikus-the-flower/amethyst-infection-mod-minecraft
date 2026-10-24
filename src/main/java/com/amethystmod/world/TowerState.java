package com.amethystmod.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Remembers, per world, whether the tower at 0,0 is built and where its ground floor is. */
public class TowerState extends PersistentState {
	/** Bumped when the tower changes in a way old worlds should receive (2: portal frame, 3: crystal cluster with a rift). */
	public static final int CURRENT_VERSION = 3;

	public boolean built;
	public int baseY;
	public int version;

	public static final PersistentState.Type<TowerState> TYPE = new PersistentState.Type<>(
			TowerState::new, TowerState::load, null);

	private static TowerState load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		TowerState state = new TowerState();
		state.built = nbt.getBoolean("built");
		state.baseY = nbt.getInt("baseY");
		state.version = nbt.getInt("version");
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putBoolean("built", built);
		nbt.putInt("baseY", baseY);
		nbt.putInt("version", version);
		return nbt;
	}

	public static TowerState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE, "amethystmod_tower");
	}
}
