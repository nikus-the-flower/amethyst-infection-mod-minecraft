package com.amethystmod.client;

import com.amethystmod.entity.SlashEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** A flying crescent of light, curved toward where it flies, fading at the end of its flight. */
public class SlashRenderer extends EntityRenderer<SlashEntity> {
	private static final Identifier TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;

	public SlashRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(SlashEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light) {
		float time = entity.age + tickDelta;
		float fade = MathHelper.clamp(Math.min(time / 2.0f, (SlashEntity.LIFE - time) / 5.0f), 0, 1);
		float size = entity.getSize() * (0.8f + 0.2f * Math.min(1.0f, time / 4.0f));
		matrices.push();
		matrices.translate(0, 0.2, 0);
		// the same turn as arrows: +X points along the flight
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0f));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(entity.getRoll()));
		// the crescent is drawn bulging toward +X with its middle at the origin
		matrices.translate(-size * 0.8f, 0, 0);
		MatrixStack.Entry e = matrices.peek();
		int a1 = Math.round(0x99 * fade);
		int a0 = Math.round(0xFF * fade);
		BeamEffectRenderer.crescent(e, buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, true)), size * 1.05f, 120, size * 0.5f, 0.08f,
				(a1 << 24) | 0xC060FF, -time * 0.2f);
		BeamEffectRenderer.crescent(e, buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, false)), size, 100, size * 0.24f, 0.03f,
				(a0 << 24) | 0xFFE8FF, -time * 0.3f);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, buffers, light);
	}

	@Override
	public Identifier getTexture(SlashEntity entity) {
		return TEXTURE;
	}
}
