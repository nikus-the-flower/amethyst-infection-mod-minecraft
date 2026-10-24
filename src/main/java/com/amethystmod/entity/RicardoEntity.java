package com.amethystmod.entity;

import com.amethystmod.ModNetworking;
import com.amethystmod.item.LoreItem;
import com.amethystmod.item.ModItems;
import com.amethystmod.world.StoryState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtCustomerGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.StopFollowingCustomerGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;

/**
 * Ricardo, the old traveller who camps by the altar in the sunken ruins. Right-click him to talk (a
 * dialogue window with answers to choose); he trades amethyst essence for gold and diamonds, and food,
 * arrows and the simpler artifacts for essence. Wake the altar and he gives you the purity amulet.
 * He cannot be hurt and never wanders far from his camp.
 */
public class RicardoEntity extends MerchantEntity {
	private BlockPos home;

	public RicardoEntity(EntityType<? extends MerchantEntity> type, World world) {
		super(type, world);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return MerchantEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45)
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40);
	}

	public static void spawnAt(ServerWorld world, BlockPos pos) {
		RicardoEntity ricardo = ModEntities.RICARDO.create(world);
		if (ricardo == null) {
			return;
		}
		ricardo.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
		ricardo.setHome(pos);
		world.spawnEntity(ricardo);
	}

	private void setHome(BlockPos pos) {
		home = pos.toImmutable();
		setPositionTarget(home, 7);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new StopFollowingCustomerGoal(this));
		goalSelector.add(1, new LookAtCustomerGoal(this));
		goalSelector.add(4, new WanderAroundGoal(this, 0.35, 160));
		goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
	}

	// ---- talking ------------------------------------------------------------------------------

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (!isAlive() || hasCustomer()) {
			return super.interactMob(player, hand);
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			StoryState story = StoryState.get(serverPlayer.getServer());
			ServerPlayNetworking.send(serverPlayer, new ModNetworking.RicardoDialog(getId(), story.altarAwakened,
					story.gifted.contains(player.getUuid())));
			getLookControl().lookAt(player);
			playSound(SoundEvents.ENTITY_VILLAGER_AMBIENT, 0.8f, 0.85f);
		}
		return ActionResult.success(getWorld().isClient);
	}

	/** What a player chose in the dialogue window. */
	public void act(ServerPlayerEntity player, String action) {
		if (player.squaredDistanceTo(this) > 10 * 10) {
			return;
		}
		switch (action) {
			case "trade" -> {
				setCustomer(player);
				sendOffers(player, getDisplayName(), 1);
			}
			case "gift" -> {
				StoryState story = StoryState.get(player.getServer());
				if (story.altarAwakened && story.gifted.add(player.getUuid())) {
					story.markDirty();
					give(player, new ItemStack(ModItems.PURITY_AMULET));
					give(player, new ItemStack(ModItems.CEREAL_WITH_MILK, 2));
					playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 0.9f);
					playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
				}
			}
			default -> {
			}
		}
	}

	private static void give(ServerPlayerEntity player, ItemStack stack) {
		if (!player.giveItemStack(stack)) {
			player.dropItem(stack, false);
		}
	}

	// ---- trading ------------------------------------------------------------------------------

	@Override
	protected void fillRecipes() {
		TradeOfferList offers = getOffers();
		// essence for gold and diamonds
		offers.add(offer(Items.DIAMOND, 1, new ItemStack(ModItems.AMETHYST_ESSENCE, 24)));
		offers.add(offer(Items.GOLD_INGOT, 4, new ItemStack(ModItems.AMETHYST_ESSENCE, 12)));
		// food
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 8, new ItemStack(ModItems.CEREAL_WITH_MILK)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 5, new ItemStack(ModItems.COUNTRY_POTATOES)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 6, new ItemStack(ModItems.PURIFIED_CRYSTAL_BREAD, 2)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 14, new ItemStack(ModItems.PURIFIED_CRYSTAL_APPLE)));
		// odds and ends
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 10, new ItemStack(ModItems.CRYSTAL_ARROW, 16)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 2, new ItemStack(ModItems.GUIDE_BOOK)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 20, LoreItem.create("ricardo_journal")));
		// the simpler artifacts
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 40, new ItemStack(ModItems.BLINK_RING)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 40, new ItemStack(ModItems.VAMPIRIC_SHARD)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 48, new ItemStack(ModItems.GRAVITY_ORB)));
		offers.add(offer(ModItems.AMETHYST_ESSENCE, 48, new ItemStack(ModItems.STASIS_CRYSTAL)));
	}

	private static TradeOffer offer(net.minecraft.item.ItemConvertible pay, int count, ItemStack sell) {
		return new TradeOffer(new TradedItem(pay, count), sell, 999, 0, 0.0f);
	}

	@Override
	protected void afterUsing(TradeOffer offer) {
		playSound(SoundEvents.ENTITY_VILLAGER_TRADE, 0.8f, 0.9f);
	}

	@Override
	public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	@Override
	public boolean isLeveledMerchant() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return hasCustomer() ? SoundEvents.ENTITY_VILLAGER_TRADE : null;
	}

	// ---- he cannot be hurt ----------------------------------------------------------------------

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
			return super.damage(source, amount);
		}
		return false;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (home != null) {
			nbt.putInt("HomeX", home.getX());
			nbt.putInt("HomeY", home.getY());
			nbt.putInt("HomeZ", home.getZ());
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("HomeX")) {
			setHome(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")));
		}
	}
}
