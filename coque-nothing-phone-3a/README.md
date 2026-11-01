# Coque Nothing Phone (3a)

Coque imprimable en 3D, paramétrique, avec ouvertures Glyph et fixation
quart de tour compatible Quad Lock imprimée directement dans le dos.

| Fichier | Rôle |
|---|---|
| `coque.stl` | Coque complète, prête à trancher |
| `test_quadlock.stl` | Pastille d'essai (52 × 52 mm, ~15 min) pour tester la fixation sur ton support |
| `coque.scad` | Modèle OpenSCAD, toutes les cotes en tête de fichier (`part = "test"` pour la pastille) |
| `build_stl.py` | Régénère les deux STL sans OpenSCAD (`pip install manifold3d trimesh`) |
| `apercu*.png` | Aperçus |

## Cotes
- Téléphone : 163,5 × 77,5 × 8,4 mm (fiche technique)
- Coque : 167,3 × 81,3 mm, 11,3 mm d'épaisseur (15,8 mm au niveau de la fixation)
- Bords 1,6 mm, dos 1,6 mm, jeu 0,3 mm, rebord d'écran 1,2 mm
- Découpes : caméra (centrée en haut), Power + Essential Key (droite), volume (gauche),
  USB-C + haut-parleur/micros (bas)
- **Ouvertures Glyph** : bande de 6 mm autour de la caméra en 3 arcs (ponts de 4 mm).
  Options `glyph_open`, `glyph_gap`, `glyph_band`, `bridge_w`, `bridge_angles`.
- **Fixation Quad Lock** : plot Ø 44 mm × 4,5 mm centré à 78 mm du bas. On présente le
  téléphone à 45°, on appuie, on tourne d'un quart de tour : les 4 ergots du support
  passent sous le rebord. Options `quadlock`, `ql_*`.

⚠️ **Cotes estimées.** Quad Lock ne publie pas les cotes de son interface : celles-ci
sont des estimations. **Imprime d'abord `test_quadlock.stl`** et essaie-la sur ton support :
- n'entre pas → augmente `ql_bore`, `ql_arm_w` ou `ql_clr`
- entre mais ne tourne pas → augmente `ql_arm_r` ou `ql_chan_h`
- tourne mais flotte → diminue `ql_chan_h` / `ql_clr`
Ne te sers de la coque à vélo/moto qu'après avoir vérifié que le verrou clique et tient.
Pas d'homologation : une fixation imprimée reste moins sûre que l'officielle.

Les positions des boutons, de la caméra et des LED Glyph sont aussi approximatives :
vérifie au pied à coulisse.

## Impression
- **PETG** (ou ASA/PC) : la fixation doit être rigide, le TPU est trop souple pour verrouiller.
  En PETG, passe `lip` à 0,8 mm pour pouvoir clipser le téléphone.
- **Orientation : écran vers le plateau** (le rebord repose sur le plateau, le plot est en haut).
- Supports « depuis le plateau uniquement » (ils ne remplissent que l'intérieur de la coque)
- Couche 0,2 mm, 4 périmètres, remplissage 40 % minimum
