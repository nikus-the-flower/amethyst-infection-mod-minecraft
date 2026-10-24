package com.amethystmod.block;

import net.minecraft.util.math.random.Random;

public enum CrystalColor {
	VIOLET("violet"),
	ROSE("rose"),
	AZURE("azure");

	public final String id;

	CrystalColor(String id) {
		this.id = id;
	}

	public static CrystalColor random(Random random) {
		CrystalColor[] values = values();
		return values[random.nextInt(values.length)];
	}
}
