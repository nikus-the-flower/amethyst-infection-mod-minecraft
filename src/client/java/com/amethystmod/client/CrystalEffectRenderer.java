package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.CrystalEffectEntity;
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

/**
 * Draws the 3D effects of the bosses: a crystal spike that bursts out of the floor, holds and sinks
 * back, or a rune circle that spins slowly on the floor while an attack is coming.
 */
public class CrystalEffectRenderer extends EntityRenderer<CrystalEffectEntity> {
	private final ModelPart root;
	private final Identifier texture;
	private final Identifier glow;
	private final boolean rune;
	private final boolean falling;

	public CrystalEffectRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer, String name, boolean rune) {
		this(ctx, layer, name, rune, false);
	}

	public CrystalEffectRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer, String name, boolean rune, boolean falling) {
		super(ctx);
		this.root = ctx.getPart(layer);
		this.texture = Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + name + ".png");
		this.glow = Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + name + "_glow.png");
		this.rune = rune;
		this.falling = falling;
	}

	@Override
	public void render(CrystalEffectEntity entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {
		float t = entity.age + tickDelta;
		float life = entity.getLife();
		float size = entity.getSize();
		matrices.push();
		float scale;
		if (rune) {
			float appear = MathHelper.clamp(t / 6.0f, 0, 1);
			float fade = MathHelper.clamp((life - t) / 4.0f, 0, 1);
			scale = size * (0.6f + 0.4f * appear) * (0.8f + 0.2f * fade);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw() + t * 2.5f));
		} else if (falling) {
			// drops from high above, point first, speeding up, then sticks in the floor and sinks
			float fall = MathHelper.clamp(t / CrystalEffectEntity.FALL_TICKS, 0, 1);
			float sink = MathHelper.clamp((life - t) / 8.0f, 0, 1);
			scale = size;
			matrices.translate(0, 16.0f * (1.0f - fall * fall) + (sink - 1.0f) * 2.0f * size + 1.2f * size, 0);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw() + t * (fall < 1 ? 12.0f : 0.0f)));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
		} else {
			float grow = MathHelper.clamp(t / 3.5f, 0, 1);
			float sink = MathHelper.clamp((life - t) / 8.0f, 0, 1);
			scale = size * (0.25f + 0.75f * grow * grow);
			matrices.translate(0, (sink - 1.0f) * 2.2f * size, 0);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
		}
		matrices.scale(-scale, -scale, scale);
		matrices.translate(0, -1.501, 0);
		if (!rune) {
			root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture)), light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		}
		root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEyes(glow)), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(CrystalEffectEntity entity) {
		return texture;
	}
}
