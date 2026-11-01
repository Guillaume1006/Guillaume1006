# Coque Nothing Phone (3a)

Coque imprimable en 3D, paramétrique.

| Fichier | Rôle |
|---|---|
| `coque.stl` | Prête à trancher (Cura, PrusaSlicer, Bambu Studio…) |
| `coque.scad` | Modèle OpenSCAD, toutes les cotes en tête de fichier |
| `build_stl.py` | Régénère le STL sans OpenSCAD (`pip install manifold3d trimesh`) |
| `apercu.png`, `apercu_dos.png` | Aperçus (3D et dos vu à plat) |

## Cotes
- Téléphone : 163,5 × 77,5 × 8,4 mm (fiche technique)
- Coque : 167,3 × 81,3 × 11,3 mm, bords 1,6 mm, dos 1,6 mm, jeu 0,3 mm, rebord d'écran 1,2 mm
- Découpes : îlot caméra (44 × 28 mm, centré en haut), Power + Essential Key (droite),
  volume (gauche), USB-C + haut-parleur/micros (bas)
- **Trous Glyph** : 184 trous de 1,8 mm (grille hexagonale, pas de 3 mm) en anneau
  autour de la caméra pour laisser passer les LED Glyph. Option `glyph_holes`
  (mettre `false` pour un dos plein) ; zone réglable avec `glyph_w` / `glyph_h`.
  Astuce : en TPU transparent ou naturel, la lumière diffuse aussi à travers la matière.

⚠️ Les positions des boutons et de la caméra sont **approximatives**. Mesure ton
téléphone au pied à coulisse et ajuste les variables `cam_*`, `right_*`, `left_*`
avant l'impression finale. Imprime d'abord une version test si besoin.

## Impression
- **TPU 95A** (souple, obligatoire pour clipser le téléphone grâce au rebord)
- Couche 0,2 mm, 3 périmètres, remplissage 100 % (dos et bords sont fins)
- Dos à plat sur le plateau ; supports seulement sous les fentes latérales si ton trancheur le demande
- Vitesse lente (20–30 mm/s) pour le TPU
