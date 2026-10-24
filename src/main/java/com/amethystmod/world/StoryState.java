package com.amethystmod.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Story progress saved with the world (in the overworld): have Amitariel and the Amethyst Heart fallen? */
public class StoryState extends PersistentState {
	/** Mirror of {@link #heartDefeated} for hot code paths (random ticks); updated on start and on victory. */
	public static volatile boolean infectionStopped;

	public boolean heartDefeated;
	/** Amitariel has fallen: the rift to the world of the Amethyst is open in his hall. */
	public boolean amitarielDefeated;
	/** The altar of the sunken ruins has purified something once: Ricardo's task is done. */
	public boolean altarAwakened;
	/** Players Ricardo has already given his gift to. */
	public final java.util.Set<java.util.UUID> gifted = new java.util.HashSet<>();

	public static final PersistentState.Type<StoryState> TYPE = new PersistentState.Type<>(
			StoryState::new, StoryState::load, null);

	private static StoryState load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		StoryState state = new StoryState();
		state.heartDefeated = nbt.getBoolean("heartDefeated");
		state.amitarielDefeated = nbt.getBoolean("amitarielDefeated");
		state.altarAwakened = nbt.getBoolean("altarAwakened");
		for (net.minecraft.nbt.NbtElement e : nbt.getList("gifted", net.minecraft.nbt.NbtElement.INT_ARRAY_TYPE)) {
			state.gifted.add(net.minecraft.nbt.NbtHelper.toUuid(e));
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putBoolean("heartDefeated", heartDefeated);
		nbt.putBoolean("amitarielDefeated", amitarielDefeated);
		nbt.putBoolean("altarAwakened", altarAwakened);
		net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
		for (java.util.UUID id : gifted) {
			list.add(net.minecraft.nbt.NbtHelper.fromUuid(id));
		}
		nbt.put("gifted", list);
		return nbt;
	}

	public static StoryState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "amethystmod_story");
	}

	public static void markAmitarielDefeated(MinecraftServer server) {
		StoryState state = get(server);
		state.amitarielDefeated = true;
		state.markDirty();
	}

	public static void markAltarAwakened(MinecraftServer server) {
		StoryState state = get(server);
		state.altarAwakened = true;
		state.markDirty();
	}

	public static void markHeartDefeated(MinecraftServer server) {
		StoryState state = get(server);
		state.heartDefeated = true;
		state.markDirty();
		infectionStopped = true;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> infectionStopped = get(server).heartDefeated);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> infectionStopped = false);
	}
}
