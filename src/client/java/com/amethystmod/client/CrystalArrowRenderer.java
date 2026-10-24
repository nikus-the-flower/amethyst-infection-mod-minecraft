package com.amethystmod.client;

import com.amethystmod.AmethystMod;
import com.amethystmod.entity.CrystalArrowEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

public class CrystalArrowRenderer extends ProjectileEntityRenderer<CrystalArrowEntity> {
	private static final Identifier TEXTURE = Identifier.of(AmethystMod.MOD_ID, "textures/entity/projectiles/crystal_arrow.png");

	public CrystalArrowRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(CrystalArrowEntity entity) {
		return TEXTURE;
	}
}
