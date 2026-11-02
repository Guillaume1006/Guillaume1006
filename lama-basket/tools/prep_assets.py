"""Prépare les illustrations HD de Lama Basket à partir des images Canva fournies.

- détourage des fonds verts (lama, ballon, panneau)
- effacement du chariot peint sur le rail (le jeu dessine sa propre rampe lumineuse et ses chariots dorés)
- masques du ciel et de l'ouverture des rideaux (pour les variantes de décor)
- repérage des ampoules (pour les faire clignoter)
Sorties dans lama-basket/web/img/ + assets.json (repères).
"""
import json
import sys
from collections import deque

import numpy as np
from PIL import Image, ImageFilter

U = sys.argv[1]
OUT = sys.argv[2]
BG, LAMA, BALL, BOARD = (U + '/b85bc055-image.png', U + '/8d86fdea-image.png',
                         U + '/1bded4bd-image.png', U + '/e2ad6b7f-image.png')


def chroma(path, t0=25, t1=90):
    a = np.asarray(Image.open(path).convert('RGB')).astype(np.float32)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    greenness = g - np.maximum(r, b)
    alpha = np.clip(1 - (greenness - t0) / (t1 - t0), 0, 1)
    # suppression du reflet vert sur les bords
    lim = np.maximum(r, b)
    g2 = np.where(g > lim, lim + (g - lim) * 0.15, g)
    out = np.dstack([r, g2, b, alpha * 255]).clip(0, 255).astype(np.uint8)
    im = Image.fromarray(out, 'RGBA')
    bbox = Image.fromarray((alpha > 0.05).astype(np.uint8) * 255).getbbox()
    return im, bbox


def crop_pad(im, bbox, pad):
    x0, y0, x1, y1 = bbox
    return im.crop((max(0, x0 - pad), max(0, y0 - pad), min(im.width, x1 + pad), min(im.height, y1 + pad)))


def blobs(mask, min_px, max_px):
    """Composantes connexes (4-voisinage) d'un masque booléen -> centres et rayons."""
    h, w = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    res = []
    ys, xs = np.nonzero(mask)
    for y, x in zip(ys, xs):
        if seen[y, x]:
            continue
        q = deque([(y, x)])
        seen[y, x] = True
        pts = []
        while q:
            cy, cx = q.popleft()
            pts.append((cy, cx))
            for ny, nx in ((cy + 1, cx), (cy - 1, cx), (cy, cx + 1), (cy, cx - 1)):
                if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True
                    q.append((ny, nx))
        if min_px <= len(pts) <= max_px:
            p = np.array(pts)
            cy, cx = p.mean(0)
            res.append((float(cx), float(cy), float(np.sqrt(len(pts) / np.pi))))
    return res


meta = {}

# ---------- lama ----------
im, bbox = chroma(LAMA)
lama = crop_pad(im, bbox, 6)
lama = lama.resize((round(lama.width * 520 / lama.height), 520), Image.LANCZOS)
lama.save(OUT + '/lama.png', optimize=True)
meta['lama'] = {'w': lama.width, 'h': lama.height}

# ---------- ballon ----------
im, bbox = chroma(BALL)
x0, y0, x1, y1 = bbox
cx, cy, rad = (x0 + x1) / 2, (y0 + y1) / 2, min(x1 - x0, y1 - y0) / 2
ball = im.crop((int(cx - rad), int(cy - rad), int(cx + rad), int(cy + rad))).resize((256, 256), Image.LANCZOS)
yy, xx = np.mgrid[0:256, 0:256]
d = np.sqrt((xx - 127.5) ** 2 + (yy - 127.5) ** 2)
circ = np.clip(127.0 - d, 0, 1)
arr = np.asarray(ball).copy()
arr[..., 3] = (circ * 255).astype(np.uint8)
Image.fromarray(arr, 'RGBA').save(OUT + '/ball.png', optimize=True)

# ---------- panneau ----------
im, bbox = chroma(BOARD)
board = crop_pad(im, bbox, 4)
scale = 560 / board.width
board = board.resize((560, round(board.height * scale)), Image.LANCZOS)
board.save(OUT + '/board.png', optimize=True)
ba = np.asarray(board).astype(int)
bright = (ba[..., 0] > 240) & (ba[..., 1] > 225) & (ba[..., 2] > 150) & (ba[..., 3] > 200)
bb = blobs(bright, 25, 900)
meta['board'] = {'w': board.width, 'h': board.height,
                 'bulbs': [[round(x / board.width, 4), round(y / board.height, 4), round(r / board.width, 4)] for x, y, r in bb]}

# ---------- décor ----------
src = Image.open(BG).convert('RGB')
W, H = src.size
a = np.asarray(src).astype(np.float32)

# chariot peint : effacé (le jeu dessine la rampe et les chariots)
TX0, TX1, TY0, TY1 = 452, 612, 334, 430
fixed = a.copy()
AX, AY = 472.0, 205.0          # sommet du chapiteau (les rayures partent de là)
RAIL0, RAIL1 = 370, 423        # bande du rail
for y in range(TY0, TY1):
    for x in range(TX0, TX1):
        if RAIL0 <= y <= RAIL1:
            fixed[y, x] = a[y, x - 165]           # le rail est uniforme horizontalement
        else:
            ty = TY0 - 4 if y < RAIL0 else TY1 + 4  # même rayon, juste au-dessus / au-dessous
            sx = AX + (x - AX) * (ty - AY) / (y - AY)
            sx0 = int(np.floor(sx)); f = sx - sx0
            fixed[y, x] = a[ty, sx0] * (1 - f) + a[ty, sx0 + 1] * f
# adoucir la couture
blur = Image.fromarray(fixed.clip(0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2))
bl = np.asarray(blur).astype(np.float32)
m = np.zeros((H, W), np.float32)
m[TY0 - 2:TY1 + 2, TX0 - 2:TX1 + 2] = 1
m = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))).astype(np.float32) / 255
edge = m * (1 - m) * 4
fixed = fixed * (1 - edge[..., None] * 0.6) + bl * (edge[..., None] * 0.6)
bgimg = Image.fromarray(fixed.clip(0, 255).astype(np.uint8))
bgimg.save(OUT + '/bg.jpg', quality=88, optimize=True, progressive=True)

r, g, b = a[..., 0], a[..., 1], a[..., 2]
yy = np.mgrid[0:H, 0:W][0]
# ciel : bleu nuit dans le haut de l'image
sky = (b > 45) & (b > r + 25) & (b > g + 12) & (yy < 560)
skyimg = Image.fromarray((sky * 255).astype(np.uint8)).filter(ImageFilter.MedianFilter(5)).filter(ImageFilter.GaussianBlur(1.5))
# ouverture : fond rouge sombre entre les rideaux
xx = np.mgrid[0:H, 0:W][1]
opening = (r < 138) & (g < 42) & (b < 40) & (yy > 820) & (yy < 1195) & (xx > 255) & (xx < 690)
openimg = Image.fromarray((opening * 255).astype(np.uint8)).filter(ImageFilter.MedianFilter(7)).filter(ImageFilter.GaussianBlur(2))
for name, mimg in (('sky_mask.png', skyimg), ('open_mask.png', openimg)):
    half = mimg.resize((W // 2, H // 2), Image.LANCZOS)
    z = np.zeros((H // 2, W // 2, 4), np.uint8)
    z[..., 3] = np.asarray(half)
    Image.fromarray(z, 'RGBA').save(OUT + '/' + name, optimize=True)

# ampoules du décor
bright = (r > 245) & (g > 215) & (b > 120) & ~((yy > 325) & (yy < 435)) & ~((yy > 580) & (yy < 805) & (((xx > 38) & (xx < 250)) | ((xx > 692) & (xx < 905))))
bl_ = blobs(bright, 12, 1500)
meta['bg'] = {
    'w': W, 'h': H,
    'rail': 386, 'railBottom': 421, 'rim': 680, 'ball': 1440, 'floor': 1230,
    'tv': [[40, 590, 248, 800], [695, 592, 902, 800]],
    'podium': [472, 1505],
    'trolley': [TX0, TY0, TX1, TY1],
    'skyTop': [int(v) for v in a[0:8].reshape(-1, 3).mean(0)],
    'bulbs': [[round(x, 1), round(y, 1), round(r_, 1)] for x, y, r_ in bl_],
}
json.dump(meta, open(OUT + '/assets.json', 'w'))
print('bulbs bg', len(bl_), 'board', len(bb), 'lama', lama.size, 'board', board.size)
