# Imprimer avec Bambu Lab (Bambu Studio)

Deux fichiers prêts à ouvrir, déjà orientés et centrés sur le plateau :

| Ordre | Fichier |
|---|---|
| 1 | `bambu_1_test_quadlock.3mf` : pastille d'essai Quad Lock (plaque au plateau, plot en haut) |
| 2 | `bambu_2_coque.3mf` : coque (écran vers le plateau, plot en haut) |

Bambu Studio affiche le temps d'impression et le poids de filament après le tranchage.
Ils tiennent sur tous les plateaux Bambu Lab (A1 mini compris : la coque fait 167 × 81 mm).

## 1. Ouvrir
Bambu Studio → **Fichier > Importer > Importer 3MF/STL** (ou glisser le fichier dans la fenêtre).
Si Bambu Studio demande s'il faut charger « la géométrie uniquement », réponds **oui**.
Ne retourne pas la pièce : elle est déjà dans le bon sens.

## 2. Réglages (panneau de gauche)
- **Imprimante** : la tienne (A1, A1 mini, P1S, X1C…), buse 0,4 mm
- **Plaque** : Textured PEI (sur Smooth PEI, mets de la colle en bâton : le PETG colle trop fort)
- **Filament** : Bambu PETG HF (ou Generic PETG)
- **Process** : 0.20mm Standard, puis dans l'onglet de l'objet :
  - Qualité : hauteur de couche 0,2 mm
  - Solidité : **Murs : 4**, couches dessus/dessous : 5, **remplissage 40 %** (Gyroïde)
  - **Supports** (coque uniquement) : activer, type **Arborescent (auto)**, cocher **Sur le plateau uniquement**
    → les supports remplissent seulement l'intérieur de la coque, sous le dos
  - Autres : **Bordure (brim) extérieure, 5 mm** pour la coque (seul le fin rebord d'écran touche le plateau)
- La pastille d'essai : mêmes réglages **sans** supports ni bordure.

## 3. Trancher et envoyer
**Trancher la plaque** → vérifier l'aperçu couche par couche (le plot Quad Lock doit être en haut)
→ **Imprimer la plaque** (Wi-Fi / cloud Bambu, ou carte microSD). L'impression se suit aussi dans l'app Bambu Handy.

## 4. Après l'impression
- Retire les supports à l'intérieur de la coque (pince plate), ébavure les bords au cutter.
- Pastille d'essai : présente-la à 45° sur ton support Quad Lock, appuie, tourne d'un quart de tour.
  Si ça ne rentre pas, ne tourne pas ou a du jeu, voir le tableau de réglages du `README.md`,
  puis régénère : `python build_stl.py && python make_bambu.py`.
- N'imprime la coque qu'une fois la pastille validée.
