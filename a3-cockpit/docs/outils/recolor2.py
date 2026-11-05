"""Repeint la carrosserie blanche en gris quasi noir métallisé, teinte les vitres arrière.
Usage : python recolor2.py top34|front34 entrée.png sortie.png"""
import sys, math, os
import numpy as np
from PIL import Image, ImageDraw

CFG = {
    "top34": {
        "rims": [(60, 278, 37, 58, -15), (501, 677, 56, 70, -20)],
        "plate": [(816, 624), (818, 647), (938, 557), (935, 534)],
        "plate_quad": [(815, 622), (935, 532), (939, 558), (819, 649)],
        "keep": [],
        "keep_ell": [],
        "tint": [
            [(336, 226), (476, 326), (488, 452), (436, 458), (300, 332)],      # vitre porte AR
            [(484, 336), (566, 388), (598, 522), (556, 530), (488, 466)],      # custode
            [(640, 425), (856, 288), (942, 390), (950, 472), (730, 584), (698, 560)],  # lunette
        ],
        "front_glass": [
            [(115, 195), (150, 180), (210, 172), (330, 232), (318, 335), (290, 325), (120, 215)],  # vitre porte AV
        ],
        "glass_v": 0.55,                                          # ici le verre d'origine est sombre
    },
    "front34": {
        "rims": [(553, 388, 74, 108, 0), (945, 305, 36, 78, 0)],
        "plate": [(25, 310), (166, 324), (161, 380), (21, 362)],
        "plate_quad": [(23, 309), (167, 323), (162, 381), (19, 364)],
        "keep": [
            [(230, 296), (262, 248), (420, 236), (434, 262), (426, 292), (398, 302)],   # phare
        ],
        "keep_ell": [(343, 405, 34, 24), (100, 281, 46, 22)],                            # antibrouillard, anneaux                                                # antibrouillard
        "tint": [
            [(792, 20), (866, 28), (902, 108), (816, 124)],                              # vitre porte AR
            [(868, 34), (902, 44), (944, 106), (906, 110)],                              # custode
        ],
        "front_glass": [
            [(297, 132), (350, 95), (415, 40), (470, 24), (695, 20), (650, 145), (450, 136)],   # pare-brise
            [(676, 32), (700, 24), (790, 20), (800, 24), (803, 124), (656, 146), (668, 96)],    # vitre porte AV
        ],
        "mirror": (730, 106, 53, 22),
    },
}

def clamp(x): return np.clip(x, 0, 1)

def poly_mask(W, H, polys):
    m = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(m)
    for p in polys: d.polygon(p, fill=255)
    return np.array(m).astype(float) / 255.0

def main(cfg_name, src, out):
    cfg = CFG[cfg_name]
    im = Image.open(src).convert("RGBA")
    a = np.array(im).astype(float) / 255.0
    rgb, al = a[:, :, :3], a[:, :, 3]
    H, W = al.shape
    mx, mn = rgb.max(2), rgb.min(2)
    v = mx
    s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    yy, xx = np.mgrid[0:H, 0:W]

    w = clamp((v - 0.40) / 0.12) * clamp((0.17 - s) / 0.07)
    for (cx, cy, rx, ry, ang) in cfg["rims"]:
        c, sn = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        dx, dy = xx - cx, yy - cy
        u, vv = dx * c + dy * sn, -dx * sn + dy * c
        d = np.sqrt((u / rx) ** 2 + (vv / ry) ** 2)
        inside = clamp((0.95 - d) / 0.07)                     # jante (ellipse resserrée)
        bright = clamp((v - 0.80) / 0.06)                     # carrosserie blanche visible dans l'ellipse
        edge = clamp((d - 0.80) / 0.08)                       # seulement au bord de l'ellipse
        w *= 1 - inside * (1 - bright * edge)
    for (cx, cy, rx, ry) in cfg["keep_ell"]:
        w *= clamp((np.sqrt(((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2) - 1.0) / 0.15)
    pm = poly_mask(W, H, [cfg["plate"]])
    km = poly_mask(W, H, cfg["keep"]) if cfg["keep"] else np.zeros((H, W))
    w *= (1 - pm) * (1 - km)

    # gris quasi noir métallisé (neutre)
    t = clamp((v - 0.42) / 0.58)
    # A3_TEINTE : 0 = quasi noir (d'origine), 1 = anthracite, 2.2 = gris métal (APK 2.4), 3.0 = +15 % (défaut)
    k = float(os.environ.get("A3_TEINTE", "3.0"))
    dark = np.array([0.036, 0.037, 0.039]) * (1 - k) + np.array([0.085, 0.087, 0.091]) * k
    mid = np.array([0.140, 0.143, 0.148]) * (1 - k) + np.array([0.255, 0.260, 0.268]) * k
    light = np.array([0.40, 0.405, 0.415]) * (1 - k) + np.array([0.53, 0.535, 0.545]) * k   # reflets sobres
    k1 = t ** 1.25
    k2 = clamp((t - 0.72) / 0.28) ** 2.2
    col = dark * (1 - k1)[..., None] + mid * k1[..., None]
    col = col * (1 - k2)[..., None] + light * k2[..., None]
    rng = np.random.default_rng(7)                           # paillettes métallisées
    col = col + (rng.random((H, W, 1)) - 0.5) * 0.018 * (0.4 + t[..., None])
    new = rgb * (1 - w)[..., None] + col * w[..., None]
    # chromes conservés un peu atténués (moins blancs)
    chrome = km * clamp((v - 0.55) / 0.2) * clamp((0.2 - s) / 0.1)
    new = new * (1 - 0.45 * chrome)[..., None]

    # plaque blanche vierge (on garde la bande bleue)
    blue = (rgb[:, :, 2] > rgb[:, :, 0] + 0.15) & (s > 0.35)
    new[(pm > 0.5) & ~blue] = np.array([0.92, 0.92, 0.93])

    # vitres teintées à l'arrière : quasi noires, avec un léger reflet conservé
    from PIL import ImageFilter

    def glass_mask(polys):
        zone = poly_mask(W, H, polys)
        g = (zone > 0.5) & (v < cfg.get("glass_v", 0.90)) & (s < 0.30)   # verre seul (ni tôle, ni feu rouge)
        if "mirror" in cfg:
            mx_, my_, mrx, mry = cfg["mirror"]
            g &= ((xx - mx_) / mrx) ** 2 + ((yy - my_) / mry) ** 2 > 1.0
        gi = Image.fromarray((g * 255).astype("uint8"))
        gi = gi.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.7))
        return np.array(gi).astype(float) / 255.0

    tm = glass_mask(cfg["tint"])
    fm = glass_mask(cfg["front_glass"]) if cfg.get("front_glass") else np.zeros((H, W))
    lum = rgb.mean(2)
    tinted = np.stack([0.016 + lum * 0.05] * 3, axis=2)                       # arrière : quasi noir
    front = np.stack([0.030 + 0.13 * lum ** 1.6] * 3, axis=2)                  # avant : intérieur visible, reflets atténués
    front = front * np.array([0.97, 0.99, 1.03])                               # très léger reflet froid
    new = new * (1 - fm)[..., None] + front * fm[..., None]
    new = new * (1 - tm)[..., None] + tinted * tm[..., None]

    res = Image.fromarray((clamp(np.dstack([new, al])) * 255).astype("uint8"))
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from plate import paste_plate
    res = paste_plate(res, cfg["plate_quad"])
    res.save(out)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
