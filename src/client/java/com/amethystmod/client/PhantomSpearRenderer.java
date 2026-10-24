package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.PhantomSpearEntity;
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

/** A spear of light pointing along its aim, glowing. */
public class PhantomSpearRenderer extends EntityRenderer<PhantomSpearEntity> {
	private static final Identifier TEXTURE = Identifier.of(AmethystMod.MOD_ID, "textures/entity/phantom_spear.png");
	private static final Identifier GLOW = Identifier.of(AmethystMod.MOD_ID, "textures/entity/phantom_spear_glow.png");
	private static final Identifier PURE = Identifier.of(AmethystMod.MOD_ID, "textures/entity/phantom_spear_pure.png");
	private static final Identifier PURE_GLOW = Identifier.of(AmethystMod.MOD_ID, "textures/entity/phantom_spear_pure_glow.png");
	private final ModelPart root;

	public PhantomSpearRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer) {
		super(ctx);
		this.root = ctx.getPart(layer);
	}

	@Override
	public void render(PhantomSpearEntity entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0f));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
		matrices.scale(-0.8f, -0.8f, 0.8f);
		matrices.translate(0, -1.501, 0);
		boolean pure = entity.isPure();
		root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(pure ? PURE : TEXTURE)), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xCCFFFFFF);
		root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEyes(pure ? PURE_GLOW : GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(PhantomSpearEntity entity) {
		return TEXTURE;
	}
}
