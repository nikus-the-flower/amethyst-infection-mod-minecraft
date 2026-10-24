package com.amethystmod.client;

import com.amethystmod.block.AltarBlockEntity;
import com.amethystmod.block.ModBlockEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * The relics float and turn over their pedestals. When all three are in place the altar wakes: light
 * runs from each pedestal to the altar and a rune ring turns around it; during a purification the item
 * rises, spins ever faster and the light swells.
 */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
	public AltarRenderer(BlockEntityRendererFactory.Context ctx) {
	}

	@Override
	public boolean rendersOutsideBoundingBox(AltarBlockEntity entity) {
		return true;
	}

	@Override
	public void render(AltarBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light, int overlay) {
		if (be.getWorld() == null) {
			return;
		}
		float time = be.getWorld().getTime() + tickDelta;
		boolean altar = be.getType() == ModBlockEntities.PURIFICATION_ALTAR;
		float ritual = 0;
		if (altar && be.inRitual()) {
			ritual = MathHelper.clamp((time - be.getRitualStart()) / AltarBlockEntity.RITUAL_TICKS, 0, 1);
		}
		if (!be.getStack().isEmpty()) {
			matrices.push();
			float lift = (altar ? 1.25f : 1.15f) + ritual * 0.9f;
			matrices.translate(0.5, lift + 0.08f * MathHelper.sin(time * 0.08f), 0.5);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(time * (0.04f + ritual * 0.45f)));
			matrices.scale(0.7f, 0.7f, 0.7f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(be.getStack(), ModelTransformationMode.GROUND,
					LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, matrices, buffers, be.getWorld(), 0);
			matrices.pop();
		}
		if (!altar || !AltarBlockEntity.awake(be.getWorld(), be.getPos())) {
			return;
		}
		VertexConsumer solid = buffers.getBuffer(RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, false));
		VertexConsumer soft = buffers.getBuffer(RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, true));
		// light from each relic to the altar
		Vec3d top = new Vec3d(0.5, 1.3 + ritual * 0.9, 0.5);
		float width = 0.035f + ritual * 0.07f + 0.01f * MathHelper.sin(time * 0.3f);
		for (AltarBlockEntity pedestal : AltarBlockEntity.pedestals(be.getWorld(), be.getPos())) {
			Vec3d from = Vec3d.of(pedestal.getPos().subtract(be.getPos())).add(0.5, 1.2, 0.5);
			Vec3d d = top.subtract(from);
			matrices.push();
			matrices.translate(from.x, from.y, from.z);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) -Math.atan2(d.z, d.x)));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
			float len = (float) d.length();
			BeamEffectRenderer.box(matrices.peek(), solid, 0, len, -width, width, -width, width, 0xFFF4FFFF, -time * 0.1f, len * 0.3f);
			BeamEffectRenderer.box(matrices.peek(), soft, 0, len, -width * 2.5f, width * 2.5f, -width * 2.5f, width * 2.5f,
					0x6690D8FF, -time * 0.05f, len * 0.3f);
			matrices.pop();
		}
		// the rune ring, turning around the altar's top
		matrices.push();
		matrices.translate(0.5, 0.9, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(time * (0.02f + ritual * 0.2f)));
		float r = 1.3f + ritual * 0.4f;
		for (int i = 0; i < 12; i++) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (i * Math.PI / 6)));
			BeamEffectRenderer.box(matrices.peek(), soft, r - 0.12f, r + 0.12f, -0.02f, 0.02f, -0.25f, 0.25f,
					(i % 2 == 0 ? 0xAAF4FFFF : 0x88A0E8FF), 0, 0.2f);
			matrices.pop();
		}
		matrices.pop();
	}
}
