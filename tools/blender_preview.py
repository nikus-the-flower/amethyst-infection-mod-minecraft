"""Runs inside Blender: rebuilds a game model from its JSON (exactly as Minecraft would pose it),
applies its texture and renders preview images.

    exec(open(".../blender_preview.py").read()); preview("amethyst_stalker", "C:/out", ["front", "back"])
"""
import json
import math
from pathlib import Path

import bmesh
import bpy
from mathutils import Euler, Matrix, Vector

try:
    PROJECT = Path(__file__).resolve().parent.parent
except NameError:  # run through exec(open(...).read())
    PROJECT = Path.cwd()
RES = PROJECT / "src/main/resources/assets/amethystmod"
SCALE = 1 / 16


def mc_to_blender(v):
    """Minecraft model space (y down, ground at y=24) to Blender (z up, ground at 0)."""
    return Vector((v.x * SCALE, v.z * SCALE, (24 - v.y) * SCALE))


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()
    for block in (bpy.data.meshes, bpy.data.materials, bpy.data.images, bpy.data.cameras, bpy.data.lights):
        for item in list(block):
            block.remove(item)


def build(name, textured=True, offsets=None):
    data = json.loads((RES / "geo" / f"{name}.json").read_text())
    tw, th = data["texture"]
    parts = {p["name"]: p for p in data["parts"]}
    world = {}

    def world_of(pname):
        if pname in world:
            return world[pname]
        p = parts[pname]
        pivot = Vector(p["pivot"])
        if offsets and p["parent"] is None and p["name"] in offsets:
            pivot = pivot + Vector(offsets[p["name"]])
        local = Matrix.Translation(pivot) @ Euler(
            [math.radians(a) for a in p["rot"]], "XYZ").to_matrix().to_4x4()
        parent = world_of(p["parent"]) if p["parent"] else Matrix.Identity(4)
        world[pname] = parent @ local
        return world[pname]

    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    uv = bm.loops.layers.uv.new("UVMap")

    def add_face(corners, uvrect):
        verts = [bm.verts.new(mc_to_blender(c)) for c in corners]
        face = bm.faces.new(verts)
        u0, v0, u1, v1 = uvrect
        # image space (top-left origin) to Blender UV (bottom-left origin)
        quad = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
        for loop, (u, v) in zip(face.loops, quad):
            loop[uv].uv = (u / tw, 1 - v / th)

    for pname in data["parts"]:
        pass
    for p in data["parts"]:
        m = world_of(p["name"])
        for c in p["cubes"]:
            ox, oy, oz = c["o"]
            w, h, d = c["s"]
            u, v = c["uv"]
            x0, y0, z0, x1, y1, z1 = ox, oy, oz, ox + w, oy + h, oz + d

            def P(x, y, z):
                return m @ Vector((x, y, z))

            # corner order chosen so quads wind consistently; uv rects follow the Minecraft box layout
            add_face([P(x0, y0, z0), P(x1, y0, z0), P(x1, y1, z0), P(x0, y1, z0)], (u + d, v + d, u + d + w, v + d + h))      # front (-z)
            add_face([P(x1, y0, z1), P(x0, y0, z1), P(x0, y1, z1), P(x1, y1, z1)], (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h))  # back (+z)
            add_face([P(x0, y0, z1), P(x0, y0, z0), P(x0, y1, z0), P(x0, y1, z1)], (u, v + d, u + d, v + d + h))              # -x
            add_face([P(x1, y0, z0), P(x1, y0, z1), P(x1, y1, z1), P(x1, y1, z0)], (u + d + w, v + d, u + 2 * d + w, v + d + h))  # +x
            add_face([P(x0, y0, z1), P(x1, y0, z1), P(x1, y0, z0), P(x0, y0, z0)], (u + d, v, u + d + w, v + d))              # top (-y)
            add_face([P(x0, y1, z0), P(x1, y1, z0), P(x1, y1, z1), P(x0, y1, z1)], (u + d + w, v, u + d + 2 * w, v + d))      # bottom (+y)
    bm.normal_update()
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)

    mat = bpy.data.materials.new(name + "_mat")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = next(n for n in nodes if n.type == "BSDF_PRINCIPLED")
    if textured:
        img = bpy.data.images.load(str(RES / "textures/entity" / f"{name}.png"))
        img.colorspace_settings.name = "sRGB"
        tex = nodes.new("ShaderNodeTexImage")
        tex.image = img
        tex.interpolation = "Closest"
        mat.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
        nodes.active = tex
    mesh.materials.append(mat)
    return obj


def preview(name, out_dir, views=("front", "back"), size=(900, 760)):
    clear_scene()
    obj = build(name)
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.render.resolution_x, scene.render.resolution_y = size
    scene.render.film_transparent = False
    shading = scene.display.shading
    shading.light = "STUDIO"
    shading.color_type = "TEXTURE"
    shading.show_object_outline = False
    scene.world = scene.world or bpy.data.worlds.new("w")
    scene.world.color = (0.09, 0.07, 0.13)

    dims = obj.dimensions
    center = Vector((0, 0, dims.z / 2))
    radius = max(dims.x, dims.y, dims.z) * 1.35 + 0.6
    cam_data = bpy.data.cameras.new("cam")
    cam_data.lens = 50
    cam = bpy.data.objects.new("cam", cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam
    target = bpy.data.objects.new("target", None)
    target.location = center
    scene.collection.objects.link(target)
    track = cam.constraints.new("TRACK_TO")
    track.target = target
    track.track_axis = "TRACK_NEGATIVE_Z"
    track.up_axis = "UP_Y"

    angles = {"front": 215, "back": 35, "side": 90}
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    paths = []
    for view in views:
        a = math.radians(angles[view])
        # the model faces -Y in Blender (Minecraft -Z); angle 180 looks at it head-on
        cam.location = center + Vector((math.sin(a) * radius, math.cos(a) * radius, radius * 0.25))
        scene.render.filepath = str(out / f"{name}_{view}.png")
        bpy.ops.render.render(write_still=True)
        paths.append(scene.render.filepath)
    return paths
