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
- **Ouvertures Glyph** : bande ouverte de 6 mm autour de la caméra (2 mm de matière
  conservée contre l'ouverture caméra), découpée en 3 arcs par des ponts de 4 mm.
  Options : `glyph_open` (false = dos plein), `glyph_gap`, `glyph_band`, `bridge_w`,
  `bridge_angles`.
- **Quad Lock** : logement de 0,6 mm de profondeur dans le dos (56 × 66 mm + jeu 0,3 mm),
  centré à 78 mm du bas, pour coller l'**adaptateur universel Quad Lock officiel**.
  Les tailles annoncées varient selon la version (V3 : 56 × 66 ou 48 × 60 ; MAG : 60 × 60) :
  mesure le tien et règle `ql_w`, `ql_h`, `ql_r`. Collage : nettoie à l'alcool, utilise
  l'adhésif fourni + une colle compatible TPU, laisse sécher 24 h. Option `quadlock`.

⚠️ Les positions des boutons et de la caméra sont **approximatives**. Mesure ton
téléphone au pied à coulisse et ajuste les variables `cam_*`, `right_*`, `left_*`
avant l'impression finale. Imprime d'abord une version test si besoin.

## Impression
- **TPU 95A** (souple, obligatoire pour clipser le téléphone grâce au rebord)
- Couche 0,2 mm, 3 périmètres, remplissage 100 % (dos et bords sont fins)
- Dos à plat sur le plateau ; supports seulement sous les fentes latérales si ton trancheur le demande
- Vitesse lente (20–30 mm/s) pour le TPU
