package com.amethystmod.block;

import com.amethystmod.ModParticles;
import com.amethystmod.item.ModItems;
import com.amethystmod.world.StoryState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a pedestal or the altar holds. The altar also runs the purification: when the three relics
 * (the shard of purity, the emperor's crown and the flask of chaos) stand on the pedestals around it,
 * an amethyst thing laid on it is turned, after a short ritual, into its purified version. The relics
 * are not used up.
 */
public class AltarBlockEntity extends BlockEntity {
	public static final int RITUAL_TICKS = 90;
	/** How far around the altar its pedestals may stand. */
	public static final int REACH = 4;

	private ItemStack stack = ItemStack.EMPTY;
	/** World time the ritual started, or -1. */
	private long ritualStart = -1;

	public AltarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static AltarBlockEntity pedestal(BlockPos pos, BlockState state) {
		return new AltarBlockEntity(ModBlockEntities.ALTAR_PEDESTAL, pos, state);
	}

	public static AltarBlockEntity altar(BlockPos pos, BlockState state) {
		return new AltarBlockEntity(ModBlockEntities.PURIFICATION_ALTAR, pos, state);
	}

	public ItemStack getStack() {
		return stack;
	}

	public void setStack(ItemStack stack) {
		this.stack = stack;
		changed();
	}

	public long getRitualStart() {
		return ritualStart;
	}

	public boolean inRitual() {
		return ritualStart >= 0;
	}

	private void changed() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), 3);
		}
	}

	// ---- the relics and the recipes ----------------------------------------------------------

	public static boolean isRelic(ItemStack stack) {
		return stack.isOf(ModItems.PURITY_SHARD) || stack.isOf(ModItems.EMPEROR_CROWN) || stack.isOf(ModItems.CHAOS_FLASK);
	}

	/** What the altar turns an item into, or null. */
	public static Item purified(Item item) {
		Map<Item, Item> map = Map.of(
				ModItems.AMETHYST_ESSENCE, ModItems.PURIFIED_AMETHYST,
				ModItems.AMETHYST_HELMET, ModItems.PURIFIED_HELMET,
				ModItems.AMETHYST_CHESTPLATE, ModItems.PURIFIED_CHESTPLATE,
				ModItems.AMETHYST_LEGGINGS, ModItems.PURIFIED_LEGGINGS,
				ModItems.AMETHYST_BOOTS, ModItems.PURIFIED_BOOTS,
				ModItems.AMETHYST_SWORD, ModItems.PURIFIER_BLADE,
				ModItems.AMITARIEL_SPEAR, ModItems.PURIFIED_SPEAR);
		return map.get(item);
	}

	/** The pedestals around an altar. */
	public static List<AltarBlockEntity> pedestals(World world, BlockPos altar) {
		List<AltarBlockEntity> list = new ArrayList<>();
		for (BlockPos p : BlockPos.iterate(altar.add(-REACH, -1, -REACH), altar.add(REACH, 1, REACH))) {
			if (world.getBlockEntity(p) instanceof AltarBlockEntity be && be.getType() == ModBlockEntities.ALTAR_PEDESTAL) {
				list.add(be);
			}
		}
		return list;
	}

	/** True when the three different relics stand on pedestals around the altar. */
	public static boolean awake(World world, BlockPos altar) {
		Set<Item> relics = new HashSet<>();
		for (AltarBlockEntity p : pedestals(world, altar)) {
			if (isRelic(p.stack)) {
				relics.add(p.stack.getItem());
			}
		}
		return relics.size() == 3;
	}

	/** Lays {@code item} on the altar and begins the ritual. */
	public void beginRitual(ItemStack item) {
		this.stack = item;
		this.ritualStart = world.getTime();
		changed();
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.2f);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 2.0f, 0.6f);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, AltarBlockEntity be) {
		if (be.ritualStart < 0 || !(world instanceof ServerWorld server)) {
			return;
		}
		long t = world.getTime() - be.ritualStart;
		Vec3d top = Vec3d.ofCenter(pos).add(0, 1.4 + t / (double) RITUAL_TICKS, 0);
		if (t % 4 == 0) {
			server.spawnParticles(ModParticles.WARD_MOTE, top.x, top.y, top.z, 3, 0.3, 0.3, 0.3, 0.02);
			for (AltarBlockEntity p : pedestals(world, pos)) {
				Vec3d from = Vec3d.ofCenter(p.pos).add(0, 1.2, 0);
				Vec3d mid = from.lerp(top, (t % 20) / 20.0);
				server.spawnParticles(ModParticles.CRYSTAL_SPARK, mid.x, mid.y, mid.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
		}
		if (t % 20 == 0) {
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 0.6f + t / 120.0f);
		}
		if (t >= RITUAL_TICKS) {
			Item result = purified(be.stack.getItem());
			if (result != null) {
				be.stack = be.stack.copyComponentsToNewStack(result, be.stack.getCount());
			}
			be.ritualStart = -1;
			be.changed();
			server.spawnParticles(ModParticles.WARD_MOTE, top.x, top.y, top.z, 60, 0.6, 0.6, 0.6, 0.15);
			server.spawnParticles(ModParticles.PRISM_FLARE, top.x, top.y, top.z, 20, 0.4, 0.4, 0.4, 0.1);
			world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.4f);
			world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.5f, 1.6f);
			if (!StoryState.get(server.getServer()).altarAwakened) {
				StoryState.markAltarAwakened(server.getServer());
				for (ServerPlayerEntity player : server.getPlayers(p -> p.squaredDistanceTo(Vec3d.ofCenter(pos)) < 40 * 40)) {
					player.sendMessage(Text.translatable("message.amethystmod.altar.awakened").formatted(Formatting.AQUA), false);
				}
			}
		}
	}

	// ---- saving and syncing ------------------------------------------------------------------

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (!stack.isEmpty()) {
			nbt.put("Item", stack.encode(registries));
		}
		nbt.putLong("RitualStart", ritualStart);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		stack = nbt.contains("Item") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Item")) : ItemStack.EMPTY;
		ritualStart = nbt.contains("RitualStart") ? nbt.getLong("RitualStart") : -1;
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createNbt(registries);
	}
}
