# Imprimer en TPU 95A avec Bambu Lab (Bambu Studio)

Deux fichiers prêts à ouvrir, déjà orientés et centrés sur le plateau :

| Ordre | Fichier |
|---|---|
| 1 | `bambu_1_test_quadlock.3mf` : pastille d'essai Quad Lock (plaque au plateau, plot en haut) |
| 2 | `bambu_2_coque.3mf` : coque (écran vers le plateau, plot en haut) |

Bambu Studio affiche le temps d'impression et le poids de filament après le tranchage.
Ils tiennent sur tous les plateaux Bambu Lab (A1 mini compris : la coque fait 167 × 81 mm).

## ⚠️ TPU et Quad Lock
Le TPU 95A est souple : idéal pour clipser le téléphone et amortir les chocs, mais les
ergots du support Quad Lock peuvent déformer le rebord du plot et se décrocher.
**La pastille d'essai en TPU est indispensable** : si elle se déclipse quand tu tires fort,
ne l'utilise pas à moto. Solutions alors : coller l'adaptateur universel Quad Lock officiel
au dos (je peux remettre un logement), ou imprimer seulement la fixation en PETG.

## 0. Filament
- Le TPU **ne passe pas dans l'AMS / AMS lite** : mets la bobine sur le **support externe**.
- Sèche le TPU avant (4–6 h à 55–65 °C) : humide, il file et fait des bulles.

## 1. Ouvrir
Bambu Studio → **Fichier > Importer > Importer 3MF/STL** (ou glisser le fichier).
Si Bambu Studio demande de charger « la géométrie uniquement », réponds **oui**.
Ne retourne pas la pièce : elle est déjà dans le bon sens.

## 2. Réglages
- **Imprimante** : la tienne, buse 0,4 mm
- **Plaque** : Textured PEI (ou Smooth PEI + colle en bâton pour pouvoir décoller le TPU)
- **Filament** : **Bambu TPU 95A HF** (ou Generic TPU), en **filament externe** (« Ext »)
- **Process** : 0.20mm Standard, puis :
  - Solidité : **Murs : 4**, couches dessus/dessous : 5, remplissage **40 %** (Gyroïde)
  - Vitesse : laisse le profil filament limiter le débit ; si ça file, baisse à ~40–60 mm/s
  - Rétraction : garde celle du profil TPU (courte)
  - **Supports** (coque uniquement) : activer, type **Arborescent (auto)**,
    cocher **Sur le plateau uniquement**, **distance Z haut : 0,3 mm**, interface : 0 couche
    → supports seulement à l'intérieur, sous le dos ; l'écart de 0,3 mm les rend détachables en TPU
  - **Bordure (brim) extérieure 5 mm** pour la coque (seul le rebord d'écran touche le plateau)
- Pastille d'essai : mêmes réglages **sans** supports ni bordure.

## 3. Trancher et envoyer
**Trancher la plaque** → vérifier l'aperçu (plot Quad Lock en haut) → **Imprimer la plaque**
(Wi-Fi / cloud, ou microSD). Suivi possible dans l'app Bambu Handy.

## 4. Après l'impression
- Retire les supports intérieurs en tirant doucement à la pince, coupe les restes au cutter.
- Pastille : présente-la à 45° sur ton support, appuie, tourne d'un quart de tour, tire fort.
  Réglages dans le tableau du `README.md`, puis : `python build_stl.py && python make_bambu.py`.
- N'imprime la coque qu'une fois la pastille validée.
