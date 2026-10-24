package com.amethystmod.client;

import com.amethystmod.entity.BeamEffectEntity;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Draws {@link BeamEffectEntity}: glowing 3D beams and ring walls made of a bright core and a soft
 * outer glow, with light running along them. Their size is the size of their hitbox.
 */
public class BeamEffectRenderer extends EntityRenderer<BeamEffectEntity> {
	private static final Identifier TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;
	private static final int CORE = 0xFFFFE0FF;
	private static final int GLOW = 0x88C060FF;
	private static final int AIM = 0xCCFF4FA8;
	/** Bright core and soft glow of each colour (amethyst, pure). */
	private static final int[][] PALETTE = {{CORE, GLOW}, {0xFFF4FFFF, 0x8890D8FF}};

	public BeamEffectRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(BeamEffectEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(BeamEffectEntity entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider buffers, int light) {
		float time = entity.age + tickDelta;
		float scroll = -time * 0.12f;
		VertexConsumer solid = buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, false));
		VertexConsumer soft = buffers.getBuffer(RenderLayer.getBeaconBeam(TEXTURE, true));
		matrices.push();
		int[] colors = PALETTE[MathHelper.clamp(entity.getColor(), 0, PALETTE.length - 1)];
		Entity owner = entity.getOwnerId() >= 0 ? entity.getWorld().getEntityById(entity.getOwnerId()) : null;
		if (owner != null) {
			// stay glued to the owner: draw at its smoothed position, not ours
			matrices.translate(MathHelper.lerp(tickDelta, owner.prevX, owner.getX()) - MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
					MathHelper.lerp(tickDelta, owner.prevY, owner.getY()) - MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
					MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ()) - MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
		}
		float fade = Math.min(1.0f, Math.min((entity.age + tickDelta) / 4.0f, (entity.getLife() - entity.age - tickDelta) / 6.0f));
		fade = Math.max(0.0f, fade);
		switch (entity.getMode()) {
			case BeamEffectEntity.AIM, BeamEffectEntity.FIRE -> {
				float a = MathHelper.lerp(tickDelta, entity.prevAngle, entity.getAngle());
				float p = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getBeamPitch());
				matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-a));
				matrices.multiply(RotationAxis.POSITIVE_Z.rotation(p));
				float len = entity.getLength();
				float w = entity.getBeamWidth();
				MatrixStack.Entry e = matrices.peek();
				if (entity.getMode() == BeamEffectEntity.AIM) {
					// a thin, flickering sight line
					int alpha = 120 + (int) (100 * Math.abs(MathHelper.sin(time * 0.6f)));
					box(e, soft, 0, len, -w, w, -w, w, (AIM & 0x00FFFFFF) | (alpha << 24), scroll, len * 0.3f);
					box(e, soft, 0, len, -w * 3, w * 3, -w * 3, w * 3, 0x30FF60C0, scroll, len * 0.3f);
				} else {
					float pulse = 1.0f + 0.08f * MathHelper.sin(time * 1.5f);
					float core = w * 0.5f * pulse;
					box(e, solid, 0, len, -core, core, -core, core, CORE, scroll * 3, len * 0.25f);
					box(e, soft, 0, len, -w, w, -w, w, GLOW, scroll * 2, len * 0.25f);
					box(e, soft, 0, len, -w * 1.5f, w * 1.5f, -w * 1.5f, w * 1.5f, 0x40B050FF, scroll, len * 0.25f);
				}
			}
			case BeamEffectEntity.SWEEP -> {
				float a = entity.sweepAngle(tickDelta);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-a));
				float len = entity.getLength();
				float inner = entity.getInner();
				float w = entity.getBeamWidth();
				MatrixStack.Entry e = matrices.peek();
				if (entity.age + tickDelta < entity.getDelay()) {
					// before it starts turning: a faint line showing where it lies
					int alpha = 60 + (int) (60 * Math.abs(MathHelper.sin(time * 0.4f)));
					box(e, soft, inner, len, 0.02f, 0.12f, -w * 0.4f, w * 0.4f, (GLOW & 0x00FFFFFF) | (alpha << 24), scroll, len * 0.3f);
				} else {
					box(e, solid, inner, len, 0.05f, 0.7f, -w * 0.45f, w * 0.45f, CORE, scroll * 3, len * 0.25f);
					box(e, soft, inner, len, 0.02f, 0.9f, -w, w, GLOW, scroll * 2, len * 0.25f);
				}
			}
			case BeamEffectEntity.RING -> {
				if (entity.age + tickDelta >= entity.getDelay()) {
					float r = entity.ringRadius(tickDelta);
					float h = entity.getBeamWidth();
					ring(matrices.peek(), solid, r, 0.02f, h * 0.8f, colors[0], scroll * 3);
					ring(matrices.peek(), soft, r + 0.25f, 0.0f, h, colors[1], scroll * 2);
					ring(matrices.peek(), soft, r - 0.25f, 0.0f, h, colors[1], scroll * 2);
				}
			}
			case BeamEffectEntity.WHIRL -> {
				// three crescent slashes circling at waist height, tilted a little each
				float r = entity.getLength();
				float turn = entity.getAngle() + entity.getSpeed() * time;
				matrices.translate(0, 1.0, 0);
				for (int k = 0; k < 3; k++) {
					matrices.push();
					matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-(turn + k * (float) Math.PI * 2 / 3)));
					matrices.multiply(RotationAxis.POSITIVE_X.rotation((k - 1) * 0.18f));
					MatrixStack.Entry e = matrices.peek();
					crescent(e, soft, r * 1.08f, 110, r * 0.42f, 0.10f, withAlpha(colors[1], fade), scroll);
					crescent(e, solid, r, 95, r * 0.22f, 0.04f, withAlpha(colors[0], fade), scroll * 2);
					matrices.pop();
				}
				ring(matrices.peek(), soft, r * 1.02f, -0.03f, 0.03f, withAlpha(colors[1] & 0x44FFFFFF, fade), scroll);
			}
			case BeamEffectEntity.DOME -> {
				float r = entity.getLength();
				float pulse = 0.85f + 0.15f * MathHelper.sin(time * 0.12f);
				matrices.translate(0, 0.9, 0);
				sphere(matrices.peek(), soft, r, withAlpha(colors[1] & 0x30FFFFFF, fade * pulse), scroll * 0.3f);
				sphereLines(matrices.peek(), soft, r * 1.005f, time, withAlpha(colors[0] & 0x99FFFFFF, fade * pulse));
			}
			default -> {
			}
		}
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, buffers, light);
	}

	private static int withAlpha(int color, float factor) {
		int a = Math.round(((color >>> 24) & 0xFF) * MathHelper.clamp(factor, 0, 1));
		return (a << 24) | (color & 0x00FFFFFF);
	}

	/**
	 * A crescent slash lying flat (in X-Z), bulging toward +X: an arc of {@code arcDeg} degrees at radius
	 * {@code r}, {@code width} wide in the middle and sharp at both ends, {@code thick} thick.
	 */
	public static void crescent(MatrixStack.Entry e, VertexConsumer c, float r, float arcDeg, float width, float thick,
								int color, float v0) {
		int n = 14;
		float half = (float) Math.toRadians(arcDeg) / 2;
		for (int i = 0; i < n; i++) {
			float a0 = -half + 2 * half * i / n;
			float a1 = -half + 2 * half * (i + 1) / n;
			float w0 = width * MathHelper.cos((float) Math.PI * (i / (float) n - 0.5f));
			float w1 = width * MathHelper.cos((float) Math.PI * ((i + 1) / (float) n - 0.5f));
			float ox0 = MathHelper.cos(a0) * r;
			float oz0 = MathHelper.sin(a0) * r;
			float ox1 = MathHelper.cos(a1) * r;
			float oz1 = MathHelper.sin(a1) * r;
			float ix0 = MathHelper.cos(a0) * (r - w0);
			float iz0 = MathHelper.sin(a0) * (r - w0);
			float ix1 = MathHelper.cos(a1) * (r - w1);
			float iz1 = MathHelper.sin(a1) * (r - w1);
			float v = v0 + i / (float) n;
			for (float y : new float[]{thick, -thick}) {
				quad(e, c, ix0, y, iz0, ox0, y, oz0, ox1, y, oz1, ix1, y, iz1, color, v, v + 1.0f / n);
			}
			quad(e, c, ox0, -thick, oz0, ox1, -thick, oz1, ox1, thick, oz1, ox0, thick, oz0, color, v, v + 1.0f / n);
		}
	}

	/** A sphere of radius r, drawn from both sides. */
	private static void sphere(MatrixStack.Entry e, VertexConsumer c, float r, int color, float v0) {
		int lon = 20;
		int lat = 12;
		for (int i = 0; i < lat; i++) {
			float p0 = (float) Math.PI * (i / (float) lat - 0.5f);
			float p1 = (float) Math.PI * ((i + 1) / (float) lat - 0.5f);
			for (int j = 0; j < lon; j++) {
				float t0 = (float) Math.PI * 2 * j / lon;
				float t1 = (float) Math.PI * 2 * (j + 1) / lon;
				quad(e, c, sx(r, p0, t0), sy(r, p0), sz(r, p0, t0), sx(r, p0, t1), sy(r, p0), sz(r, p0, t1),
						sx(r, p1, t1), sy(r, p1), sz(r, p1, t1), sx(r, p1, t0), sy(r, p1), sz(r, p1, t0), color, v0, v0 + 0.3f);
			}
		}
	}

	/** Glowing meridians and parallels over a sphere, slowly turning. */
	private static void sphereLines(MatrixStack.Entry e, VertexConsumer c, float r, float time, int color) {
		float w = 0.035f;
		for (int j = 0; j < 6; j++) {
			float t = (float) Math.PI * j / 3 + time * 0.02f;
			for (int i = 0; i < 12; i++) {
				float p0 = (float) Math.PI * (i / 12.0f - 0.5f);
				float p1 = (float) Math.PI * ((i + 1) / 12.0f - 0.5f);
				quad(e, c, sx(r, p0, t - w), sy(r, p0), sz(r, p0, t - w), sx(r, p0, t + w), sy(r, p0), sz(r, p0, t + w),
						sx(r, p1, t + w), sy(r, p1), sz(r, p1, t + w), sx(r, p1, t - w), sy(r, p1), sz(r, p1, t - w), color, 0, 1);
			}
		}
		for (float lat : new float[]{-0.5f, 0.0f, 0.5f}) {
			for (int j = 0; j < 24; j++) {
				float t0 = (float) Math.PI * 2 * j / 24;
				float t1 = (float) Math.PI * 2 * (j + 1) / 24;
				quad(e, c, sx(r, lat - w, t0), sy(r, lat - w), sz(r, lat - w, t0), sx(r, lat - w, t1), sy(r, lat - w), sz(r, lat - w, t1),
						sx(r, lat + w, t1), sy(r, lat + w), sz(r, lat + w, t1), sx(r, lat + w, t0), sy(r, lat + w), sz(r, lat + w, t0), color, 0, 1);
			}
		}
	}

	private static float sx(float r, float lat, float lon) {
		return r * MathHelper.cos(lat) * MathHelper.cos(lon);
	}

	private static float sy(float r, float lat) {
		return r * MathHelper.sin(lat);
	}

	private static float sz(float r, float lat, float lon) {
		return r * MathHelper.cos(lat) * MathHelper.sin(lon);
	}

	/** A box along X, drawn from both sides. */
	static void box(MatrixStack.Entry e, VertexConsumer c, float x0, float x1, float y0, float y1,
							float z0, float z1, int color, float v0, float vLen) {
		float v1 = v0 + vLen;
		quad(e, c, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, color, v0, v1);
		quad(e, c, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, color, v0, v1);
		quad(e, c, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, color, v0, v1);
		quad(e, c, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, color, v0, v1);
	}

	/** A circular wall of radius r from y0 to y1, drawn from both sides. */
	private static void ring(MatrixStack.Entry e, VertexConsumer c, float r, float y0, float y1, int color, float v0) {
		if (r <= 0.1f) {
			return;
		}
		int segments = Math.max(24, Math.min(96, (int) (r * 4)));
		for (int i = 0; i < segments; i++) {
			float a0 = (float) (Math.PI * 2 * i / segments);
			float a1 = (float) (Math.PI * 2 * (i + 1) / segments);
			float x0 = MathHelper.cos(a0) * r;
			float z0 = MathHelper.sin(a0) * r;
			float x1 = MathHelper.cos(a1) * r;
			float z1 = MathHelper.sin(a1) * r;
			quad(e, c, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, color, v0, v0 + 0.5f);
		}
	}

	static void quad(MatrixStack.Entry e, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz, int color, float v0, float v1) {
		Matrix4f m = e.getPositionMatrix();
		// both windings, so the face shows from either side
		vertex(e, m, c, ax, ay, az, color, 0, v0);
		vertex(e, m, c, bx, by, bz, color, 0, v1);
		vertex(e, m, c, cx, cy, cz, color, 1, v1);
		vertex(e, m, c, dx, dy, dz, color, 1, v0);
		vertex(e, m, c, dx, dy, dz, color, 1, v0);
		vertex(e, m, c, cx, cy, cz, color, 1, v1);
		vertex(e, m, c, bx, by, bz, color, 0, v1);
		vertex(e, m, c, ax, ay, az, color, 0, v0);
	}

	private static void vertex(MatrixStack.Entry e, Matrix4f m, VertexConsumer c, float x, float y, float z, int color, float u, float v) {
		c.vertex(m, x, y, z).color(color).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(e, 0.0f, 1.0f, 0.0f);
	}

	@Override
	public Identifier getTexture(BeamEffectEntity entity) {
		return TEXTURE;
	}
}
