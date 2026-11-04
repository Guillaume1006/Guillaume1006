"""Plaque française FG-669-AZ et pose en perspective sur un quadrilatère."""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"

def make_plate(text="FG-669-AZ", W=1040, H=220):
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=22, fill=(245, 245, 246, 255), outline=(20, 20, 20, 255), width=6)
    blue = (0, 51, 160, 255)
    d.rounded_rectangle([6, 6, 118, H - 7], radius=16, fill=blue)
    d.rectangle([60, 6, 118, H - 7], fill=blue)
    d.rounded_rectangle([W - 119, 6, W - 7, H - 7], radius=16, fill=blue)
    d.rectangle([W - 119, 6, W - 60, H - 7], fill=blue)
    # étoiles européennes + F
    cx, cy, r = 62, 72, 36
    for i in range(12):
        a = 2 * math.pi * i / 12
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(255, 204, 0, 255))
    f = ImageFont.truetype(FONT, 70)
    d.text((62, 160), "F", font=f, fill=(255, 255, 255, 255), anchor="mm")
    # numéro
    ft = ImageFont.truetype(FONT, 180)
    box = d.textbbox((0, 0), text, font=ft)
    tw = box[2] - box[0]
    sx = min(1.0, (W - 2 * 135) / tw)
    tmp = Image.new("RGBA", (tw + 20, 240), (0, 0, 0, 0))
    ImageDraw.Draw(tmp).text((10 - box[0], 120), text, font=ft, fill=(15, 15, 15, 255), anchor="lm")
    tmp = tmp.resize((int(tmp.width * sx), int(tmp.height * 0.9)), Image.LANCZOS)
    im.alpha_composite(tmp, ((W - tmp.width) // 2, (H - tmp.height) // 2 + 4))
    return im

def coeffs(dst, src):
    """Coefficients PIL PERSPECTIVE : pixel de sortie (dst) -> pixel source."""
    A, B = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); B.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); B.append(v)
    return np.linalg.solve(np.array(A, float), np.array(B, float)).tolist()

def paste_plate(base, quad, shade=0.93, ss=4):
    """quad = coins TL, TR, BR, BL de la plaque dans l'image."""
    plate = make_plate()
    W, H = base.size
    big = (W * ss, H * ss)
    q = [(x * ss, y * ss) for (x, y) in quad]
    src = [(0, 0), (plate.width, 0), (plate.width, plate.height), (0, plate.height)]
    warped = plate.transform(big, Image.PERSPECTIVE, coeffs(q, src), Image.BICUBIC)
    warped = warped.resize((W, H), Image.LANCZOS)
    a = np.array(warped).astype(float)
    a[:, :, :3] *= shade
    warped = Image.fromarray(a.clip(0, 255).astype("uint8"))
    out = base.copy()
    out.alpha_composite(warped)
    return out
