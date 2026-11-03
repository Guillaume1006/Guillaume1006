"""Efface le rail métallique peint en travers du chapiteau dans img/bg.jpg (et met à jour img/sky_mask.png).

Le jeu dessine sa propre rampe lumineuse plus haut, pour suspendre le panier au-dessus du chapiteau.
- toile : reconstruction le long des rayures (rayons partant du sommet), en fondu entre la rangée
  au-dessus et la rangée au-dessous du rail ; l'ombre du chariot peint est remplacée par la bande rouge voisine ;
- ciel : interpolation verticale des pixels de ciel de chaque colonne (sans les fanions).
Usage : python3 erase_rail.py <dossier img>   (à lancer une seule fois, après prep_assets.py)
"""
import sys

import numpy as np
from PIL import Image, ImageFilter

D = sys.argv[1]
a = np.asarray(Image.open(D + '/bg.jpg').convert('RGB')).astype(np.float32)
H, W, _ = a.shape
Y0, Y1 = 361, 437          # bande à reconstruire
TOPR, BOTR = 354, 433      # rangées sources (au-dessus / au-dessous du rail)
AX, AY = 472.0, 205.0      # sommet du chapiteau


def samp(row, x):
    x = np.clip(x, 0, W - 1.001)
    x0 = np.floor(x).astype(int)
    f = (x - x0)[..., None]
    return a[row, x0] * (1 - f) + a[row, x0 + 1] * f


# ombre du chariot peint sous le rail, au centre : bande rouge voisine (même hauteur, même ombrage)
for y in range(416, 448):
    sx = 597 + (np.arange(426, 523) - 428) * (676 - 597) / (520 - 428)
    a[y, 426:523] = samp(y, sx.astype(np.float32))
sm = np.asarray(Image.fromarray(a.clip(0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.5))).astype(np.float32)
for x in list(range(422, 431)) + list(range(518, 527)):
    a[416:448, x] = a[416:448, x] * 0.4 + sm[416:448, x] * 0.6


def is_sky(p):
    return p[0] < 60 and p[1] < 95 and p[2] > 50 and p[2] > p[0] + 30


def sky_col(rows, x):
    pts = [a[r, x] for r in rows if is_sky(a[r, x])]
    return np.median(pts, 0) if len(pts) >= 3 else None


def fill(vals):
    idx = [i for i, v in enumerate(vals) if v is not None]
    out = np.zeros((W, 3), np.float32)
    for c in range(3):
        out[:, c] = np.interp(np.arange(W), idx, [vals[i][c] for i in idx])
    return out


skyTop = fill([sky_col(range(300, 361), x) for x in range(W)])
skyBot = fill([sky_col(range(430, 446), x) for x in range(W)])
xs = np.arange(W, dtype=np.float32)
res = a.copy()
for y in range(Y0, Y1 + 1):
    t = (y - Y0) / (Y1 - Y0)
    w = t * t * (3 - 2 * t)
    xa = AX + (xs - AX) * (TOPR - AY) / (y - AY)
    xb = AX + (xs - AX) * (BOTR - AY) / (y - AY)
    roof = (1 - w) * samp(TOPR, xa) + w * samp(BOTR, xb)
    sky = skyTop * (1 - t) + skyBot * t
    u = (y - 362) / (430 - 362)                     # bords du toit (silhouette) interpolés
    L, R = 257 + (124 - 257) * u, 703 + (813 - 703) * u
    m = np.clip(np.minimum(xs - L, R - xs) + 0.5, 0, 1)[..., None]
    res[y] = roof * m + sky * (1 - m)
res[Y0:Y1 + 1] += np.random.default_rng(3).normal(0, 1.6, res[Y0:Y1 + 1].shape)
bl = np.asarray(Image.fromarray(res.clip(0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.0))).astype(np.float32)
for y, k in ((Y0 - 1, 0.5), (Y0, 0.7), (Y0 + 1, 0.5), (Y1 - 1, 0.5), (Y1, 0.7), (Y1 + 1, 0.5)):
    res[y] = res[y] * (1 - k) + bl[y] * k
out = res.clip(0, 255).astype(np.uint8)
Image.fromarray(out).save(D + '/bg.jpg', quality=90, optimize=True, progressive=True)

# masque du ciel : on recalcule la bande (le ciel y est maintenant visible)
o = out.astype(int)
r, g, b = o[..., 0], o[..., 1], o[..., 2]
sky = ((b > 45) & (b > r + 25) & (b > g + 12)).astype(np.uint8) * 255
sk = np.asarray(Image.fromarray(sky).filter(ImageFilter.MedianFilter(5)).filter(ImageFilter.GaussianBlur(1.5)))
mask = Image.open(D + '/sky_mask.png').convert('RGBA')
mw, mh = mask.size
half = np.asarray(Image.fromarray(sk).resize((mw, mh), Image.LANCZOS))
ma = np.asarray(mask).copy()
y0, y1 = (Y0 - 8) * mh // H, (Y1 + 10) * mh // H
ma[y0:y1, :, 3] = half[y0:y1]
Image.fromarray(ma, 'RGBA').save(D + '/sky_mask.png', optimize=True)
print('rail effacé, bande', Y0, Y1)
