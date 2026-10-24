package com.amethystmod.world;

import com.amethystmod.AmethystMod;
import com.amethystmod.item.LoreItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Places a {@link Blueprint} at a site: finds the ground, then sets blocks from the bottom up a few
 * thousand per tick (without neighbour updates), lays foundations under the walls, fills the chests and
 * wakes the markers (guards, Ricardo, the note on the lectern).
 */
public final class SiteBuilder {
	private static final int PER_TICK = 6000;

	private final ServerWorld world;
	private final WorldSites.Site site;
	private final Blueprint blueprint;
	private final List<Map.Entry<BlockPos, BlockState>> order;
	private final int[] bounds;
	private int index;
	private BlockPos origin;
	private boolean started;

	public SiteBuilder(ServerWorld world, WorldSites.Site site, Blueprint blueprint) {
		this.world = world;
		this.site = site;
		this.blueprint = blueprint;
		this.order = new ArrayList<>(blueprint.blocks.entrySet());
		this.order.sort(Comparator.comparingInt(e -> e.getKey().getY()));
		this.bounds = blueprint.bounds();
	}

	private void forceChunks(boolean force) {
		for (int cx = (site.x + bounds[0] - 2) >> 4; cx <= (site.x + bounds[3] + 2) >> 4; cx++) {
			for (int cz = (site.z + bounds[2] - 2) >> 4; cz <= (site.z + bounds[5] + 2) >> 4; cz++) {
				world.setChunkForced(cx, cz, force);
				if (force) {
					world.getChunk(cx, cz);
				}
			}
		}
	}

	/** @return true when done */
	public boolean tick() {
		if (!started) {
			started = true;
			forceChunks(true);
			int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, site.x, site.z);
			site.y = y;
			origin = new BlockPos(site.x, y, site.z);
		}
		int end = Math.min(order.size(), index + PER_TICK);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (; index < end; index++) {
			Map.Entry<BlockPos, BlockState> e = order.get(index);
			pos.set(origin.getX() + e.getKey().getX(), origin.getY() + e.getKey().getY(), origin.getZ() + e.getKey().getZ());
			world.setBlockState(pos, e.getValue(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}
		if (index < order.size()) {
			return false;
		}
		if (blueprint.foundations) {
			foundations();
		}
		finish();
		return true;
	}

	/** Under the lowest block of every column: stone down to the ground, so nothing floats. */
	private void foundations() {
		Map<Long, Integer> lowest = new java.util.HashMap<>();
		for (Map.Entry<BlockPos, BlockState> e : blueprint.blocks.entrySet()) {
			if (e.getValue().isAir() || !e.getValue().getFluidState().isEmpty()) {
				continue;
			}
			long key = ((long) e.getKey().getX() << 32) ^ (e.getKey().getZ() & 0xffffffffL);
			lowest.merge(key, e.getKey().getY(), Math::min);
		}
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
			int x = (int) (e.getKey() >> 32);
			int z = (int) (long) e.getKey().intValue();
			if (e.getValue() > 0) {
				continue;
			}
			for (int y = e.getValue() - 1, n = 0; n < 24; y--, n++) {
				pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
				BlockState here = world.getBlockState(pos);
				if (!here.isAir() && here.getFluidState().isEmpty() && !here.isReplaceable()) {
					break;
				}
				world.setBlockState(pos, n < 2 ? blueprint.rubble() : Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}

	private void finish() {
		for (Blueprint.Chest chest : blueprint.chests) {
			BlockPos pos = origin.add(chest.pos());
			BlockEntity be = world.getBlockEntity(pos);
			if (!(be instanceof ChestBlockEntity box)) {
				continue;
			}
			int slot = 0;
			for (String extra : chest.extras()) {
				ItemStack stack = extra(extra);
				if (!stack.isEmpty()) {
					// special items go to fixed slots; the loot table fills the empty ones when the chest is opened
					box.setStack(slot, stack);
					slot += 9 + world.random.nextInt(3);
				}
			}
			box.setLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(AmethystMod.MOD_ID, "chests/" + chest.lootTable())),
					world.random.nextLong());
		}
		for (Blueprint.Marker marker : blueprint.markers) {
			BlockPos pos = origin.add(marker.pos());
			String kind = marker.kind();
			if (kind.equals("note")) {
				if (world.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
					lectern.setBook(note(world));
					world.setBlockState(pos, world.getBlockState(pos).with(net.minecraft.block.LecternBlock.HAS_BOOK, true), Block.NOTIFY_LISTENERS);
				}
			} else if (kind.equals("ricardo")) {
				com.amethystmod.entity.RicardoEntity.spawnAt(world, pos);
			} else if (kind.startsWith("guard:")) {
				EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(kind.substring(6)));
				Entity entity = type.create(world);
				if (entity instanceof MobEntity mob) {
					mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
					mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
					mob.setPersistent();
					world.spawnEntity(mob);
				}
			}
		}
		forceChunks(false);
		site.built = true;
		WorldSites.State.get(world).markDirty();
		AmethystMod.LOGGER.info("Built {} at {}", site.kind.id(), origin);
	}

	private static ItemStack extra(String spec) {
		String[] p = spec.split(":");
		return switch (p[0]) {
			case "artifact" -> new ItemStack(Registries.ITEM.get(Identifier.of(AmethystMod.MOD_ID, p[1])));
			case "lore" -> LoreItem.create(p[1]);
			case "item" -> new ItemStack(Registries.ITEM.get(Identifier.of(p[1], p[2])), Integer.parseInt(p[3]));
			default -> ItemStack.EMPTY;
		};
	}

	/** The note in the starting house: it says where the three ruins are. */
	public static ItemStack note(ServerWorld world) {
		WorldSites.Site ruins = WorldSites.site(world.getServer(), WorldSites.Kind.RUINS);
		WorldSites.Site crater = WorldSites.site(world.getServer(), WorldSites.Kind.CRATER);
		WorldSites.Site castle = WorldSites.site(world.getServer(), WorldSites.Kind.CASTLE);
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<RawFilteredPair<Text>> pages = new ArrayList<>();
		pages.add(RawFilteredPair.of(Text.translatable("note.amethystmod.page1")));
		pages.add(RawFilteredPair.of(Text.translatable("note.amethystmod.page2",
				ruins.x, ruins.z, crater.x, crater.z, castle.x, castle.z)));
		pages.add(RawFilteredPair.of(Text.translatable("note.amethystmod.page3")));
		book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,
				new WrittenBookContentComponent(RawFilteredPair.of("Note"), "?", 0, pages, true));
		book.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("note.amethystmod.title"));
		return book;
	}
}
