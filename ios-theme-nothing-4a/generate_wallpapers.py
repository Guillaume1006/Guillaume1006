"""Génère des fonds d'écran style iOS (dégradés flous) en 1084x2392 (Nothing Phone)."""
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

W, H = 1084, 2392
OUT = Path(__file__).parent / "wallpapers"

THEMES = {
    "ios-bleu":   ("#0a1f5c", ["#2f7bff", "#7b5cff", "#00c6ff", "#ff6bd6"]),
    "ios-violet": ("#1b0b3a", ["#a35cff", "#ff5cae", "#5c7bff", "#ffb35c"]),
    "ios-aurore": ("#10141f", ["#34e0a1", "#2f9bff", "#a35cff", "#ff8a5c"]),
    "ios-clair":  ("#eef3ff", ["#8ec5ff", "#d1b3ff", "#ffc2e2", "#ffe0a3"]),
    "ios-noir":   ("#000000", ["#1c1c2e", "#2a2a4a", "#3a1c4a", "#12324a"]),
}

for name, (bg, colors) in THEMES.items():
    rng = random.Random(name)
    img = Image.new("RGB", (W, H), bg)
    layer = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(layer)
    for i, c in enumerate(colors):
        r = rng.randint(450, 700)
        x = rng.randint(-100, W + 100)
        y = int((i + 0.5) * H / len(colors)) + rng.randint(-200, 200)
        d.ellipse([x - r, y - r, x + r, y + r], fill=c)
    layer = layer.filter(ImageFilter.GaussianBlur(260))
    img = Image.blend(img, layer, 0.95)
    img.save(OUT / f"{name}.png", optimize=True)
    print("ok", name)
