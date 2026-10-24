package com.amethystmod.client;

import com.amethystmod.entity.RiftEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Draws a rift: a tall jagged tear of light that always faces the viewer, with a dark void in its
 * middle, a soft violet glow around it and crystal shards circling it. It tears open from a thin line.
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {
	private static final Identifier TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;
	private static final float HEIGHT = 4.6f;
	private static final float WIDTH = 0.95f;
	private static final int SLICES = 18;

	public RiftRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(RiftEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	/** Half-width of the tear at height fraction {@code f}, jagged and slowly flickering. */
	private static float halfWidth(float f, float time, int seed) {
		float shape = (float) Math.pow(MathHelper.sin((float) Math.PI * f), 0.7);
		float jag = 1.0f + 0.28f * MathHelper.sin(f * 23.0f + seed) + 0.12f * MathHelper.sin(time * 0.15f + f * 9.0f + seed);
		return WIDTH * shape * jag;
	}

	/** Sideways offset of the tear: a gentle zigzag. */
	private static float offset(float f, float time) {
		return 0.22f * MathHelper.sin(f * 11.0f + time * 0.03f) + 0.12f * MathHelper.sin(f * 27.0f);
	}

	@Override
	public void render(RiftEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light) {
		float time = entity.age + tickDelta;
		float open = entity.openness(tickDelta);
		float spread = 0.08f + 0.92f * open * open;
		float tall = Math.min(1.0f, 0.3f + open * 1.4f);
		matrices.push();
		// always turned toward the viewer
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-dispatcher.camera.getYaw()));
		MatrixStack.Entry e = matrices.peek();
		VertexConsumer solid = buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, false));
		VertexConsumer soft = buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, true));
		float baseY = HEIGHT * (1 - tall) / 2;
		float h = HEIGHT * tall;
		float scroll = -time * 0.05f;
		// soft violet glow, the bright edge, then the dark void inside
		tear(e, soft, baseY, h, spread * 2.0f, 0.0f, time, 0x66B040FF, scroll, 1);
		tear(e, soft, baseY, h, spread * 1.35f, 0.0f, time, 0xAAE070FF, scroll * 1.5f, 2);
		tear(e, solid, baseY, h, spread, 0.0f, time, 0xFFFFE6FF, scroll * 2, 3);
		// the void sits just in front of the bright tear on both sides
		for (float z : new float[]{0.015f, -0.015f}) {
			tear(e, solid, baseY + h * 0.06f, h * 0.88f, spread * 0.55f, z, time, 0xFF1A0630, scroll * 0.5f, 3);
		}
		matrices.pop();

		// crystal shards circling the rift
		for (int i = 0; i < 6; i++) {
			matrices.push();
			float a = time * 0.04f + i * (float) Math.PI / 3;
			float r = 1.4f + 0.3f * MathHelper.sin(time * 0.05f + i);
			matrices.translate(MathHelper.cos(a) * r * open, 0.6f + (i % 3) * 1.4f + 0.3f * MathHelper.sin(time * 0.07f + i), MathHelper.sin(a) * r * open);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(time * 0.1f + i));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotation(0.6f));
			float s = 0.09f * open;
			box(matrices.peek(), solid, -s, s, -s * 3.5f, s * 3.5f, -s, s, 0xFFF0B8FF);
			box(matrices.peek(), soft, -s * 2, s * 2, -s * 5, s * 5, -s * 2, s * 2, 0x55C060FF);
			matrices.pop();
		}
		super.render(entity, yaw, tickDelta, matrices, buffers, light);
	}

	/** One layer of the tear, as a strip of quads from bottom to top in the view plane (z = depth). */
	private static void tear(MatrixStack.Entry e, VertexConsumer c, float y0, float height, float scale, float z, float time,
							 int color, float v0, int seed) {
		Matrix4f m = e.getPositionMatrix();
		for (int i = 0; i < SLICES; i++) {
			float f0 = (float) i / SLICES;
			float f1 = (float) (i + 1) / SLICES;
			float w0 = halfWidth(f0, time, seed) * scale;
			float w1 = halfWidth(f1, time, seed) * scale;
			float x0 = offset(f0, time);
			float x1 = offset(f1, time);
			float ya = y0 + f0 * height;
			float yb = y0 + f1 * height;
			float va = v0 + f0;
			float vb = v0 + f1;
			// both windings, so it shows from either side
			vertex(e, m, c, x0 - w0, ya, z, color, 0, va);
			vertex(e, m, c, x0 + w0, ya, z, color, 1, va);
			vertex(e, m, c, x1 + w1, yb, z, color, 1, vb);
			vertex(e, m, c, x1 - w1, yb, z, color, 0, vb);
			vertex(e, m, c, x1 - w1, yb, z, color, 0, vb);
			vertex(e, m, c, x1 + w1, yb, z, color, 1, vb);
			vertex(e, m, c, x0 + w0, ya, z, color, 1, va);
			vertex(e, m, c, x0 - w0, ya, z, color, 0, va);
		}
	}

	private static void box(MatrixStack.Entry e, VertexConsumer c, float x0, float x1, float y0, float y1, float z0, float z1, int color) {
		Matrix4f m = e.getPositionMatrix();
		float[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0},
				{x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1},
				{x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1},
				{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1},
				{x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0}};
		for (float[] q : faces) {
			vertex(e, m, c, q[0], q[1], q[2], color, 0, 0);
			vertex(e, m, c, q[3], q[4], q[5], color, 0, 1);
			vertex(e, m, c, q[6], q[7], q[8], color, 1, 1);
			vertex(e, m, c, q[9], q[10], q[11], color, 1, 0);
		}
	}

	private static void vertex(MatrixStack.Entry e, Matrix4f m, VertexConsumer c, float x, float y, float z, int color, float u, float v) {
		c.vertex(m, x, y, z).color(color).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(e, 0.0f, 1.0f, 0.0f);
	}

	@Override
	public Identifier getTexture(RiftEntity entity) {
		return TEXTURE;
	}
}
