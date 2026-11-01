"""Génère coque.stl sans OpenSCAD (mêmes paramètres que coque.scad).
Usage : pip install manifold3d trimesh && python build_stl.py
"""
import numpy as np
import trimesh
from manifold3d import CrossSection, Manifold, JoinType

phone_w, phone_l, phone_t, phone_r = 77.5, 163.5, 8.4, 9
clearance, wall, back, lip, lip_h = 0.3, 1.6, 1.6, 1.2, 1.0
cam_w, cam_h, cam_from_top = 44, 28, 26
right_from, right_to, left_from, left_to, btn_h = 33, 82, 33, 78, 4.5
glyph_open, glyph_gap, glyph_band, bridge_w, bridge_angles = True, 2.0, 6.0, 4.0, [90, 210, 330]
quadlock, ql_w, ql_h, ql_r, ql_clear, ql_depth, ql_from_bottom = True, 56, 66, 8, 0.3, 0.6, 78
usb_w, usb_h, spk_w, spk_h = 14, 7.5, 58, 4.0
SEG, eps = 64, 0.01

W, L = phone_w + 2 * clearance, phone_l + 2 * clearance
H = back + phone_t + clearance + lip_h


def rrect(w, l, r, h):
    cs = CrossSection.square([w - 2 * r, l - 2 * r], center=True)
    return Manifold.extrude(cs.offset(r, JoinType.Round, circular_segments=SEG), h)


def stadium(w, h, depth):
    c = CrossSection.circle(h / 2, SEG)
    cs = CrossSection.batch_hull([c.translate([-(w - h) / 2, 0]), c.translate([(w - h) / 2, 0])])
    return Manifold.extrude(cs, depth)


def glyph_cut():
    d = back + 2 * eps
    band = stadium(cam_w + 2 * (glyph_gap + glyph_band), cam_h + 2 * (glyph_gap + glyph_band), d)
    band = band - stadium(cam_w + 2 * glyph_gap, cam_h + 2 * glyph_gap, d + 2 * eps).translate([0, 0, -eps])
    for a in bridge_angles:
        band = band - box(100, bridge_w, d + 2 * eps).translate([0, -bridge_w / 2, -eps]).rotate([0, 0, a])
    return band


def box(sx, sy, sz):
    return Manifold.cube([sx, sy, sz])


case = rrect(W + 2 * wall, L + 2 * wall, phone_r + clearance + wall, H).translate([0, L / 2, -back])
cuts = [
    rrect(W, L, phone_r + clearance, phone_t + clearance + eps).translate([0, L / 2, 0]),
    rrect(W - 2 * lip, L - 2 * lip, phone_r - lip, H).translate([0, L / 2, phone_t]),
    stadium(cam_w, cam_h, back + 2 * eps).translate([0, L - clearance - cam_from_top, -back - eps]),
]
if glyph_open:
    cuts.append(glyph_cut().translate([0, L - clearance - cam_from_top, -back - eps]))
if quadlock:
    cuts.append(rrect(ql_w + 2 * ql_clear, ql_h + 2 * ql_clear, ql_r + ql_clear, ql_depth + eps)
                .translate([0, clearance + ql_from_bottom, -back - eps]))
zc = phone_t / 2
cuts.append(box(wall + 2, right_to - right_from, btn_h).translate([W / 2 - 1, L - clearance - right_to, zc - btn_h / 2]))
cuts.append(box(wall + 2, left_to - left_from, btn_h).translate([-W / 2 - wall - 1, L - clearance - left_to, zc - btn_h / 2]))
for w, h in [(usb_w, usb_h), (spk_w, spk_h)]:
    cuts.append(stadium(w, h, wall + 2).translate([0, 0, -1]).rotate([90, 0, 0]).translate([0, 0, zc]))

for c in cuts:
    case = case - c

m = case.to_mesh()
mesh = trimesh.Trimesh(vertices=np.asarray(m.vert_properties)[:, :3], faces=np.asarray(m.tri_verts))
mesh.export("coque.stl")
print(f"coque.stl : {mesh.extents.round(1)} mm, étanche={mesh.is_watertight}, volume={mesh.volume/1000:.1f} cm3")
