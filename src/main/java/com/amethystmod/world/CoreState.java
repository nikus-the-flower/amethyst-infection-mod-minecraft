package com.amethystmod.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every core of the infection in the normal world, saved with the world. A core is only a record here
 * (where it is and when it was born); its crystal entity appears when players come near, and the land
 * around it is painted by the {@link com.amethystmod.InfectionManager}. Records cost next to nothing, so
 * the whole world can be full of cores while only the ones near players do any work.
 */
public class CoreState extends PersistentState {
	public static final class Core {
		public final int id;
		public final int x;
		public final int z;
		/** Ground level once known (-10000 until the core's chunk has been loaded). */
		public int y = UNKNOWN_Y;
		/** World time of its birth: its age decides its phase and how far its land reaches. */
		public long born;
		/** World time of its next geode (0 = not planned yet). */
		public long nextGeode;
		/** After the Heart's fall: the world time it fades away (0 = it does not). */
		public long dieAt;

		// runtime only: the shape of its land and how far each ray has been painted
		public float[] shape;
		public int[] reach;
		public int lastPhase;
		public int cursor;

		public Core(int id, int x, int z) {
			this.id = id;
			this.x = x;
			this.z = z;
		}

		public double distanceSq(double px, double pz) {
			double dx = px - x;
			double dz = pz - z;
			return dx * dx + dz * dz;
		}
	}

	public static final int UNKNOWN_Y = -10000;

	public final Map<Integer, Core> cores = new LinkedHashMap<>();
	/** World cells (256 x 256 blocks) whose starting core has already been decided. */
	public final Set<Long> cells = new HashSet<>();
	public int nextId = 1;

	public static final PersistentState.Type<CoreState> TYPE = new PersistentState.Type<>(CoreState::new, CoreState::load, null);

	private static CoreState load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		CoreState s = new CoreState();
		s.nextId = Math.max(1, nbt.getInt("nextId"));
		for (NbtElement e : nbt.getList("cores", NbtElement.COMPOUND_TYPE)) {
			NbtCompound c = (NbtCompound) e;
			Core core = new Core(c.getInt("id"), c.getInt("x"), c.getInt("z"));
			core.y = c.contains("y") ? c.getInt("y") : UNKNOWN_Y;
			core.born = c.getLong("born");
			core.nextGeode = c.getLong("nextGeode");
			core.dieAt = c.getLong("dieAt");
			s.cores.put(core.id, core);
			s.nextId = Math.max(s.nextId, core.id + 1);
		}
		for (long cell : nbt.getLongArray("cells")) {
			s.cells.add(cell);
		}
		return s;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putInt("nextId", nextId);
		NbtList list = new NbtList();
		for (Core core : cores.values()) {
			NbtCompound c = new NbtCompound();
			c.putInt("id", core.id);
			c.putInt("x", core.x);
			c.putInt("z", core.z);
			c.putInt("y", core.y);
			c.putLong("born", core.born);
			c.putLong("nextGeode", core.nextGeode);
			c.putLong("dieAt", core.dieAt);
			list.add(c);
		}
		nbt.put("cores", list);
		nbt.putLongArray("cells", cells.stream().mapToLong(Long::longValue).toArray());
		return nbt;
	}

	public Core add(int x, int z, long born) {
		Core core = new Core(nextId++, x, z);
		core.born = born;
		cores.put(core.id, core);
		markDirty();
		return core;
	}

	public static CoreState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "amethystmod_cores");
	}
}
