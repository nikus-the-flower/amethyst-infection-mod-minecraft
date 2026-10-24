package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.item.ModItems;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Draws the 3D growths of the amethyst and purifier's armor (crests, shoulder spikes, back crystals,
 * halo, wings of light) on top of the normal armor layer, attached to the player's own body parts.
 */
public class ArmorExtras extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private record Set(String name, ModelPart root, Item helmet, Item chestplate, Item leggings, Item boots) {
		Identifier texture() {
			return Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + name + ".png");
		}

		Identifier glow() {
			return Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + name + "_glow.png");
		}
	}

	private final Set amethyst;
	private final Set purified;

	public ArmorExtras(PlayerEntityRenderer renderer, ModelPart amethystRoot, ModelPart purifiedRoot) {
		super(renderer);
		amethyst = new Set("armor_amethyst", amethystRoot, ModItems.AMETHYST_HELMET, ModItems.AMETHYST_CHESTPLATE,
				ModItems.AMETHYST_LEGGINGS, ModItems.AMETHYST_BOOTS);
		purified = new Set("armor_purified", purifiedRoot, ModItems.PURIFIED_HELMET, ModItems.PURIFIED_CHESTPLATE,
				ModItems.PURIFIED_LEGGINGS, ModItems.PURIFIED_BOOTS);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player,
					   float limbAngle, float limbDistance, float tickDelta, float animationProgress,
					   float headYaw, float headPitch) {
		if (player.isInvisible()) {
			return;
		}
		PlayerEntityModel<AbstractClientPlayerEntity> model = getContextModel();
		for (Set set : new Set[]{amethyst, purified}) {
			if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(set.helmet())) {
				draw(matrices, vertexConsumers, light, model.head, set, "helm");
			}
			if (player.getEquippedStack(EquipmentSlot.CHEST).isOf(set.chestplate())) {
				draw(matrices, vertexConsumers, light, model.body, set, "chest");
				draw(matrices, vertexConsumers, light, model.leftArm, set, "chest_arm_l");
				draw(matrices, vertexConsumers, light, model.rightArm, set, "chest_arm_r");
			}
			if (player.getEquippedStack(EquipmentSlot.LEGS).isOf(set.leggings())) {
				draw(matrices, vertexConsumers, light, model.leftLeg, set, "legs_l");
				draw(matrices, vertexConsumers, light, model.rightLeg, set, "legs_r");
			}
			if (player.getEquippedStack(EquipmentSlot.FEET).isOf(set.boots())) {
				draw(matrices, vertexConsumers, light, model.leftLeg, set, "boots_l");
				draw(matrices, vertexConsumers, light, model.rightLeg, set, "boots_r");
			}
		}
	}

	private void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ModelPart attachTo, Set set, String part) {
		ModelPart extra = set.root().getChild(part);
		matrices.push();
		attachTo.rotate(matrices);
		extra.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(set.texture())), light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		extra.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEyes(set.glow())), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		matrices.pop();
	}
}
