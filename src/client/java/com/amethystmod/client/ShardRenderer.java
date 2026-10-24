package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.CrystalShardEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** A spinning 3D crystal shard pointing along its flight. */
public class ShardRenderer extends EntityRenderer<CrystalShardEntity> {
	private static final Identifier TEXTURE = Identifier.of(AmethystMod.MOD_ID, "textures/entity/shard_bolt.png");
	private static final Identifier GLOW = Identifier.of(AmethystMod.MOD_ID, "textures/entity/shard_bolt_glow.png");
	private final ModelPart root;

	public ShardRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer) {
		super(ctx);
		this.root = ctx.getPart(layer);
	}

	@Override
	public void render(CrystalShardEntity entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0f));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((entity.age + tickDelta) * 25.0f));
		matrices.scale(-0.7f, -0.7f, 0.7f);
		matrices.translate(0, -1.501, 0);
		root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(CrystalShardEntity entity) {
		return TEXTURE;
	}
}
