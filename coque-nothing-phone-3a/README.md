# Coque Nothing Phone (3a)

Coque imprimable en 3D, paramétrique : coque en TPU 95A avec ouvertures Glyph, et support
quart de tour compatible Quad Lock en PETG, qui se loge dans le dos de la coque.

| Fichier | Rôle |
|---|---|
| `coque.stl` | Coque, à imprimer en **TPU 95A** |
| `support_quadlock.stl` | Support Quad Lock (insert), à imprimer en **PETG** |
| `bambu_1_support_quadlock_PETG.3mf`, `bambu_2_coque_TPU.3mf` | **Prêts pour Bambu Studio** (orientés, centrés) — voir `BAMBU.md` |
| `coque.scad` | Modèle OpenSCAD (`part = "coque"` ou `"support"`), toutes les cotes en tête de fichier |
| `build_stl.py` | Régénère les STL sans OpenSCAD (`pip install manifold3d trimesh`) |
| `make_bambu.py` | Régénère les 3MF à partir des STL |
| `rendu_3d.png` | Rendu 3D |
| `verification_photo.png` | Découpes superposées à la photo officielle (rouge = ouvertures, orange = logement du support) |

## Cotes
- Téléphone : 163,52 × 77,50 × 8,35 mm, 201 g (dimensions officielles Nothing)
- Coque : 167,3 × 81,3 × 11,2 mm (+ 4,5 mm de plot Quad Lock une fois le support posé)
- Bords 1,6 mm, dos 1,6 mm, jeu 0,3 mm, rebord d'écran 1,2 mm (TPU 95A)
- Positions calées sur la photo officielle du dos (voir `verification_photo.png`) :
  - **Caméra** : ouverture qui épouse le bloc caméra noir (51,8 × 23,8 mm), centre à 35,9 mm du haut
  - **Flash** : trou Ø 7 mm, à 17 mm du haut et 6,7 mm à gauche du centre (vu de dos)
  - **Glyph** : 3 fentes en arc de 3,4 mm sur un rayon de 32,9 mm autour du disque (haut-gauche, droite,
    bas-gauche vu de dos). Options `glyph_open`, `glyph_r`, `glyph_band`, `glyph_arcs`.
  - **Boutons** : Power + Essential Key à 59–101 mm du haut (droite), volume à 53–86 mm (gauche)
- USB-C + haut-parleur/micros (bas)
- **Support Quad Lock (PETG)** : plaque carrée 52 × 52 × 0,8 mm + plot Ø 44 × 4,5 mm.
  La plaque se loge dans un creux à l'intérieur du dos (centré à 70 mm du bas), le plot sort
  par un trou Ø 44,4 mm ; le téléphone bloque le tout, le carré empêche la rotation.
  On présente le téléphone à 45° sur le support, on appuie, on tourne d'un quart de tour.
  Options `quadlock`, `ql_*`, `ins_*`.

⚠️ **Cotes estimées.** Quad Lock ne publie pas les cotes de son interface : celles-ci
sont des estimations. **Imprime d'abord le support PETG** (petit, rapide) et essaie-le sur ton support :
- n'entre pas → augmente `ql_bore`, `ql_arm_w` ou `ql_clr`
- entre mais ne tourne pas → augmente `ql_arm_r` ou `ql_chan_h`
- tourne mais flotte → diminue `ql_chan_h` / `ql_clr`
Ne te sers de la coque à vélo/moto qu'après avoir vérifié que le verrou clique et tient.
Pas d'homologation : une fixation imprimée reste moins sûre que l'officielle.

Les positions viennent d'une photo (précision ~1 mm) : un contrôle au pied à coulisse reste conseillé.

## Impression
Avec une imprimante Bambu Lab : suivre **`BAMBU.md`** (réglages Bambu Studio pas à pas).

- **Coque : TPU 95A**, dos à plat sur le plateau, **sans supports**, 4 murs, 0,2 mm
- **Support Quad Lock : PETG**, plaque à plat, plot en haut, sans supports, remplissage 100 %
