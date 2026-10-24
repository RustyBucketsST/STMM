"""Turns the Blockbench models in blockbench/ into Java model code.

Each .bbmodel listed in blockbench/models.json becomes a class in com.skibiditoilet.client.bbmodel with one
`add<Group>(ModelPartData parent)` method per exported top-level group. A group's method also adds the
groups nested in it, except the ones listed under "separate" in models.json: those get their own method
and are only added where the Java model asks for them (variant extras such as the DJ's headphones).
Groups or cubes with Export switched off in Blockbench (reference parts like the Alliance suit) are skipped.

The models use Blockbench's "Modded Entity" format with box UV, which maps 1:1 onto Minecraft's
ModelPartBuilder: Blockbench's X and Y axes are flipped and its ground is at y = 0 where Minecraft's
is at y = 24. A rotated cube becomes a child part pivoting on the cube's origin, as Blockbench's own
Java export does.

Usage:
  python3 tools/bbmodel_to_java.py                     regenerate every class
  python3 tools/bbmodel_to_java.py --embed-textures    also copy the current PNGs into the .bbmodel files
                                                       (so Blockbench shows them even without the repo)
"""
import base64
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BB_DIR = ROOT / "blockbench"
PACKAGE = "com.skibiditoilet.client.bbmodel"
JAVA_DIR = ROOT / "src/main/java" / PACKAGE.replace(".", "/")


class Cube:
    def __init__(self, name, uv, origin, size, inflate, mirror, texture):
        self.name = name
        self.uv = uv            # (u, v) box UV offset
        self.origin = origin    # (x, y, z) Minecraft coordinates relative to the owning part's pivot
        self.size = size
        self.inflate = inflate
        self.mirror = mirror
        self.texture = texture  # index into the model's texture list (for previews)


class Part:
    def __init__(self, name, pivot, rotation, separate=False):
        self.name = name
        self.pivot = pivot        # Minecraft coordinates relative to the parent part's pivot
        self.rotation = rotation  # (pitch, yaw, roll) in radians
        self.cubes = []
        self.children = []
        self.separate = separate


def load(path, separate=(), include_unexported=False):
    """Reads a .bbmodel into a list of top-level Parts in Minecraft model space."""
    data = json.loads(Path(path).read_text())
    elements = {e["uuid"]: e for e in data["elements"]}

    def cube_texture(element):
        for face in element.get("faces", {}).values():
            if face.get("texture") is not None:
                return face["texture"]
        return 0

    def add_cube(part, element, origin):
        if not (element.get("export", True) or include_unexported) or element.get("type", "cube") != "cube":
            return
        f, t = element["from"], element["to"]
        size = [t[i] - f[i] for i in range(3)]
        cube_origin = element.get("origin", [0, 0, 0])
        rotation = element.get("rotation", [0, 0, 0])
        target, base = part, origin
        if any(rotation):
            # A rotated cube gets its own part, pivoting on the cube's origin.
            target = Part(f"{element['name']}_r", to_pivot(cube_origin, origin), to_rotation(rotation))
            part.children.append(target)
            base = cube_origin
        target.cubes.append(Cube(
            element["name"],
            tuple(element.get("uv_offset", [0, 0])),
            (base[0] - t[0], base[1] - t[1], f[2] - base[2]),
            tuple(size),
            element.get("inflate", 0),
            element.get("mirror_uv", False),
            cube_texture(element)))

    def add_group(group, parent_origin):
        if not (group.get("export", True) or include_unexported):
            return None
        origin = group.get("origin", [0, 0, 0])
        part = Part(group["name"], to_pivot(origin, parent_origin), to_rotation(group.get("rotation", [0, 0, 0])),
                    group["name"] in separate)
        for child in group.get("children", []):
            if isinstance(child, str):
                add_cube(part, elements[child], origin)
            else:
                sub = add_group(child, origin)
                if sub:
                    part.children.append(sub)
        return part

    parts, loose = [], Part("root", (0, 0, 0), (0, 0, 0))
    for node in data["outliner"]:
        if isinstance(node, str):
            add_cube(loose, elements[node], None)
        else:
            part = add_group(node, None)
            if part:
                parts.append(part)
    if loose.cubes or loose.children:
        raise SystemExit(f"{path}: put every cube in a group (found cubes at the top level)")
    return data, parts


def to_pivot(origin, parent_origin):
    if parent_origin is None:
        return (-origin[0], 24 - origin[1], origin[2])
    return (-(origin[0] - parent_origin[0]), -(origin[1] - parent_origin[1]), origin[2] - parent_origin[2])


def to_rotation(rotation):
    return (-math.radians(rotation[0]), -math.radians(rotation[1]), math.radians(rotation[2]))


# ---------------------------------------------------------------- Java output

def num(x):
    x = round(x, 4)
    if x == int(x):
        return str(int(x))
    return f"{x:g}f"


def rad(x):
    x = round(x, 6)
    return "0" if x == 0 else f"{x:g}f"


def camel(name):
    return "".join(w[:1].upper() + w[1:] for w in name.replace("-", "_").split("_"))


def transform(part):
    px, py, pz = part.pivot
    if any(part.rotation):
        return "ModelTransform.of({}, {}, {}, {}, {}, {})".format(*map(num, part.pivot), *map(rad, part.rotation))
    if px == py == pz == 0:
        return "ModelTransform.NONE"
    return "ModelTransform.pivot({}, {}, {})".format(*map(num, part.pivot))


def builder(part, indent):
    """The ModelPartBuilder expression for a part's cubes, ending in a comma (before any trailing comment)."""
    if not part.cubes:
        return "ModelPartBuilder.create(),"
    lines = ["ModelPartBuilder.create()"]
    mirrored = False
    for i, c in enumerate(part.cubes):
        call = ".uv({}, {})".format(*map(num, c.uv))
        if c.mirror != mirrored:
            call += ".mirrored()" if c.mirror else ".mirrored(false)"
            mirrored = c.mirror
        args = ", ".join(map(num, c.origin + c.size))
        if c.inflate:
            args += f", new Dilation({num(c.inflate)})"
        call += f".cuboid({args})"
        comma = "," if i == len(part.cubes) - 1 else ""
        lines.append(f"{indent}        {call}{comma}  // {c.name}")
    return "\n".join(lines)


def emit_part(part, parent_var, out, depth, indent="        "):
    """Adds the code that builds `part` (and its non-separate children) under `parent_var`."""
    var = "part" if depth == 0 else f"{part.name}{depth}"
    out.append(f'{indent}ModelPartData {var} = {parent_var}.addChild("{part.name}", {builder(part, indent)}')
    out.append(f"{indent}        {transform(part)});")
    for child in part.children:
        if not child.separate:
            emit_part(child, var, out, depth + 1, indent)
    return var


def methods(part, model_name, out):
    """One add method for `part`, then one for each separate group below it."""
    out.append("")
    out.append(f'    /** Adds group "{part.name}" from {model_name}. */')
    out.append(f"    public static ModelPartData add{camel(part.name)}(ModelPartData parent) {{")
    emit_part(part, "parent", out, 0)
    out.append("        return part;")
    out.append("    }")
    stack = list(part.children)
    while stack:
        child = stack.pop(0)
        if child.separate:
            methods(child, model_name, out)
        else:
            stack.extend(child.children)


def generate(bb_file, cls, separate):
    data, parts = load(bb_file, separate)
    res = data.get("resolution", {"width": 64, "height": 64})
    body = []
    for part in parts:
        methods(part, bb_file.name, body)
    uses_dilation = any("new Dilation(" in line for line in body)
    imports = ["net.minecraft.client.model.ModelPartBuilder", "net.minecraft.client.model.ModelPartData",
               "net.minecraft.client.model.ModelTransform"]
    if uses_dilation:
        imports.insert(0, "net.minecraft.client.model.Dilation")
    return "\n".join([
        f"package {PACKAGE};",
        "",
        *[f"import {i};" for i in imports],
        "",
        "/**",
        f" * Generated by tools/bbmodel_to_java.py from blockbench/{bb_file.name}. Don't edit this file:",
        " * change the model in Blockbench, then run the script again.",
        " */",
        f"public final class {cls} {{",
        f"    public static final int TEXTURE_WIDTH = {res['width']};",
        f"    public static final int TEXTURE_HEIGHT = {res['height']};",
        "",
        f"    private {cls}() {{",
        "    }",
        *body,
        "}",
        "",
    ])


def embed_textures(bb_file):
    data = json.loads(bb_file.read_text())
    for tex in data.get("textures", []):
        png = (bb_file.parent / tex["relative_path"]).resolve()
        tex["source"] = "data:image/png;base64," + base64.b64encode(png.read_bytes()).decode()
    bb_file.write_text(json.dumps(data, indent="\t") + "\n")


def main():
    config = json.loads((BB_DIR / "models.json").read_text())
    JAVA_DIR.mkdir(parents=True, exist_ok=True)
    for name, entry in config.items():
        bb_file = BB_DIR / name
        if "--embed-textures" in sys.argv:
            embed_textures(bb_file)
        java = JAVA_DIR / f"{entry['class']}.java"
        java.write_text(generate(bb_file, entry["class"], set(entry.get("separate", []))))
        print("wrote", java.relative_to(ROOT))


if __name__ == "__main__":
    main()
