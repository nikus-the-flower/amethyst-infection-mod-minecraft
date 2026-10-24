package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/** Renders a {@link GeoModel} with its colour texture plus a full-bright glow layer. */
public class GeoRenderer<T extends MobEntity> extends MobEntityRenderer<T, GeoModel<T>> {
	private final java.util.function.Function<T, String> textureName;
	private final float scale;

	public GeoRenderer(EntityRendererFactory.Context ctx, GeoDef def, EntityModelLayer layer,
					   java.util.function.Function<T, String> textureName, float shadow, float scale) {
		super(ctx, new GeoModel<>(ctx.getPart(layer), def), shadow);
		this.textureName = textureName;
		this.scale = scale;
		addFeature(new Glow<>(this, textureName));
	}

	@Override
	public Identifier getTexture(T entity) {
		return Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + textureName.apply(entity) + ".png");
	}

	@Override
	protected void scale(T entity, MatrixStack matrices, float amount) {
		matrices.scale(scale, scale, scale);
	}

	@Override
	protected float getLyingAngle(T entity) {
		return 0.0f;
	}

	private static class Glow<T extends MobEntity> extends FeatureRenderer<T, GeoModel<T>> {
		private final java.util.function.Function<T, String> textureName;

		Glow(FeatureRendererContext<T, GeoModel<T>> context, java.util.function.Function<T, String> textureName) {
			super(context);
			this.textureName = textureName;
		}

		@Override
		public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
						   float limbAngle, float limbDistance, float tickDelta, float animationProgress,
						   float headYaw, float headPitch) {
			VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEyes(Identifier.of(AmethystMod.MOD_ID, "textures/entity/" + textureName.apply(entity) + "_glow.png")));
			getContextModel().render(matrices, consumer, 0xF000F0, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
		}
	}
}
