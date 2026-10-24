package com.amethystmod.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A cuboid model description loaded from assets/amethystmod/geo/NAME.json (exported by tools/geo.py). */
public final class GeoDef {
	public record Cube(float ox, float oy, float oz, int w, int h, int d, int u, int v) {
	}

	public record Part(String name, String parent, float px, float py, float pz,
					   float rx, float ry, float rz, List<Cube> cubes) {
	}

	public final int texW;
	public final int texH;
	public final String anim;
	public final List<Part> parts;
	public final Map<String, Part> byName = new HashMap<>();

	private GeoDef(int texW, int texH, String anim, List<Part> parts) {
		this.texW = texW;
		this.texH = texH;
		this.anim = anim;
		this.parts = parts;
		for (Part p : parts) {
			byName.put(p.name(), p);
		}
	}

	public static GeoDef load(String name) {
		String path = "/assets/amethystmod/geo/" + name + ".json";
		try (InputStream in = GeoDef.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing model " + path);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray tex = root.getAsJsonArray("texture");
			List<Part> parts = new ArrayList<>();
			for (JsonElement pe : root.getAsJsonArray("parts")) {
				JsonObject p = pe.getAsJsonObject();
				float[] pivot = floats(p.getAsJsonArray("pivot"));
				float[] rot = floats(p.getAsJsonArray("rot"));
				List<Cube> cubes = new ArrayList<>();
				for (JsonElement ce : p.getAsJsonArray("cubes")) {
					JsonObject c = ce.getAsJsonObject();
					float[] o = floats(c.getAsJsonArray("o"));
					float[] s = floats(c.getAsJsonArray("s"));
					float[] uv = floats(c.getAsJsonArray("uv"));
					cubes.add(new Cube(o[0], o[1], o[2], (int) s[0], (int) s[1], (int) s[2], (int) uv[0], (int) uv[1]));
				}
				String parent = p.get("parent").isJsonNull() ? null : p.get("parent").getAsString();
				parts.add(new Part(p.get("name").getAsString(), parent, pivot[0], pivot[1], pivot[2],
						(float) Math.toRadians(rot[0]), (float) Math.toRadians(rot[1]), (float) Math.toRadians(rot[2]), cubes));
			}
			return new GeoDef(tex.get(0).getAsInt(), tex.get(1).getAsInt(), root.get("anim").getAsString(), parts);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read model " + path, e);
		}
	}

	private static float[] floats(JsonArray array) {
		float[] out = new float[array.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = array.get(i).getAsFloat();
		}
		return out;
	}

	public TexturedModelData toModelData() {
		ModelData data = new ModelData();
		Map<String, ModelPartData> built = new HashMap<>();
		for (Part p : parts) {
			ModelPartData parent = p.parent() == null ? data.getRoot() : built.get(p.parent());
			ModelPartBuilder builder = ModelPartBuilder.create();
			for (Cube c : p.cubes()) {
				builder.uv(c.u(), c.v()).cuboid(c.ox(), c.oy(), c.oz(), c.w(), c.h(), c.d(), new Dilation(0.0f));
			}
			built.put(p.name(), parent.addChild(p.name(), builder,
					ModelTransform.of(p.px(), p.py(), p.pz(), p.rx(), p.ry(), p.rz())));
		}
		return TexturedModelData.of(data, texW, texH);
	}
}
